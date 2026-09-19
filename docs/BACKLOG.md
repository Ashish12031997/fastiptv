# Agile Backlog & Jira Implementation Plan — FastIPTV

**Project Key**: `FIP`  
**Methodology**: Scrum / Agile (2-Week Sprints)  
**Target Platform**: Android TV & Fire TV (API 21–35, Min SDK 21), Mobile/Tablet Adaptive  
**Core Stack**: Kotlin 2.1 (K2), Jetpack Compose TV 1.0+, Media3 ExoPlayer 1.5+, Room 2.7+ (FTS4), Hilt 2.55, Retrofit + Kotlinx Serialization, Coil 3.1, WorkManager 2.10+

---

## 🗺️ Epic Overview

| Epic ID | Epic Name | Description | Total Points | Sprints |
|---|---|---|:---:|:---:|
| **EPIC-1** | **Foundation & Architecture Infrastructure** | Build system, KTS version catalog, DI, dynamic networking, and session storage. | 21 SP | Sprint 1 |
| **EPIC-2** | **Room SSOT & Offline Persistence Engine** | Room database schema, FTS4 virtual tables, reactive flows, and atomic cache reconciliation. | 26 SP | Sprint 1–2 |
| **EPIC-3** | **Media3 ExoPlayer & Stream Auto-Recovery** | Resilient media player, OkHttp data source, 4-stage recovery pipeline, audio focus. | 34 SP | Sprint 2–3 |
| **EPIC-4** | **Compose TV Design System & Home Experience** | TV design tokens, spatial focus management, top navigation bar, category carousels. | 29 SP | Sprint 3–4 |
| **EPIC-5** | **Live TV Playback & EPG Ingestion Engine** | Fullscreen player, D-pad channel surfing, on-demand short EPG, streaming XMLTV sync. | 34 SP | Sprint 4–5 |
| **EPIC-6** | **High-Performance FTS Search Engine** | Sub-2ms search queries over 30,000+ items via SQLite FTS4, TV on-screen keyboard. | 18 SP | Sprint 5 |
| **EPIC-7** | **VOD Movies & TV Series Subsystems** | Hierarchical browsing and playback for Movies and Series (Seasons/Episodes). | 29 SP | Sprint 6 |
| **EPIC-8** | **User Personalization & Adaptive Layouts** | Favorites, Continue Watching (Recents), Settings screen, Mobile/Tablet UI fallback. | 21 SP | Sprint 7 |
| **EPIC-9** | **Performance Hardening & Release Readiness** | Low-RAM (<150MB) Fire TV Stick tuning, Baseline Profiles, R8, crash-proofing. | 21 SP | Sprint 8 |

---

## 🏃 Sprint Breakdown & User Stories

---

### Sprint 1: Foundation, Build System & Data Engine (Weeks 1–2)

#### `FIP-101` [Task] Modern Gradle KTS Version Catalog & Multi-Module/Clean Architecture Setup
* **Epic**: `EPIC-1` | **Component**: `Build / Infra` | **Points**: 3 SP | **Priority**: Blocker
* **Description**: Set up Gradle 8.11+ with Kotlin 2.1+ using `gradle/libs.versions.toml`. Configure compiler flags, KSP, JVM target 17, and Android SDK 35 (minSdk 21).
* **Technical Details**:
  - Configure `libs.versions.toml` with version definitions for Compose TV, Media3, Hilt, Room, Retrofit, Serialization, and Coil.
  - Enable Compose compiler metrics and reports in `app/build.gradle.kts`.
  - Add ProGuard rules for Kotlinx Serialization and Media3.
* **Acceptance Criteria**:
  - `GIVEN` a clean repository checkout
  - `WHEN` executing `./gradlew assembleDebug`
  - `THEN` the build succeeds without deprecation warnings and generates a runnable debug APK.

---

