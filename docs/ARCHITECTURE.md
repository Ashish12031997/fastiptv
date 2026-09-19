# Architecture — FastIPTV

## Overview

FastIPTV is a high-performance, TV-first IPTV application for Android TV and Fire TV (with adaptive mobile/tablet support). It follows **Clean Architecture** with the **MVVM (Model-View-ViewModel)** pattern, organized into modular layers:

```
┌───────────────────────────────────────────────────────────┐
│                        UI LAYER                           │
│  Jetpack Compose TV (TV) / Compose Material3 (Mobile)     │
│  Screens → ViewModels → StateFlow (UI State)              │
├───────────────────────────────────────────────────────────┤
│                      DOMAIN LAYER                         │
│  Use Cases → Repository Interfaces → Domain Models        │
│  (Pure Kotlin — zero Android framework dependencies)      │
├───────────────────────────────────────────────────────────┤
│                       DATA LAYER                          │
│  Retrofit / OkHttp API ↔ Room Database (SSOT)             │
│  Repository Implementations ↔ Data Sources                │
├───────────────────────────────────────────────────────────┤
│                      PLAYER LAYER                         │
│  Media3 ExoPlayer ↔ StreamErrorHandler ↔ PlaybackState    │
│  OkHttpDataSource Factory ↔ Audio Focus & Wake Locks      │
└───────────────────────────────────────────────────────────┘
```

**Dependency Rule**: Outer layers depend on inner layers. Domain has zero Android framework dependencies and contains pure business logic.

---

## Data Flow (Single Source of Truth)

The application uses Room as the **Single Source of Truth (SSOT)**. ViewModels observe reactive Room `Flow` streams. Network responses update Room in atomic transactions, which automatically notify active UI collectors.

```
User Action (D-Pad click, search input, channel change)
    │
    ▼
ViewModel receives intent / action
    │
    ▼
UseCase invokes Repository method
    │
    ▼
Repository (Room as Reactive SSOT):
    ├── 1. Emits current cache from Room immediately via Flow
    ├── 2. Triggers background network fetch (Xtream API)
    ├── 3. Atomically reconciles & upserts into Room (@Transaction)
    └── 4. Room automatically emits updated dataset to active collectors
    │
    ▼
ViewModel processes & maps to immutable UI State (StateFlow)
    │
    ▼
Jetpack Compose TV recomposes (diffed via stable keys)
```

---

## Layer Details

### 1. Data Layer (`data/`)

#### API (`data/api/`)

| Component | Purpose |
|---|---|
| `XtreamApi.kt` | Retrofit interface for all Xtream Codes endpoints (Live, VOD, Series, EPG) |
| `XtreamUrlBuilder.kt` | Constructs media stream URLs (`.m3u8`, `.ts`, `.mkv`, `.mp4`) |
| `AuthInterceptor.kt` | Injects active username, password, and custom User-Agent into requests |
| `DynamicBaseUrlInterceptor.kt` | Dynamically rewrites HTTP host and port based on active `ServerConfig` |
| `SessionManager.kt` | Manages credential storage via Jetpack DataStore (encrypted) |

**Key Design Decisions:**
- **Dynamic Base URL**: IPTV server hosts and ports are configured at runtime. An OkHttp interceptor dynamically redirects requests to the active host without recreating Retrofit instances.
- **Player-Grade User-Agent**: Every request carries `IPTVSmartersPro/1.0.0 (Linux;Android 11) ExoPlayerLib/2.18.2` to bypass server-side firewalls and panel anti-bot blocks.
- **Connection Pooling & Keep-Alive**: Persistent TCP connections prevent connection setup latency on repeated API and chunk requests.

#### Database & Caching (`data/db/`)

Room acts as the local persistent cache and offline store.

| Table | Purpose | Key Columns / Indexes |
|---|---|---|
| `channels` | Cached live channels | `stream_id` (PK), `name`, `icon_url`, `category_id`, `epg_channel_id`, `stream_type` |
| `categories` | Categories (Live, VOD, Series) | `category_id` (PK), `name`, `type` (`live`/`vod`/`series`) |
| `favorites` | User favorites | `stream_id` (PK), `type`, `added_at` |
| `recents` | Watch history | `stream_id` (PK), `type`, `last_watched`, `watch_position_ms`, `duration_ms` |
| `vod_streams` | Cached movies | `stream_id` (PK), `name`, `poster_url`, `category_id`, `container_ext`, `rating` |
| `series` | Cached TV series | `series_id` (PK), `name`, `cover_url`, `category_id`, `rating`, `genre` |
| `episodes` | Series episodes | `episode_id` (PK), `series_id`, `season_number`, `episode_number`, `title`, `container_ext` |
| `epg_programs` | EPG listings | `id` (PK), `epg_channel_id`, `title`, `start_time`, `end_time`, `description` |
| `channels_fts` | Full-Text Search virtual table | FTS4 virtual table mapping `name` from `channels` |
| `vod_fts` | Full-Text Search virtual table | FTS4 virtual table mapping `name` from `vod_streams` |

