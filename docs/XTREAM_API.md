# Xtream Codes API Reference — FastIPTV

Complete reference for the Xtream Codes API used by IPTV providers.

---

## Base Configuration

| Field | Value |
|---|---|
| **Base URL** | `http://<HOST>:<PORT>` |
| **API Endpoint** | `/player_api.php` |
| **Auth Method** | Query parameters: `username=<USER>&password=<PASS>` |
| **Response Format** | JSON |

### Required Headers

```http
User-Agent: IPTVSmartersPro/1.0.0 (Linux;Android 11) ExoPlayerLib/2.18.2
Accept: application/json
```

> ⚠️ Many IPTV panels block default user agents (`okhttp`, `python-requests`, `curl`). Always set a player-like User-Agent.

---

## 1. Authentication & Server Handshake

### Request
```
GET /player_api.php?username={USER}&password={PASS}
```
*(No `action` parameter)*

### Response
```json
{
  "user_info": {
    "username": "user123",
    "password": "secret",
    "auth": 1,
    "status": "Active",
    "exp_date": "1735689600",
    "is_trial": "0",
    "active_cons": 0,
    "max_connections": "1",
    "allowed_output_formats": ["m3u8", "ts", "rtmp"]
  },
  "server_info": {
    "url": "your-provider.com",
    "port": "8080",
    "https_port": "8443",
    "server_protocol": "http",
    "timezone": "Europe/London",
    "timestamp_now": 1789441920
  }
}
```

### Key Fields
| Field | Type | Description |
|---|---|---|
| `auth` | `Int` | `1` = valid, `0` = invalid |
| `status` | `String` | `Active`, `Banned`, `Disabled`, `Expired` |
| `exp_date` | `String?` | Unix epoch or `null` (lifetime) |
| `max_connections` | `String` | Max concurrent streams |
| `allowed_output_formats` | `List<String>` | Supported stream formats |

---

## 2. Live TV

### 2.1 Get Live Categories
```
GET /player_api.php?...&action=get_live_categories
```

```json
[
  { "category_id": "1", "category_name": "USA | GENERAL", "parent_id": 0 },
  { "category_id": "2", "category_name": "SPORTS", "parent_id": 0 }
]
```

### 2.2 Get Live Streams
```
GET /player_api.php?...&action=get_live_streams
GET /player_api.php?...&action=get_live_streams&category_id=1   # Filtered
```

```json
[
  {
    "num": 1,
    "name": "USA: CNN HD",
    "stream_type": "live",
    "stream_id": 10542,
    "stream_icon": "http://host/images/logos/cnn.png",
    "epg_channel_id": "cnn.us",
    "added": "1610000000",
    "category_id": "1",
    "tv_archive": 1,
    "tv_archive_duration": 3
  }
]
```

| Field | Description |
|---|---|
| `stream_id` | Used to construct playback URL |
| `stream_icon` | Channel logo URL |
| `epg_channel_id` | Links to EPG data |
| `tv_archive` | `1` = catch-up supported |
| `tv_archive_duration` | Days of catch-up available |

---

## 3. VOD (Movies)

### 3.1 Get VOD Categories
```
GET /player_api.php?...&action=get_vod_categories
```
*Same format as live categories.*

### 3.2 Get VOD Streams
```
GET /player_api.php?...&action=get_vod_streams
GET /player_api.php?...&action=get_vod_streams&category_id=5
```

```json
[
  {
    "num": 1,
    "name": "Inception (2010)",
    "stream_type": "movie",
    "stream_id": 45892,
    "stream_icon": "http://host/images/posters/inception.jpg",
    "rating": "8.8",
    "rating_5based": 4.4,
    "added": "1612000000",
    "category_id": "5",
    "container_extension": "mkv"
  }
]
```

> `container_extension` is required to build the playback URL.

### 3.3 Get VOD Info (Full Details)
```
GET /player_api.php?...&action=get_vod_info&vod_id=45892
```

```json
{
  "info": {
    "tmdb_id": "27205",
    "name": "Inception",
    "cover_big": "http://.../poster_large.jpg",
    "releasedate": "2010-07-16",
    "episode_run_time": "148",
    "youtube_trailer": "YoHD9XEInc0",
    "director": "Christopher Nolan",
    "actors": "Leonardo DiCaprio, Joseph Gordon-Levitt",
    "description": "A thief who steals corporate secrets...",
    "genre": "Action, Science Fiction",
    "duration": "02:28:00",
    "rating": "8.8",
    "video": {
      "codec_name": "h264",
      "width": 1920,
      "height": 1080,
      "bit_rate": "5500000"
    },
    "audio": {
      "codec_name": "aac",
      "channels": 6,
      "channel_layout": "5.1"
    }
  },
  "movie_data": {
    "stream_id": 45892,
    "name": "Inception",
    "container_extension": "mkv"
  }
}
```