#### `FIP-102` [Story] Encrypted Credentials & Dynamic Server Configuration via DataStore
* **Epic**: `EPIC-1` | **Component**: `Data / Storage` | **Points**: 5 SP | **Priority**: Blocker
* **User Story**:
  > As an IPTV subscriber,  
  > I want the app to securely persist my provider's host URL, port, username, and password,  
  > So that I don't have to re-enter my credentials every time the app opens.
* **Technical Details**:
  - Create `SessionManager.kt` backed by Jetpack `DataStore<Preferences>`.
  - Encrypt sensitive credentials using AndroidX Security (`EncryptedSharedPreferences` / Tink AEAD wrapper).
  - Expose `serverConfigFlow: Flow<ServerConfig?>` to the domain layer.
* **Acceptance Criteria**:
  - `GIVEN` valid IPTV server parameters (`http://provider.com:8080`, `user`, `pass`)
  - `WHEN` `saveCredentials()` is invoked
  - `THEN` credentials persist across process death and are emitted instantly on next cold start.

---

#### `FIP-103` [Story] OkHttp Networking Engine with Dynamic Base URL & Anti-Bot User-Agent
* **Epic**: `EPIC-1` | **Component**: `Network` | **Points**: 5 SP | **Priority**: Blocker
* **User Story**:
  > As an IPTV user,  
  > I want network requests to dynamically target my provider's URL and bypass anti-bot blocks,  
  > So that API and stream requests succeed without 403 Forbidden errors.
* **Technical Details**:
  - Create `AuthInterceptor.kt`: Injects `username`, `password`, and custom User-Agent `IPTVSmartersPro/1.0.0 (Linux;Android 11) ExoPlayerLib/2.18.2`.
  - Create `DynamicBaseUrlInterceptor.kt`: Intercepts requests and swaps the host/port dynamically using the active `ServerConfig`.
  - Configure OkHttp `ConnectionPool(maxIdleConnections = 10, keepAliveDuration = 5, TimeUnit.MINUTES)`.
* **Acceptance Criteria**:
  - `GIVEN` an API request to `/player_api.php`
  - `WHEN` credentials or server URL change at runtime
  - `THEN` the next outgoing HTTP request redirects to the new host without recreating Retrofit.
  - `AND` headers include the custom IPTV User-Agent.

---

#### `FIP-104` [Story] Xtream Codes Retrofit Service & DTO Deserialization
* **Epic**: `EPIC-1` | **Component**: `Network` | **Points**: 5 SP | **Priority**: High
* **User Story**:
  > As a developer,  
  > I want type-safe Retrofit endpoints for Xtream Codes API actions,  
  > So that the app can fetch authentication info, categories, streams, and series.
* **Technical Details**:
  - Implement `XtreamApi.kt`: Endpoints for `get_live_categories`, `get_live_streams`, `get_vod_categories`, `get_vod_streams`, `get_series_categories`, `get_series`, `get_series_info`, `get_short_epg`.
  - Use `kotlinx.serialization.json.Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }`.
  - Provide unit tests with mock JSON fixtures for malformed responses.
* **Acceptance Criteria**:
  - `GIVEN` valid mock server responses for Xtream endpoints
  - `WHEN` executing API calls
  - `THEN` JSON payloads deserialize into strongly-typed DTOs without runtime reflection.

---

#### `FIP-105` [Task] Normalized Room Database Schema with FTS4 Tables
* **Epic**: `EPIC-2` | **Component**: `Database` | **Points**: 5 SP | **Priority**: Blocker
* **Description**: Create Room database entities, DAOs, and FTS4 virtual tables for high-performance offline indexing.
* **Technical Details**:
  - Entities: `ChannelEntity`, `CategoryEntity`, `FavoriteEntity`, `RecentEntity`, `VodEntity`, `SeriesEntity`, `EpisodeEntity`, `EpgProgramEntity`.
  - Virtual Tables: `ChannelFtsEntity` (`@Fts4(contentEntity = ChannelEntity::class)`), `VodFtsEntity`.
  - Create indexes on `(category_id)`, `(stream_id)`, and `(epg_channel_id, start_time, end_time)`.
