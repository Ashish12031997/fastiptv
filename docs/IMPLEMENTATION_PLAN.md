# Implementation Plan — FastIPTV

> 💡 **For Agile/Jira-style Epics, User Stories, Acceptance Criteria, and Story Points, see [BACKLOG.md](BACKLOG.md).**

---

## Sprint & Phase Roadmap Overview

FastIPTV follows **Scrum / Agile methodology** with 2-week sprint increments. Each phase delivers a functional, testable slice of the application.

```
Sprint 1-2 (Foundation & SSOT Cache) ──► Sprint 2-3 (Player & Auto-Recovery)
                 │                                        │
                 ▼                                        ▼
Sprint 3-4 (Compose TV & Home)       ──► Sprint 4-5 (Live TV & EPG Engine)
                 │                                        │
                 ▼                                        ▼
Sprint 5-6 (FTS Search & Series/VOD) ──► Sprint 7-8 (Personalization & Release)
```

---

## Phase 1: Project Scaffold & Modern Build Setup
**Sprint 1** | **Goal**: Android TV/Fire TV project builds with Gradle KTS and latest version catalog.

- [x] Set up `gradle/libs.versions.toml` with Kotlin 2.1+, Compose TV 1.0+, Media3 1.5+, Room 2.6+, Hilt 2.55, Coil 3.1
- [x] Configure `app/build.gradle.kts` (Compose K2 compiler, KSP, Hilt, Room schema export)
- [x] Configure `AndroidManifest.xml` (Leanback launcher intent, `android.hardware.touchscreen=false`, landscape lock)
- [x] Set up `local.properties` credential template & `.gitignore`
- [x] Create `FastIptvApp.kt` (Hilt application class)
- [x] Create `MainActivity.kt` (Single activity hosting Compose TV NavHost)
- [x] Verify: `./gradlew assembleDebug` succeeds and installs on Fire TV/emulator

## Phase 2: Dynamic Networking & Credential Management
**Sprint 1** | **Goal**: Dynamic base URL, credential persistence in DataStore, and Xtream Codes API client.