**Reactive SSOT Repository Pattern (Preventing Cache Orphans):**
```kotlin
// ChannelDao.kt
@Query("""
    SELECT c.*, (f.stream_id IS NOT NULL) AS is_favorite 
    FROM channels c
    LEFT JOIN favorites f ON c.stream_id = f.stream_id
    WHERE c.category_id = :categoryId
    ORDER BY c.stream_id ASC
""")
fun observeChannelsByCategory(categoryId: String): Flow<List<ChannelWithFavoriteEntity>>

// IptvRepositoryImpl.kt
override fun getChannels(categoryId: String): Flow<List<Channel>> {
    return channelDao.observeChannelsByCategory(categoryId)
        .map { list -> list.map { it.toDomain() } }
        .onStart {
            // Trigger background sync without blocking cached emission
            refreshChannelsForCategory(categoryId)
        }
}

private suspend fun refreshChannelsForCategory(categoryId: String) {
    try {
        val fresh = api.getLiveStreams(categoryId = categoryId)
        database.withTransaction {
            // Remove channels no longer present in provider category to prevent orphans
            channelDao.deleteByCategory(categoryId)
            channelDao.upsertAll(fresh.toEntities(categoryId))
        }
    } catch (e: IOException) {
        // Network unavailable: Room flow continues serving cached data seamlessly
    }
}
```

#### High-Performance Search Architecture (Room FTS4)

Rather than keeping 30,000+ objects in memory on memory-constrained devices (e.g. Fire TV Sticks with 1GB RAM), search queries execute directly against indexed SQLite FTS4 virtual tables:

```kotlin
@Query("""
    SELECT c.*, (f.stream_id IS NOT NULL) AS is_favorite
    FROM channels c
    JOIN channels_fts fts ON c.rowid = fts.docid
    LEFT JOIN favorites f ON c.stream_id = f.stream_id
    WHERE channels_fts MATCH :query || '*'
    LIMIT 50
""")
fun searchChannels(query: String): Flow<List<ChannelWithFavoriteEntity>>
```
- **Execution Time**: < 2ms directly in SQLite.
- **Memory Footprint**: Only matching results are materialized into RAM.

---

### 2. Domain Layer (`domain/`)

Zero Android dependencies. Pure Kotlin interfaces and business models.

#### Models (`domain/model/`)

| Model | Purpose |
|---|---|
| `Channel` | Live TV channel (`id`, `name`, `logoUrl`, `categoryId`, `epgChannelId`, `isFavorite`, `nowPlaying`) |
| `Movie` | VOD movie (`id`, `name`, `posterUrl`, `rating`, `year`, `duration`, `containerExt`, `isFavorite`) |
| `Series` | TV series show (`id`, `name`, `coverUrl`, `rating`, `genre`, `seasons`) |
| `Episode` | Series episode (`id`, `seriesId`, `seasonNumber`, `episodeNumber`, `title`, `containerExt`) |
| `EpgProgram` | EPG listing (`id`, `title`, `startTime`, `endTime`, `description`, `nowPlaying`) |
| `ServerConfig` | Host, port, username, password, active protocol, connection status |
| `SearchResult` | Discriminated union of Channel, Movie, and Series search hits |
| `PlayerState` | Sealed state representing player lifecycle (Idle, Loading, Playing, Buffering, Error) |

#### Use Cases (`domain/usecase/`)

| Use Case | Purpose |
|---|---|
| `AuthenticateUseCase` | Validates credentials and verifies server connection |
| `GetCategoriesUseCase` | Fetches observable categories by type (Live, VOD, Series) |
| `GetChannelsUseCase` | Observes channels by category with favorite indicators |
| `GetVodStreamsUseCase` | Observes movies by category with ratings |
| `GetSeriesUseCase` | Observes TV series and season details |
| `SearchContentUseCase` | FTS search across channels, movies, and series |
| `ToggleFavoriteUseCase` | Atomic toggle for channel, movie, or series favorites |
| `GetFavoritesUseCase` | Observes unified user favorites |
| `GetRecentUseCase` | Observes continue-watching queue and watch progress |
| `BuildStreamUrlUseCase` | Resolves target streaming URL based on protocol preference (.m3u8 vs .ts) |
| `SyncEpgUseCase` | Background ingestion of XMLTV and short EPG data |

---

### 3. Player Layer (`player/`)

Isolated Media3 ExoPlayer management, custom network data sources, and error recovery.

#### Media3 Player Configuration (`StreamPlayer.kt`)