* **Acceptance Criteria**:
  - `GIVEN` Room database initialization
  - `WHEN` executing database migrations or creation
  - `THEN` all tables, relations, and FTS4 tables are created with foreign key cascades.

---

### Sprint 2: SSOT Caching & Media3 Player Core (Weeks 3–4)

#### `FIP-201` [Story] Reactive Repository with Atomic Cache Refresh (No Orphan Records)
* **Epic**: `EPIC-2` | **Component**: `Data / Repository` | **Points**: 8 SP | **Priority**: High
* **User Story**:
  > As a user,  
  > I want my channel list to load instantly from cache and silently refresh in the background,  
  > So that deleted or updated provider channels are automatically reconciled without ghost entries.
* **Technical Details**:
  - Implement `IptvRepositoryImpl.kt` using Room as SSOT.
  - Return `Flow<List<Channel>>` mapped from `channelDao.observeChannelsByCategory(categoryId)`.
  - In `onStart { refreshCategory(categoryId) }`, fetch from API and run `database.withTransaction`:
    - Delete removed category streams (`DELETE FROM channels WHERE category_id = :id`).
    - Upsert fresh entries (`channelDao.upsertAll(freshEntities)`).
* **Acceptance Criteria**:
  - `GIVEN` 1,000 cached channels in Room
  - `WHEN` `getChannels(categoryId)` is collected
  - `THEN` cached channels emit in `< 50ms`.
  - `AND` when network fetch completes, updated channels emit automatically via Room flow without app restart.

---

#### `FIP-202` [Story] Media3 ExoPlayer Engine with OkHttp DataSource & Buffer Tuning
* **Epic**: `EPIC-3` | **Component**: `Player` | **Points**: 8 SP | **Priority**: Blocker
* **User Story**:
  > As a TV viewer,  
  > I want video streams to begin playback in under 500ms with smooth buffering,  
  > So that channel surfing feels as fast as traditional cable TV.
* **Technical Details**:
  - Provide `@Singleton ExoPlayer` configured with `DefaultMediaSourceFactory(OkHttpDataSource.Factory(okHttpClient))`.
  - Configure `DefaultLoadControl`:
    - `bufferForPlaybackMs = 2,500ms` (Fast start).
    - `minBufferMs = 15,000ms`, `maxBufferMs = 60,000ms`.
    - `targetBufferBytes = 50MB` (Protects low-RAM TV sticks).
  - Configure `AudioAttributes(C.AUDIO_CONTENT_TYPE_MOVIE, C.USAGE_MEDIA)` with `handleAudioFocus = true`.
  - Set `setWakeMode(C.WAKE_MODE_NETWORK)`.
* **Acceptance Criteria**:
  - `GIVEN` a valid HLS (`.m3u8`) or MPEG-TS (`.ts`) stream URL
  - `WHEN` `player.setMediaItem(item)` and `player.prepare()` are called
  - `THEN` first frame renders in `< 500ms` on broadband connections.
  - `AND` network requests include the custom User-Agent from OkHttp.

---

#### `FIP-203` [Story] StreamErrorHandler: 4-Stage Auto-Recovery Pipeline
* **Epic**: `EPIC-3` | **Component**: `Player` | **Points**: 8 SP | **Priority**: High
* **User Story**:
  > As a viewer,  
  > I want the player to automatically recover when a live stream drops or encounters a 403/format error,  
  > So that I don't have to exit and re-select the channel manually.
* **Technical Details**:
  - Implement `StreamErrorHandler.kt` implementing `Player.Listener`:
    - **Stage 1**: Retry same URL 3 times with exponential backoff (1s, 2s, 4s).
    - **Stage 2**: Switch container format (`.m3u8` ↔ `.ts`).
    - **Stage 3**: Evict OkHttp connection pool (`okHttpClient.connectionPool.evictAll()`), re-resolve DNS, and retry.
    - **Stage 4**: Emit `PlayerState.Error(canRetry = true)` and trigger 30s auto-retry timer.
  - Ensure `recoveryJob?.cancel()` runs on `onChannelChanged(streamId)`.