- [x] Create `SessionManager.kt` backed by Jetpack DataStore (encrypted credentials)
- [x] Create `DynamicBaseUrlInterceptor.kt` (dynamically redirects requests to user's IPTV host)
- [x] Create `AuthInterceptor.kt` (credentials + anti-bot User-Agent injection)
- [x] Create `XtreamApi.kt` (Retrofit interface for Live, VOD, Series, and EPG)
- [x] Create API response DTOs with Kotlinx Serialization (`@Serializable`)
- [x] Create `XtreamUrlBuilder.kt` (constructs `.m3u8`, `.ts`, `.mkv` stream URLs)
- [x] Create `NetworkModule.kt` (Hilt provider for OkHttpClient, Retrofit, XtreamApi)
- [x] Verify: Unit test authenticates against mock server and dynamically redirects host

## Phase 3: Local Cache — Room Database & FTS4 Tables
**Sprint 1–2** | **Goal**: High-speed reactive persistence with full-text search indexing.

- [x] Create Room entities: `ChannelEntity`, `CategoryEntity`, `FavoriteEntity`, `RecentEntity`, `VodEntity`, `SeriesEntity`, `EpisodeEntity`
- [x] Create SQLite FTS4 virtual tables: `ChannelFtsEntity`, `VodFtsEntity`
- [x] Create DAOs with relational queries (LEFT JOIN `favorites` for `is_favorite` flag)
- [x] Create `AppDatabase.kt` and `DatabaseModule.kt` (Hilt)
- [x] Implement reactive SSOT pattern in `IptvRepositoryImpl.kt` (`Flow` + atomic `@Transaction` sync)
- [x] Verify: Channel lists survive app restarts and orphaned entries are automatically purged

## Phase 4: Video Player — Media3 ExoPlayer & Auto-Recovery
**Sprint 2–3** | **Goal**: Sub-500ms playback startup with thread-safe 4-stage error recovery.

- [x] Create `StreamPlayer.kt` with `OkHttpDataSource.Factory` sharing app OkHttpClient
- [x] Configure `DefaultLoadControl` (2.5s playback threshold, 50MB RAM buffer cap)
- [x] Configure `AudioAttributes` with `handleAudioFocus = true` and `WAKE_MODE_NETWORK`
- [x] Implement `StreamErrorHandler.kt` (4-stage pipeline: backoff retry → format switch → socket reset → user alert)
- [x] Implement thread-safe cancellation in `StreamErrorHandler` (`recoveryJob?.cancel()` on channel switch)
- [x] Create `PlayerModule.kt` (Hilt module providing ExoPlayer and LoadControl)
- [x] Wire single-connection guard (`stop()` halts playback and resets session)
- [x] Verify: Live stream recovers from simulated network socket drop without user intervention

## Phase 5: UI — TV Design System & Top Navigation
**Sprint 3** | **Goal**: 10-foot TV theme, spatial focus system, and top navigation bar.

- [x] Create TV design system (`Theme.kt`, `Color.kt`, `Typography.kt` for 10-foot displays)
- [x] Create `AppNavigation.kt` (Compose TV NavHost with all routes)
- [x] Create `TopNavBar.kt` with D-pad focus handling across Live, Movies, Series, Favorites, Search, Settings
- [x] Create reusable `ChannelCard.kt` using `androidx.tv.material3.Card` with native focus scaling and glowing borders
- [x] Verify: D-pad navigates between empty screens and top tabs smoothly

## Phase 6: UI — Home Screen & Continue Watching
**Sprint 3–4** | **Goal**: Netflix-style category carousels with 60 FPS scrolling.

- [x] Create `HomeViewModel.kt` (observes cached categories, favorites, and recents)
- [x] Create `HomeScreen.kt` with `LazyColumn` hosting nested `LazyRow` carousels
- [x] Implement "Continue Watching" row bound to `recents` table via `RecentDao`
- [x] Use stable keys (`key = { ... }`) and Coil 3 image cache tuning
- [x] Verify: Category and recents rows render smoothly with stable keys and reactive updates

## Phase 7: UI — Fullscreen Player & Channel Surfing
**Sprint 4** | **Goal**: Cable-box style channel surfing with D-pad Up/Down and auto-hiding overlay.

- [x] Create `PlayerScreen.kt` with embedded AndroidX Media3 `PlayerView`
- [x] Implement auto-hiding controls overlay (fades after 3 seconds of inactivity)
- [x] Implement instant channel surfing (D-pad Up/Down switches channels without recreating player)
- [x] Add channel number direct input & transient switching toast pill
- [x] Add one-click favorite toggle button (★)
- [x] Implement single-connection safety with DisposableEffect calling `player.stop()`
- [x] Verify: Channel surfing flips streams in < 500ms with smooth channel banner animation

## Phase 8: EPG Engine — Short EPG & Streaming XMLTV Sync
**Sprint 4–5** | **Goal**: Real-time now-playing info and background XMLTV schedule sync.

- [x] Implement on-demand Short EPG fetching in `PlayerViewModel` & `IptvRepository` (auto-decoding Base64 titles)
- [x] Display current program progress bar and remaining time in player overlay
- [x] Create `EpgSyncWorker.kt` (WorkManager job streaming EPG via chunked Room inserts)
- [x] Create `EpgProgramEntity` and `EpgDao` for persistent TV guide caching
- [x] Verify: XMLTV and Short EPG ingestion runs in background without memory leaks

## Phase 9: High-Performance Search Screen
**Sprint 5** | **Goal**: Sub-2ms instant search over 30,000+ items via SQLite FTS4.

- [x] Implement `searchChannels` and `searchVod` in DAOs using FTS4 MATCH queries
- [x] Create `SearchViewModel.kt` with 250ms debounced input flow
- [x] Create `SearchScreen.kt` with TV remote on-screen QWERTY keyboard
- [x] Display categorized results (Live Channels, Movies, Series)
- [x] Verify: Search across 30,000 items executes in < 2ms with zero UI stutter

## Phase 10: VOD Movies & TV Series Subsystems
**Sprint 6** | **Goal**: Hierarchical browsing and resume playback for Movies and Series.

- [x] Create `VodScreen.kt` with genre tabs and 2:3 poster art grid (Paging 3)
- [x] Create `VodDetailScreen.kt` with resume playback vs start-from-beginning options
- [x] Create `SeriesScreen.kt` and `SeriesDetailScreen.kt` (Seasons and Episodes lists)
- [x] Implement auto-play next episode upon episode completion
- [x] Verify: Watch progress saves accurately and resumes at exact millisecond

## Phase 11: Personalization, Settings & Dual Layout
**Sprint 7** | **Goal**: Favorites screen, settings management, and mobile/tablet fallback.

- [x] Create `FavoritesScreen.kt` with Live and VOD tabs
- [x] Create `SettingsScreen.kt` (server configuration, account expiry, cache purge)
- [x] Implement adaptive layout detection (`UiModeManager.currentModeType == UI_MODE_TYPE_TELEVISION`)
- [x] Provide touch-friendly Compose Material 3 UI for phones and tablets
- [x] Verify: Changing credentials in Settings updates session and reconnects seamlessly

## Phase 12: Performance Hardening & Production Release
**Sprint 8** | **Goal**: Low-RAM Fire TV Stick tuning, Baseline Profiles, and release sign-off.

- [x] Profile memory on Fire TV Stick (total app RAM allocation < 150MB)
- [x] Generate Baseline Profiles (`androidx.benchmark:benchmark-macro-junit4`) for < 1.5s cold start
- [x] Configure R8 full-mode obfuscation and ProGuard rules
- [x] Execute continuous channel surfing stress tests (100 consecutive switches without leak)
- [x] Verify: Zero crashes, zero memory leaks, and passes release lint validation