```kotlin
@Provides
@Singleton
fun provideExoPlayer(
    @ApplicationContext context: Context,
    okHttpClient: OkHttpClient,
    loadControl: LoadControl,
    errorHandler: StreamErrorHandler
): ExoPlayer {
    // Media3 MUST use OkHttpDataSource to share connection pool and User-Agent
    val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        .setUserAgent("IPTVSmartersPro/1.0.0 (Linux;Android 11) ExoPlayerLib/2.18.2")

    val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .setLoadControl(loadControl)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            /* handleAudioFocus = */ true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build().also {
            it.addListener(errorHandler)
        }
}
```

#### Buffer Tuning (Low-Latency & Anti-Stall)

```kotlin
DefaultLoadControl.Builder()
    .setBufferDurationsMs(
        /* minBufferMs */                  15_000,   // Keep 15s ahead
        /* maxBufferMs */                  60_000,   // 60s max buffer
        /* bufferForPlaybackMs */           2_500,   // Start playback at 2.5s
        /* bufferForPlaybackAfterRebuffer */ 5_000   // 5s buffer after stall
    )
    .setTargetBufferBytes(50 * 1024 * 1024)          // 50MB RAM buffer limit
    .setPrioritizeTimeOverSizeThresholds(true)
    .build()
```

#### Multi-Stage Auto-Recovery Pipeline (`StreamErrorHandler.kt`)

The player auto-recovers from stream stalls, 403 blocks, or broken manifests through a synchronized 4-stage pipeline:

```
Stream Error Triggered (Player.Listener.onPlayerError)
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  Stage 1: RETRY SAME URL                                    │
│  Attempts: 3                                                │
│  Backoff Delay: 1s → 2s → 4s                                │
│  Action: player.prepare() + player.play()                   │
└──────────────────────────────┬──────────────────────────────┘
                               │ All 3 failed
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  Stage 2: SWITCH STREAM FORMAT                              │
│  Attempts: 1 (Immediate)                                    │
│  Action: Toggle .m3u8 ↔ .ts format, player.setMediaItem()    │
└──────────────────────────────┬──────────────────────────────┘
                               │ Failed
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  Stage 3: FRESH CONNECTION & DNS FLUSH                      │
│  Attempts: 1 (2s delay)                                     │
│  Action: Evict OkHttp connection pool, retry new socket     │
└──────────────────────────────┬──────────────────────────────┘
                               │ Failed
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  Stage 4: USER NOTIFICATION & BACKGROUND AUTO-RETRY         │
│  Action: Emit PlayerState.Error to UI (Show Retry Overlay)  │
│  Background: Auto-retry in 30s if channel is not switched   │
└─────────────────────────────────────────────────────────────┘
```

#### Race-Condition-Free Channel Switching
When switching channels rapidly via D-pad (▲ / ▼), any active retry coroutine must be cancelled immediately to prevent stale channel playback:

```kotlin
class StreamErrorHandler @Inject constructor(
    private val urlBuilder: XtreamUrlBuilder,
    private val sessionManager: SessionManager,
    private val okHttpClient: OkHttpClient
) : Player.Listener {

    private var recoveryJob: Job? = null
    private var retryCount = 0
    private var currentFormat = "m3u8"
    private var currentStreamId: Int = 0

    fun onChannelChanged(streamId: Int) {
        // Cancel in-flight retry job to prevent race conditions
        recoveryJob?.cancel()
        recoveryJob = null
        currentStreamId = streamId
        retryCount = 0
    }

    override fun onPlayerError(error: PlaybackException) {
        recoveryJob?.cancel()
        recoveryJob = CoroutineScope(Dispatchers.Main.immediate).launch {
            handleRecovery(error)
        }
    }
}
```

#### Lifecycle & Concurrency Guard (`max_connections`)
Most IPTV accounts permit only **1 concurrent connection**. 
- In `PlayerScreen`, `DisposableEffect` halts playback (`player.stop()`) and frees the video surface when navigating away.
- `FLAG_KEEP_SCREEN_ON` is tied to playback state (`isPlaying`) so the TV screensaver is inhibited only while content actively renders.

---

### 4. UI Layer (`ui/`)

#### Jetpack Compose TV Design System
The UI is built with `androidx.tv.material3` for Android TV, utilizing native spatial navigation and built-in focus styling:

```kotlin
// Idiomatic TV Card with automatic focus management
androidx.tv.material3.Card(
    onClick = { onChannelSelected(channel) },
    scale = CardDefaults.scale(focusedScale = 1.05f),
    border = CardDefaults.border(
        focusedBorder = Border(border = BorderStroke(2.dp, AccentBlue))
    ),
    colors = CardDefaults.colors(
        containerColor = SurfaceDark,
        focusedContainerColor = SurfaceElevated
    )
) {
    ChannelCardContent(channel = channel)
}
```
*Note*: No manual `handleDPadKeyEvents` monkey-patching. Spatial navigation is managed by Compose TV's focus engine.