* **Acceptance Criteria**:
  - `GIVEN` an active stream that receives an HTTP 403 or network socket reset
  - `WHEN` `onPlayerError` fires
  - `THEN` the player attempts format switching and socket refresh before displaying an error overlay.
  - `AND` rapid channel changes cancel pending retries without stale playback.

---

#### `FIP-204` [Task] Single-Connection Guard & Video Surface Lifecycle Management
* **Epic**: `EPIC-3` | **Component**: `Player / UI` | **Points**: 5 SP | **Priority**: High
* **Description**: Ensure streams are strictly halted and released when navigating away from the player to prevent IPTV provider bans on accounts with `max_connections = 1`.
* **Technical Details**:
  - Implement Compose `DisposableEffect` in `PlayerScreen.kt` calling `player.stop()` on `onDispose`.
  - Bind `FLAG_KEEP_SCREEN_ON` to player state (`isPlaying`).
* **Acceptance Criteria**:
  - `GIVEN` an active video playback session
  - `WHEN` the user presses `Back` to return to Home or Settings
  - `THEN` media playback halts immediately and network TCP sockets close within 1 second.

---

### Sprint 3: Compose TV Design System & Home Experience (Weeks 5–6)

#### `FIP-301` [Story] Compose TV Design Tokens, Focus Palette & Typography
* **Epic**: `EPIC-4` | **Component**: `UI / Theme` | **Points**: 3 SP | **Priority**: High
* **User Story**:
  > As a TV user sitting 10 feet away,  
  > I want high-contrast text and luminous focus indicators,  
  > So that I can clearly see what item my remote control is focused on.
* **Technical Details**:
  - Implement TV design system in `ui/theme/`:
    - Colors: Background `#0A0A0F`, Surface `#14141F`, SurfaceElevated `#1E1E2E`, Primary `#4A9EFF`, Secondary `#FF6B6B`.
    - Focus Rings: 2dp `#4A9EFF` with 1.05x scale transform.
  - Provide dark-mode typography scaled for 10-foot viewing distances (`1080p` and `4K` TV displays).
* **Acceptance Criteria**:
  - `GIVEN` any focusable element
  - `WHEN` D-pad navigates to it
  - `THEN` the card smoothly scales to 1.05x and highlights with a `#4A9EFF` glowing border.

---

#### `FIP-302` [Story] TV Top Navigation Bar with Spatial D-Pad Handling
* **Epic**: `EPIC-4` | **Component**: `UI / Components` | **Points**: 5 SP | **Priority**: High
* **User Story**:
  > As a TV viewer,  
  > I want a top navigation bar with quick links to Live, Movies, Series, Favorites, Search, and Settings,  
  > So that I can switch between major content sections effortlessly.
* **Technical Details**:
  - Implement `TopNavBar.kt` using `androidx.tv.material3.TabRow` or custom focusable items.
  - Implement D-pad boundary interception (pressing `Down` shifts focus into the first category carousel below).
* **Acceptance Criteria**:
  - `GIVEN` the Top Navigation bar
  - `WHEN` moving D-pad `Left`/`Right`
  - `THEN` selection changes smoothly.
  - `WHEN` pressing `Down`
  - `THEN` focus transitions to the first content item in the screen body.

---

#### `FIP-303` [Story] Home Screen with Horizontally Scrolling Category Rows
* **Epic**: `EPIC-4` | **Component**: `UI / Home` | **Points**: 8 SP | **Priority**: High
* **User Story**:
  > As a user,  
  > I want a Netflix/Prime-style Home screen displaying my categories in horizontal rows,  
  > So that I can explore live channels and movies by genre.
* **Technical Details**:
  - Create `HomeScreen.kt` with `TvLazyColumn` hosting nested `TvLazyRow` components.
  - Implement `ChannelCard.kt` using `androidx.tv.material3.Card` with async logo loading via `CoilImage`.
  - Pass stable keys `key = { channel.id }` to prevent unnecessary recompositions during scrolling.
