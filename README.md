# 🚀 FastIPTV

A blazing-fast, TV-first IPTV player for Android & Fire TV. Built with native Kotlin, Jetpack Compose TV, and Media3 ExoPlayer.

> **Goal**: Zero-lag channel switching, instant search across 1000s of channels, and auto-recovery streaming that just works.

---

## ✨ Features (MVP)

| Feature | Description |
|---|---|
| 📺 **Live TV** | Browse and play live channels grouped by category |
| 🎬 **Movies (VOD)** | Browse, search, and play on-demand movies |
| 🔍 **Instant Search** | Sub-millisecond fuzzy search across all content |
| ⭐ **Favorites** | Save and quickly access favorite channels |
| 🕐 **Recent** | Continue watching from where you left off |
| 🔄 **Auto-Recovery** | 4-stage retry pipeline: same URL → format switch → fresh connection → notify |
| 🎮 **TV Remote Navigation** | Full d-pad support, channel surfing with ↑↓ in player |
| 📱 **Dual Layout** | TV-first with phone/tablet adaptation |

## 🔮 Future Features

- EPG / TV Guide grid
- TV Series (seasons + episodes)
- Catch-up / Timeshift playback
- Picture-in-Picture (PiP)
- Parental controls
- Multi-language audio track selection
- Subtitle support

---

## 🛠 Tech Stack

| Layer | Technology | Why |
|---|---|---|
| **Language** | Kotlin 2.1+ | Coroutines, null safety, modern syntax |
| **Min SDK** | API 21 (Android 5.0) | Fire TV Gen 1+ compatibility |
| **UI** | Jetpack Compose + TV Compose | Declarative, fast rendering, built-in TV focus system |
| **Video Player** | AndroidX Media3 (ExoPlayer) | Best HLS/TS/DASH player, adaptive bitrate, error recovery |
| **Networking** | Retrofit + OkHttp + Kotlinx Serialization | Fast HTTP, connection pooling, zero-reflection JSON |
| **Local Cache** | Room Database | Offline channel lists, favorites, recent channels |
| **DI** | Hilt (Dagger) | Compile-time injection, no runtime overhead |
| **Image Loading** | Coil 3 | Kotlin-first, Compose-native, memory-efficient |
| **Architecture** | Clean Architecture (MVVM) | Testable, maintainable, separation of concerns |
| **Build** | Gradle KTS + Version Catalog | Type-safe dependencies |

---

## 📦 IPTV Backend

This app integrates with **Xtream Codes API** — the industry-standard IPTV middleware used by most providers.

- **Authentication**: Username + Password via HTTP query params
- **API Endpoint**: `player_api.php` with action-based routing
- **Stream Formats**: HLS (`.m3u8`), MPEG-TS (`.ts`), direct container (`.mp4`, `.mkv`)
- **EPG**: Short EPG per channel + full XMLTV export

See [`docs/XTREAM_API.md`](docs/XTREAM_API.md) for full API reference.

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Ladybug (2024.2+) or newer
- Android SDK 35
- JDK 17+
- Fire TV device or Android TV emulator (optional for TV testing)

### Setup

1. **Clone the repo**:
   ```bash
   git clone <repo-url>
   cd fastiptv
   ```

2. **Configure SDK & optional local properties** — copy `local.properties.example` to `local.properties`:
   ```bash
   cp local.properties.example local.properties
   ```
   Set your Android SDK path in `local.properties`. IPTV credentials can also be configured directly within the app's **Settings** screen on your TV.

3. **Build & Run**:
   ```bash
   ./gradlew installDebug
   ```

4. **Run on Fire TV**:
   ```bash
   adb connect <fire-tv-ip>:5555
   adb install app/build/outputs/apk/debug/FastIPTV-debug.apk
   ```

### Testing

```bash
# Unit tests
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug
```

---

## 📁 Project Structure

```
fastiptv/
├── app/src/main/java/com/fastiptv/
│   ├── FastIptvApp.kt              # Application class (Hilt)
│   ├── MainActivity.kt             # Single activity, Compose host
│   ├── di/                          # Hilt DI modules
│   │   ├── NetworkModule.kt
│   │   ├── DatabaseModule.kt
│   │   └── PlayerModule.kt
│   ├── data/                        # Data layer
│   │   ├── api/                     # Xtream Codes API
│   │   │   ├── XtreamApi.kt        # Retrofit interface
│   │   │   ├── XtreamUrlBuilder.kt # Stream URL constructor
│   │   │   └── AuthInterceptor.kt  # Credential injection
│   │   ├── db/                      # Room database
│   │   │   ├── AppDatabase.kt
│   │   │   ├── ChannelDao.kt
│   │   │   └── FavoriteDao.kt
│   │   ├── model/                   # API response DTOs
│   │   │   ├── AuthResponse.kt
│   │   │   ├── Category.kt
│   │   │   ├── LiveStream.kt
│   │   │   └── VodStream.kt
│   │   └── repository/              # Repository implementations
│   │       └── IptvRepositoryImpl.kt
│   ├── domain/                      # Domain layer
│   │   ├── model/                   # Domain models
│   │   │   ├── Channel.kt
│   │   │   ├── Movie.kt
│   │   │   └── ServerConfig.kt
│   │   ├── repository/              # Repository interfaces
│   │   │   └── IptvRepository.kt
│   │   └── usecase/                 # Use cases
│   │       ├── GetChannelsUseCase.kt
│   │       ├── SearchContentUseCase.kt
│   │       └── ToggleFavoriteUseCase.kt
│   ├── player/                      # Video player
│   │   ├── StreamPlayer.kt         # ExoPlayer lifecycle manager
│   │   ├── StreamErrorHandler.kt   # Auto-recovery logic
│   │   └── PlayerState.kt          # Playback state model
│   └── ui/                          # Presentation layer
│       ├── theme/                   # Colors, typography, shapes
│       ├── navigation/              # Compose nav graph
│       ├── home/                    # Home screen
│       ├── channels/                # Channel list/grid
│       ├── player/                  # Fullscreen player
│       ├── search/                  # Search screen
│       ├── vod/                     # Movies browsing
│       ├── favorites/               # Favorites screen
│       ├── settings/                # Settings screen
│       └── components/              # Shared composables
├── docs/                            # Documentation
│   ├── ARCHITECTURE.md
│   ├── XTREAM_API.md
│   └── PLAYER_RECOVERY.md
├── gradle/libs.versions.toml       # Version catalog
├── local.properties                 # IPTV credentials (git-ignored)
└── .gitignore
```

---

## 📄 License

Private project — not for distribution.
