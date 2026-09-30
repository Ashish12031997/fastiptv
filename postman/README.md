# FastIPTV Postman Collection & API Suite

This folder contains a ready-to-use Postman collection and environment covering all **Xtream Codes IPTV APIs**, **Direct Stream Playback URLs**, and **FastIPTV OTA Update APIs**.

---

## 📁 Files Included

| File | Description |
|---|---|
| [`FastIPTV_API.postman_collection.json`](file:///Users/aashishpatadiya/Documents/projects/fastiptv/postman/FastIPTV_API.postman_collection.json) | Complete Postman collection with 21 endpoints, automated test assertions, and sample responses. |
| [`FastIPTV_Environment.postman_environment.json`](file:///Users/aashishpatadiya/Documents/projects/fastiptv/postman/FastIPTV_Environment.postman_environment.json) | Environment variable template containing your server URL, credentials, and dynamic IDs. |

---

## 🚀 How to Import into Postman

1. Open **Postman** (or Insomnia / Bruno / VS Code Thunder Client).
2. Click **Import** (top left).
3. Drag and drop both files:
   - `FastIPTV_API.postman_collection.json`
   - `FastIPTV_Environment.postman_environment.json`
4. In the top right environment dropdown, select **FastIPTV Environment**.

---

## ⚙️ Configuration (Only 3 Variables Required)

Open the **FastIPTV Environment** and set your actual IPTV details:

| Variable | Example Value | Description |
|---|---|---|
| `base_url` | `http://provider-domain.com:8080` | IPTV server address with protocol and port (no trailing slash) |
| `username` | `my_iptv_user` | Your IPTV account username |
| `password` | `my_iptv_pass` | Your IPTV account password |

> **Note:** The remaining variables (`live_category_id`, `live_stream_id`, `vod_stream_id`, `series_id`, `episode_id`) are **automatically populated** by the test scripts as you run requests!

---

## 📋 API Overview & Endpoints

### 1. 01. Authentication & System
- **Authenticate & Server Info**: `GET /player_api.php?username={{username}}&password={{password}}`
  - Validates credentials and returns account validity, expiration date, active/max connections, server timezone, and open ports.

### 2. 02. Live TV
- **Get Live Categories**: `GET /player_api.php?...&action=get_live_categories`
  - Returns channel categories (Sports, News, Entertainment, etc.). Automatically sets `{{live_category_id}}`.
- **Get All Live Streams**: `GET /player_api.php?...&action=get_live_streams`
  - Returns complete channel lineup. Automatically sets `{{live_stream_id}}`.
- **Get Live Streams by Category**: `GET /player_api.php?...&action=get_live_streams&category_id={{live_category_id}}`
  - Returns channels filtered by the selected category.

### 3. 03. VOD (Movies)
- **Get VOD Categories**: `GET /player_api.php?...&action=get_vod_categories`
  - Returns movie genres/categories. Automatically sets `{{vod_category_id}}`.
- **Get All VOD Streams**: `GET /player_api.php?...&action=get_vod_streams`
  - Returns movie catalog. Automatically sets `{{vod_stream_id}}` and `{{vod_extension}}`.
- **Get VOD Streams by Category**: `GET /player_api.php?...&action=get_vod_streams&category_id={{vod_category_id}}`
- **Get VOD Info (Full Metadata)**: `GET /player_api.php?...&action=get_vod_info&vod_id={{vod_stream_id}}`
  - Returns rich metadata: plot summary, cast, director, TMDB ID, backdrop, trailer, runtime, video codecs, and audio channels.

### 4. 04. TV Series
- **Get Series Categories**: `GET /player_api.php?...&action=get_series_categories`
  - Automatically sets `{{series_category_id}}`.
- **Get All Series**: `GET /player_api.php?...&action=get_series`
  - Automatically sets `{{series_id}}`.
- **Get Series by Category**: `GET /player_api.php?...&action=get_series&category_id={{series_category_id}}`
- **Get Series Info & Episodes**: `GET /player_api.php?...&action=get_series_info&series_id={{series_id}}`
  - Returns seasons breakdown and full episode list grouped by season. Automatically sets `{{episode_id}}` and `{{series_extension}}`.

### 5. 05. EPG (Electronic Program Guide)
- **Get Short EPG for Channel**: `GET /player_api.php?...&action=get_short_epg&stream_id={{live_stream_id}}&limit=10`
  - Returns current (`now_playing`) and upcoming schedule for a channel.
- **Get Simple Data Table EPG**: `GET /player_api.php?...&action=get_simple_data_table&stream_id={{live_stream_id}}`
  - Raw table format for EPG.
- **Download Full XMLTV EPG**: `GET /xmltv.php?username={{username}}&password={{password}}`
  - Standard XMLTV guide for all channels. *(Note: Can be 10MB - 50MB+).*

### 6. 06. Stream Playback & Media URLs
Direct streaming URLs used by the video player:
- **Stream Live Channel (HLS)**: `GET /live/{{username}}/{{password}}/{{live_stream_id}}.m3u8`
- **Stream Live Channel (MPEG-TS)**: `GET /live/{{username}}/{{password}}/{{live_stream_id}}.ts`
- **Stream Movie (VOD)**: `GET /movie/{{username}}/{{password}}/{{vod_stream_id}}.{{vod_extension}}`
- **Stream Series Episode**: `GET /series/{{username}}/{{password}}/{{episode_id}}.{{series_extension}}`
- **Export M3U Plus Playlist**: `GET /get.php?username={{username}}&password={{password}}&type=m3u_plus&output=ts`

### 7. 07. App Maintenance & Updates
- **Check Latest FastIPTV Release**: `GET https://api.github.com/repos/Ashish12031997/fastiptv/releases/latest`
  - Official GitHub API called by FastIPTV's OTA manager to check for new APK releases.

---

## 🛡️ User-Agent Protection
Many IPTV panels and Cloudflare reverse proxies block requests that use standard HTTP client User-Agents (`PostmanRuntime`, `curl`, `okhttp`).

All requests in this collection are pre-configured with:
```http
User-Agent: IPTVSmartersPro/1.0.0 (Linux;Android 11) ExoPlayerLib/2.18.2
Accept: application/json
```
This guarantees your requests mimic a legitimate IPTV player and won't get rejected with `HTTP 403 Forbidden`.

---

## ⚡ Automated Testing via Newman (CLI)

You can run the entire collection headlessly from your terminal using Newman:

```bash
# Run with environment file
npx -y newman run postman/FastIPTV_API.postman_collection.json \
  -e postman/FastIPTV_Environment.postman_environment.json \
  --env-var "base_url=http://your-iptv-host.com:8080" \
  --env-var "username=your_username" \
  --env-var "password=your_password"
```