* **Acceptance Criteria**:
  - `GIVEN` 20 categories each containing 50 channels
  - `WHEN` navigating vertically and horizontally across rows with D-pad
  - `THEN` scrolling maintains 60 FPS without stutter or frame drops.

---

#### `FIP-304` [Story] "Continue Watching" Recent Streams Carousel
* **Epic**: `EPIC-4` | **Component**: `UI / Home` | **Points**: 5 SP | **Priority**: Medium
* **User Story**:
  > As a user,  
  > I want the top row of my Home screen to show recently watched channels and movies,  
  > So that I can resume watching my favorite content with a single click.
* **Technical Details**:
  - Connect `GetRecentUseCase` to `HomeViewModel`.
  - Automatically insert into `recents` table upon stream start.
  - Display progress bar for VOD items showing percentage completed.
* **Acceptance Criteria**:
  - `GIVEN` a channel or movie that was played for > 30 seconds
  - `WHEN` opening the Home screen
  - `THEN` the item appears as the first entry in "Continue Watching".

---

### Sprint 4: Live Channel Surfing & EPG Engine (Weeks 7–8)

#### `FIP-401` [Story] Fullscreen Player with Auto-Hiding Controls Overlay
* **Epic**: `EPIC-5` | **Component**: `UI / Player` | **Points**: 8 SP | **Priority**: Blocker
* **User Story**:
  > As a TV viewer,  
  > I want clean fullscreen video playback with an overlay that appears when I press OK and auto-hides after 3 seconds,  
  > So that I can check program info without permanently blocking the screen.
* **Technical Details**:
  - Implement `PlayerScreen.kt` wrapping AndroidX Media3 `PlayerView` inside `AndroidView`.
  - Implement auto-hiding overlay using Coroutine timer (`delay(3000ms)` reset on any key event).
  - Display channel number, channel name, logo, current EPG title, and progress bar.
* **Acceptance Criteria**:
  - `GIVEN` video playback in progress
  - `WHEN` remote `OK` button is clicked
  - `THEN` the overlay displays.
  - `WHEN` no key is pressed for 3 seconds
  - `THEN` the overlay fades out smoothly.

---

#### `FIP-402` [Story] Instant Channel Surfing with D-Pad Up / Down Keys
* **Epic**: `EPIC-5` | **Component**: `UI / Player` | **Points**: 8 SP | **Priority**: High
* **User Story**:
  > As a TV viewer,  
  > I want to press Up and Down on my D-pad to flip to the previous or next channel,  
  > So that channel surfing is instantaneous without returning to the channel guide.
* **Technical Details**:
  - Intercept `KeyEvent.KEYCODE_DPAD_UP` and `KEYCODE_DPAD_DOWN` in `PlayerScreen`.
  - Swap stream via `player.setMediaItem(newItem)` without destroying the ExoPlayer instance.
  - Display a brief "Channel Switching Toast" showing the channel number and name.
* **Acceptance Criteria**:
  - `GIVEN` channel surfing in fullscreen mode
  - `WHEN` pressing `D-Pad Down`
  - `THEN` the next channel plays within 500ms.
  - `AND` channel number overlay shows briefly.

---

#### `FIP-403` [Story] Real-Time Short EPG On-Demand Ingestion
* **Epic**: `EPIC-5` | **Component**: `Data / EPG` | **Points**: 5 SP | **Priority**: High
* **User Story**:
  > As a viewer,  
  > I want to see what is currently playing on the channel I'm watching,  
  > So that I know what show is on and how much time remains.
* **Technical Details**:
  - Query `action=get_short_epg&stream_id={id}` when channel is selected.
  - Decode Base64 titles and descriptions where detected.
  - Calculate progress percentage from `start` and `end` timestamps.
* **Acceptance Criteria**:
  - `GIVEN` channel selection
  - `WHEN` player loads
  - `THEN` current program title, start/end time, and progress bar render accurately.

---