#### Navigation Graph

```
NavHost(startDestination = "home") {
    composable("home")             → HomeScreen
    composable("channels/{catId}") → ChannelListScreen
    composable("player/{streamId}")→ PlayerScreen
    composable("search")           → SearchScreen
    composable("favorites")        → FavoritesScreen
    composable("vod")              → VodScreen
    composable("vod/{streamId}")   → VodDetailScreen
    composable("series")           → SeriesScreen
    composable("series/{seriesId}")→ SeriesDetailScreen
    composable("settings")         → SettingsScreen
}
```

#### Dual Layout Strategy (TV vs Mobile)
The app detects the active form factor at runtime via `UiModeManager`:
- **TV Mode**: Uses `androidx.tv.material3`, landscape-locked, full D-pad focus engine, auto-hiding video player overlays.
- **Mobile / Tablet Mode**: Uses standard `androidx.compose.material3`, touch-friendly gesture controls, portrait/landscape orientation switching.
- **Shared Code**: Both form factors share identical ViewModels, Domain Use Cases, and Repositories.

---

### 5. Electronic Program Guide (EPG) Engine

IPTV EPG handling requires robust memory management due to large XMLTV files (10–50MB compressed):

```
┌────────────────────────────────────────────────────────┐
│                   EPG Ingestion Pipeline               │
│                                                        │
│  1. Short EPG (/player_api.php?action=get_short_epg)   │
│     - Lightweight on-demand fetch (next 5 programs)    │
│     - Auto-decodes Base64 titles/descriptions          │
│     - Updates active channel overlay in real-time      │
│                                                        │
│  2. Full XMLTV (/xmltv.php) — Background Sync          │
│     - Executed via WorkManager (once every 24h)        │
│     - Streamed using XmlPullParser (no OOM in memory)  │
│     - Batched upsert into `epg_programs` table         │
│     - Indexed by (epg_channel_id, start_time, end_time)│
└────────────────────────────────────────────────────────┘
```

---

## Dependency Injection (Hilt)

```
@HiltAndroidApp
FastIptvApp
    │
    ├── NetworkModule (@Provides)
    │   ├── OkHttpClient (AuthInterceptor, DynamicBaseUrlInterceptor)
    │   ├── Retrofit (Kotlinx.Serialization converter)
    │   └── XtreamApi
    │
    ├── DatabaseModule (@Provides)
    │   ├── AppDatabase
    │   ├── ChannelDao, CategoryDao, FavoriteDao, RecentDao
    │   ├── VodDao, SeriesDao, EpgDao
    │   └── ChannelFtsDao, VodFtsDao
    │
    ├── PlayerModule (@Provides)
    │   ├── OkHttpDataSource.Factory
    │   ├── DefaultLoadControl (Optimized buffer thresholds)
    │   ├── StreamErrorHandler
    │   └── ExoPlayer (@Singleton instance with audio focus)
    │
    └── RepositoryModule (@Binds)
        └── IptvRepository → IptvRepositoryImpl
```

---

## Performance Targets

| Metric | Target | Implementation Method |
|---|---|---|
| **App cold start** | < 1.5s | Room cache immediately renders; zero network gating |
| **Channel switch latency** | < 500ms to first frame | Player reuse + 2.5s playback buffer threshold |
| **Channel list render** | 60 FPS scrolling (1000+ items) | `TvLazyColumn` with stable item keys and Room Paging 3 |
| **Search query latency** | < 2ms across 30,000 items | SQLite FTS4 indexed virtual tables |
| **RAM usage** | < 150MB | OkHttp disk caching, Coil TV bitmap limits, 50MB ExoPlayer buffer cap |
| **Cache resilience** | 100% offline browseable | Normalized Room database with foreign key cascades |

---

## Threading Model

```
Main Thread (UI)
    └── Compose rendering, focus system, ExoPlayer surface presentation

Dispatchers.IO (Background Pool)
    ├── Retrofit HTTP API operations
    ├── Room database transactions and queries
    ├── XMLTV streaming parser (XmlPullParser)
    └── DataStore credential reads/writes

Dispatchers.Default (Compute Pool)
    └── Base64 EPG decoding, DTO-to-Domain conversions

ExoPlayer Internal Threads
    ├── Playback Thread (Internal timeline & track management)
    ├── MediaSource / HttpDataSource thread (Chunk fetching)
    └── MediaCodec decoding threads (Hardware video/audio pipelines)
```

All ViewModel ↔ Repository communications utilize Kotlin `Flow` with `flowOn(Dispatchers.IO)`.