---

## 4. TV Series

### 4.1 Get Series Categories
```
GET /player_api.php?...&action=get_series_categories
```

### 4.2 Get Series List
```
GET /player_api.php?...&action=get_series
GET /player_api.php?...&action=get_series&category_id=3
```

```json
[
  {
    "name": "Breaking Bad",
    "series_id": 312,
    "cover": "http://.../bb_cover.jpg",
    "plot": "A chemistry teacher...",
    "cast": "Bryan Cranston, Aaron Paul",
    "genre": "Crime, Drama",
    "rating": "9.5",
    "rating_5based": 4.8
  }
]
```

### 4.3 Get Series Info & Episodes
```
GET /player_api.php?...&action=get_series_info&series_id=312
```

```json
{
  "seasons": [
    { "season_number": 1, "name": "Season 1", "episode_count": 7 }
  ],
  "info": { "name": "Breaking Bad", "cover": "...", "rating": "9.5" },
  "episodes": {
    "1": [
      {
        "id": "7801",
        "episode_num": 1,
        "title": "Pilot",
        "container_extension": "mkv",
        "season": 1,
        "info": {
          "duration": "00:58:00",
          "video": { "width": 1920, "height": 1080, "codec_name": "h264" }
        }
      }
    ]
  }
}
```

---

## 5. EPG (Electronic Program Guide)

### 5.1 Short EPG (Per Channel)
```
GET /player_api.php?...&action=get_short_epg&stream_id=10542&limit=10
```

```json
{
  "epg_listings": [
    {
      "title": "CNN Newsroom",
      "start": "2026-09-14 03:00:00",
      "end": "2026-09-14 04:00:00",
      "description": "Live reporting and news analysis.",
      "now_playing": 1,
      "has_archive": 1
    }
  ]
}
```

> ⚠️ `title` and `description` may be **Base64-encoded** on some panels. Decode and handle gracefully.

### 5.2 Full XMLTV EPG
```
GET /xmltv.php?username={USER}&password={PASS}
```

Returns standard XMLTV XML. Can be very large (10-50MB). Use `Accept-Encoding: gzip`.

---

## 6. Stream Playback URLs

### Live TV
| Format | URL Pattern |
|---|---|
| **HLS** | `http://{HOST}:{PORT}/live/{USER}/{PASS}/{STREAM_ID}.m3u8` |
| **MPEG-TS** | `http://{HOST}:{PORT}/{USER}/{PASS}/{STREAM_ID}.ts` |

### VOD (Movies)
```
http://{HOST}:{PORT}/movie/{USER}/{PASS}/{STREAM_ID}.{CONTAINER_EXT}
```
Example: `http://host:8080/movie/user/pass/45892.mkv`

### TV Series Episodes
```
http://{HOST}:{PORT}/series/{USER}/{PASS}/{EPISODE_ID}.{CONTAINER_EXT}
```
Example: `http://host:8080/series/user/pass/7801.mkv`

### Catch-up / Timeshift
```
http://{HOST}:{PORT}/timeshift/{USER}/{PASS}/{DURATION_MIN}/{YYYY-MM-DD:HH-MM}/{STREAM_ID}.ts
```

### Full M3U Playlist Export
```
GET /get.php?username={USER}&password={PASS}&type=m3u_plus&output=ts
```
Use `output=m3u8` for HLS format.

---

## 7. Endpoint Summary Table

| Resource | Action | Endpoint |
|---|---|---|
| Auth/Handshake | *(none)* | `player_api.php?username=...&password=...` |
| Live Categories | `get_live_categories` | `player_api.php?...&action=get_live_categories` |
| Live Streams | `get_live_streams` | `player_api.php?...&action=get_live_streams[&category_id=N]` |
| VOD Categories | `get_vod_categories` | `player_api.php?...&action=get_vod_categories` |
| VOD Streams | `get_vod_streams` | `player_api.php?...&action=get_vod_streams[&category_id=N]` |
| VOD Info | `get_vod_info` | `player_api.php?...&action=get_vod_info&vod_id=N` |
| Series Categories | `get_series_categories` | `player_api.php?...&action=get_series_categories` |
| Series List | `get_series` | `player_api.php?...&action=get_series[&category_id=N]` |
| Series Info | `get_series_info` | `player_api.php?...&action=get_series_info&series_id=N` |
| Short EPG | `get_short_epg` | `player_api.php?...&action=get_short_epg&stream_id=N&limit=N` |
| Full XMLTV | *(xmltv.php)* | `xmltv.php?username=...&password=...` |
| M3U Export | *(get.php)* | `get.php?username=...&password=...&type=m3u_plus&output=ts` |