#### `FIP-404` [Story] Streaming XMLTV EPG Background Sync (WorkManager)
* **Epic**: `EPIC-5` | **Component**: `Data / EPG` | **Points**: 8 SP | **Priority**: Medium
* **User Story**:
  > As a user,  
  > I want the app to keep TV guide schedules updated in the background without causing app lag,  
  > So that program data is always available offline.
* **Technical Details**:
  - Implement `EpgSyncWorker.kt` running every 24 hours via `WorkManager`.
  - Stream `xmltv.php` using `XmlPullParser` directly into Room in 500-item chunks.
  - Do NOT load entire 30MB XML into memory; stream line-by-line to prevent OOM.
* **Acceptance Criteria**:
  - `GIVEN` an XMLTV payload of 50MB
  - `WHEN` `EpgSyncWorker` executes
  - `THEN` heap allocation stays `< 30MB`.
  - `AND` program entries populate in `epg_programs` table.

---

### Sprint 5: High-Performance Search & Discovery (Weeks 9–10)

#### `FIP-501` [Story] Sub-2ms Search over 30,000+ Streams via SQLite FTS4
* **Epic**: `EPIC-6` | **Component**: `Database` | **Points**: 5 SP | **Priority**: High
* **User Story**:
  > As a user with 20,000+ channels and movies,  
  > I want search to return results instantaneously as I type,  
  > So that I can find any channel in seconds without app freeze.
* **Technical Details**:
  - Implement `searchChannels(query)` and `searchVod(query)` queries matching on `channels_fts` and `vod_fts`.
  - Append wildcard suffix `:query || '*'` to match prefix tokens.
  - Expose as observable Flow in `SearchViewModel.kt` with a 250ms debounce.
* **Acceptance Criteria**:
  - `GIVEN` 30,000 items in the database
  - `WHEN` user types `"espn"`
  - `THEN` SQLite query executes in `< 2ms`.
  - `AND` matching Live channels and Movies display in categorized sections.

---

#### `FIP-502` [Story] TV Search Screen with On-Screen Remote Keyboard
* **Epic**: `EPIC-6` | **Component**: `UI / Search` | **Points**: 8 SP | **Priority**: High
* **User Story**:
  > As a TV user using a standard remote,  
  > I want an integrated on-screen keyboard grid,  
  > So that I can easily enter search terms using directional arrow keys.
* **Technical Details**:
  - Implement `SearchScreen.kt` with an on-screen QWERTY layout grid.
  - Provide D-pad navigation across keyboard keys with direct delete and clear buttons.
  - Integrate voice search / Android TV IME fallback where available.
* **Acceptance Criteria**:
  - `GIVEN` the Search screen
  - `WHEN` typing letters using D-pad + OK
  - `THEN` the query field updates and filtered results appear above the keyboard in real-time.

---

### Sprint 6: VOD (Movies) & TV Series Subsystems (Weeks 11–12)

#### `FIP-601` [Story] VOD Movies Grid & Category Filter Tabs
* **Epic**: `EPIC-7` | **Component**: `UI / VOD` | **Points**: 5 SP | **Priority**: High
* **User Story**:
  > As a movie fan,  
  > I want to browse on-demand movies by genre with high-res poster art,  
  > So that I can discover movies easily.
* **Technical Details**:
  - Implement `VodScreen.kt` with genre tabs (`Action`, `Comedy`, `Drama`, etc.).
  - Render movie posters with 2:3 aspect ratio and IMDb ratings badge.
  - Implement pagination via `Paging 3` for categories with 1,000+ titles.
* **Acceptance Criteria**:
  - `GIVEN` a VOD category with 500 movies
  - `WHEN` browsing with D-pad
  - `THEN` poster images load progressively with placeholder cross-fades.

---

#### `FIP-602` [Story] VOD Movie Detail Screen & Direct Container Playback
* **Epic**: `EPIC-7` | **Component**: `UI / VOD` | **Points**: 5 SP | **Priority**: High
* **User Story**:
  > As a user,  
  > I want to view movie synopsis, duration, cast, and video quality before playing,  
  > So that I can choose what to watch and resume where I left off.
* **Technical Details**:
  - Fetch detailed movie metadata using `action=get_vod_info&vod_id={id}`.
  - Construct movie playback URL (`/movie/{user}/{pass}/{id}.{ext}`).
  - Track watch position in `recents` table and provide "Resume" vs "Start from Beginning" buttons.
* **Acceptance Criteria**:
  - `GIVEN` a previously watched movie at 45:10
  - `WHEN` opening the detail page and selecting "Resume"
  - `THEN` playback seeks to 45:10 immediately.

---

#### `FIP-603` [Story] TV Series Hierarchy: Seasons, Episodes & Continuous Playback
* **Epic**: `EPIC-7` | **Component**: `Data & UI / Series` | **Points**: 8 SP | **Priority**: High
* **User Story**:
  > As a binge watcher,  
  > I want to browse TV series by season and select individual episodes,  
  > So that I can watch full seasons sequentially.
* **Technical Details**:
  - Implement `SeriesScreen.kt` and `SeriesDetailScreen.kt`.
  - Fetch and parse `action=get_series_info&series_id={id}` returning seasons and episode lists.
  - Construct episode streaming URLs (`/series/{user}/{pass}/{episode_id}.{ext}`).
  - Auto-advance to the next episode when current episode playback ends.
* **Acceptance Criteria**:
  - `GIVEN` a series with 5 seasons
  - `WHEN` selecting Season 2
  - `THEN` episode list populates with titles, episode numbers, and durations.
  - `WHEN` episode finishes playing
  - `THEN` the player prompts or automatically starts the next episode.

---

### Sprint 7: User Personalization, Settings & Dual Form Factor (Weeks 13–14)

#### `FIP-701` [Story] Favorites Management Across Channels, Movies & Series
* **Epic**: `EPIC-8` | **Component**: `Domain & UI` | **Points**: 5 SP | **Priority**: High
* **User Story**:
  > As a user,  
  > I want to star (favorite) channels and movies from both the guide and the player,  
  > So that I can access my preferred content from a dedicated Favorites screen.
* **Technical Details**:
  - Implement `ToggleFavoriteUseCase.kt` inserting/deleting from `favorites` table.
  - Add favorite star toggle button in player overlay and channel cards.
  - Provide dedicated `FavoritesScreen.kt` organized by Live and VOD tabs.
* **Acceptance Criteria**:
  - `GIVEN` a channel marked as favorite
  - `WHEN` opening the Favorites screen
  - `THEN` the channel appears immediately.
  - `WHEN` unstarred in player
  - `THEN` the star updates reactively across all screens.

---

#### `FIP-702` [Story] Settings Screen: Server Configuration & Cache Clear
* **Epic**: `EPIC-8` | **Component**: `UI / Settings` | **Points**: 5 SP | **Priority**: Medium
* **User Story**:
  > As a user,  
  > I want a Settings screen to update my server credentials, clear cache, and view account expiry,  
  > So that I can manage my subscription details.
* **Technical Details**:
  - Implement `SettingsScreen.kt` with fields for Host, Port, Username, and Password.
  - Display account metadata from `user_info` (Status, Expiry Date, Max Connections).
  - Add "Clear Local Cache" button that wipes Room tables without erasing credentials.
* **Acceptance Criteria**:
  - `GIVEN` updated server credentials
  - `WHEN` "Save & Connect" is pressed
  - `THEN` the app verifies credentials against `player_api.php`, updates `DataStore`, and refreshes categories.

---

#### `FIP-703` [Story] Adaptive Mobile & Tablet Layout Support
* **Epic**: `EPIC-8` | **Component**: `UI / Mobile` | **Points**: 8 SP | **Priority**: Medium
* **User Story**:
  > As a mobile/tablet user,  
  > I want the app to support touch controls and portrait/landscape orientation,  
  > So that I can watch IPTV on my phone or tablet when away from my TV.
* **Technical Details**:
  - Check form factor via `UiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION`.
  - Provide touch-based Material 3 layouts (`androidx.compose.material3`) on mobile devices.
  - Enable pinch-to-zoom, swipe gestures for brightness/volume on mobile player.
* **Acceptance Criteria**:
  - `GIVEN` app launched on an Android phone
  - `WHEN` tapping and swiping
  - `THEN` touch gestures work seamlessly and portrait orientation is permitted.

---

### Sprint 8: Performance Hardening, Profiling & Release (Weeks 15–16)

#### `FIP-801` [Task] Fire TV Stick Memory Capping (< 150MB) & Heap Profiling
* **Epic**: `EPIC-9` | **Component**: `Performance` | **Points**: 5 SP | **Priority**: Blocker
* **Description**: Profile and constrain memory usage on low-end Fire TV Stick devices (1GB RAM) to prevent background killing and OOM crashes.
* **Technical Details**:
  - Configure Coil 3 memory cache: `maxSizePercent(0.15)`.
  - Cap ExoPlayer video buffer: `50MB`.
  - Run Android Studio Memory Profiler on channel switching loops of 100 consecutive channels.
* **Acceptance Criteria**:
  - `GIVEN` continuous playback and channel surfing on Fire TV Stick (Gen 2/3)
  - `WHEN` monitored under Memory Profiler
  - `THEN` total app RAM allocation stays strictly `< 150MB` with zero memory leaks.

---

#### `FIP-802` [Task] Baseline Profiles & App Startup Optimization (< 1.5s Cold Start)
* **Epic**: `EPIC-9` | **Component**: `Performance` | **Points**: 5 SP | **Priority**: High
* **Description**: Generate Baseline Profiles using Macrobenchmark to pre-compile critical code paths and achieve sub-1.5s cold starts.
* **Technical Details**:
  - Configure `androidx.benchmark:benchmark-macro-junit4`.
  - Write benchmark tests for app startup, Home screen render, and channel click-to-play.
  - Generate and package `baseline-prof.txt` in release build.
* **Acceptance Criteria**:
  - `GIVEN` a release build with Baseline Profile
  - `WHEN` measuring cold start on a TV device
  - `THEN` Time to Initial Display (TTID) is `< 1,500ms`.

---

#### `FIP-803` [Task] R8 Full Mode Rules, Obfuscation & Final Release Signoff
* **Epic**: `EPIC-9` | **Component**: `Release` | **Points**: 5 SP | **Priority**: High
* **Description**: Enable R8 full mode with optimized shrinking, obfuscation, and lint validation.
* **Technical Details**:
  - Configure `proguard-rules.pro` keeping serialization models and ExoPlayer native codecs.
  - Run `./gradlew lintRelease` with zero errors.
  - Test release APK on Fire TV Stick 4K and Android TV Emulator.
* **Acceptance Criteria**:
  - `GIVEN` release APK build
  - `WHEN` installed on Fire TV hardware
  - `THEN` app starts, authenticates, renders, and streams with zero crashes or missing class exceptions.

---

## 📊 Summary Metrics & Sprint Velocity

```
Sprint 1: 21 SP  (Infra, Dynamic Base URL, Retrofit, Session, Room Schema)
Sprint 2: 29 SP  (SSOT Flow, Media3 Player, StreamErrorHandler, Lifecycle)
Sprint 3: 21 SP  (Compose TV Theme, Top Bar, Home Screen, Recents)
Sprint 4: 29 SP  (Player Overlay, Surfing, Short EPG, XMLTV Sync)
Sprint 5: 13 SP  (SQLite FTS4, TV Remote Search Keyboard)
Sprint 6: 18 SP  (VOD Movies, TV Series, Resume Progress)
Sprint 7: 18 SP  (Favorites, Settings, Mobile Dual Layout)
Sprint 8: 15 SP  (RAM Tuning, Baseline Profiles, R8 Release)
──────────────────────────────────────────────────────────────────────────
Total:   164 Story Points across 8 Sprints (16 Weeks)
```
