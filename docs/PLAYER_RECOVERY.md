# Player Auto-Recovery — FastIPTV

## Overview

One of FastIPTV's core differentiators is **automatic stream recovery**. IPTV streams are inherently unreliable — they drop, buffer, change formats, and timeout. The player must handle all of this transparently without user intervention.

---

## Recovery Pipeline

```
Stream Error Detected (ExoPlayer Player.Listener.onPlayerError)
    │
    ▼
┌─────────────────────────────────────────────┐
│  Stage 1: RETRY SAME URL                    │
│  Attempts: 3                                │
│  Delay: Exponential backoff (1s → 2s → 4s)  │
│  Action: player.prepare() + player.play()   │
└─────────────┬───────────────────────────────┘
              │ All 3 failed
              ▼
┌─────────────────────────────────────────────┐
│  Stage 2: SWITCH STREAM FORMAT              │
│  .m3u8 → .ts  OR  .ts → .m3u8              │
│  Attempts: 1                                │
│  Delay: Immediate                           │
│  Action: Build alternate URL, setMediaItem  │
└─────────────┬───────────────────────────────┘
              │ Failed
              ▼
┌─────────────────────────────────────────────┐
│  Stage 3: FRESH CONNECTION                  │
│  Clear OkHttp connection pool               │
│  Reset DNS cache                            │
│  Attempts: 1                                │
│  Delay: 2s                                  │
│  Action: Recreate DataSource, retry         │
└─────────────┬───────────────────────────────┘
              │ Failed
              ▼
┌─────────────────────────────────────────────┐
│  Stage 4: USER NOTIFICATION                 │
│  Show "Stream unavailable" overlay          │
│  Options: [Retry] [Switch Channel]          │
│  Auto-retry after 30s if user does nothing  │
└─────────────────────────────────────────────┘
```

---

## Error Classification & Handling

### Network Errors

| Error Code | Description | Recovery |
|---|---|---|
| `ERROR_CODE_IO_NETWORK_CONNECTION_FAILED` | TCP connection refused/reset | Stage 1 (retry with backoff) |
| `ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT` | Connection/read timeout | Stage 1 (retry with backoff) |
| `ERROR_CODE_IO_DNS_FAILED` | DNS resolution failed | Stage 3 (fresh connection) |

### HTTP Errors

| Error Code | Description | Recovery |
|---|---|---|
| `ERROR_CODE_IO_BAD_HTTP_STATUS` (403) | Blocked by server | Stage 2 (switch format) — some formats bypass blocks |
| `ERROR_CODE_IO_BAD_HTTP_STATUS` (404) | Stream not found | Stage 2 (switch format) — try alternate URL pattern |
| `ERROR_CODE_IO_BAD_HTTP_STATUS` (500+) | Server error | Stage 1 (retry with backoff) |

### Source/Parser Errors

| Error Code | Description | Recovery |
|---|---|---|
| `ERROR_CODE_PARSING_CONTAINER_MALFORMED` | Corrupted stream data | Stage 2 (switch format) |
| `ERROR_CODE_PARSING_MANIFEST_MALFORMED` | Bad HLS manifest | Stage 2 (switch to .ts) |
| `ERROR_CODE_IO_UNSPECIFIED` | Generic source error | Stage 1 then Stage 2 |

### Decoder Errors

| Error Code | Description | Recovery |
|---|---|---|
| `ERROR_CODE_DECODER_INIT_FAILED` | Codec not supported | Disable track, retry with remaining tracks |
| `ERROR_CODE_DECODER_QUERY_FAILED` | No suitable decoder found | Disable track, retry |
| `ERROR_CODE_DECODING_FAILED` | Runtime decode failure | Stage 1 (usually transient) |

### Live-Specific Errors

| Error Code | Description | Recovery |
|---|---|---|
| `BehindLiveWindowException` | Player fell behind live edge | Seek to `player.currentLiveOffset`, continue |

---

## Implementation

### StreamErrorHandler.kt (Pseudocode)

```kotlin
class StreamErrorHandler(
    private val urlBuilder: XtreamUrlBuilder,
    private val config: ServerConfig
) : Player.Listener {

    private var retryCount = 0
    private var currentFormat = "m3u8"  // or "ts"
    private var currentStreamId: Int = 0

    override fun onPlayerError(error: PlaybackException) {
        when {
            // Stage 1: Retry with backoff
            retryCount < 3 -> {
                retryCount++
                val delayMs = (1000L * 2.0.pow(retryCount - 1)).toLong()
                scope.launch {
                    delay(delayMs)
                    player.prepare()
                    player.play()
                }
            }

            // Stage 2: Switch format
            retryCount == 3 -> {
                retryCount++
                currentFormat = if (currentFormat == "m3u8") "ts" else "m3u8"
                val newUrl = urlBuilder.liveStreamUrl(config, currentStreamId, currentFormat)
                player.setMediaItem(MediaItem.fromUri(newUrl))
                player.prepare()
                player.play()
            }

            // Stage 3: Fresh connection
            retryCount == 4 -> {
                retryCount++
                okHttpClient.connectionPool.evictAll()
                scope.launch {
                    delay(2000)
                    player.prepare()
                    player.play()
                }
            }

            // Stage 4: Give up, notify user
            else -> {
                _playerState.value = PlayerState.Error(
                    message = "Stream unavailable",
                    canRetry = true
                )
                // Auto-retry after 30s
                scope.launch {
                    delay(30_000)
                    resetRetryState()
                    player.prepare()
                    player.play()
                }
            }
        }
    }

    // Reset on successful playback
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) resetRetryState()
    }

    fun onChannelChanged(streamId: Int) {
        currentStreamId = streamId
        resetRetryState()
    }

    private fun resetRetryState() {
        retryCount = 0
    }
}
```

---

## Buffer Configuration

Optimized for live IPTV streaming on TV devices:

```kotlin
DefaultLoadControl.Builder()
    .setBufferDurationsMs(
        /* minBufferMs */                  15_000,   // 15s — keep this much ahead
        /* maxBufferMs */                  60_000,   // 60s — max buffer size
        /* bufferForPlaybackMs */           2_500,   // 2.5s — start playing after this
        /* bufferForPlaybackAfterRebuffer */ 5_000   // 5s — after rebuffer, wait longer
    )
    .setTargetBufferBytes(/* 50MB */ 50 * 1024 * 1024)
    .setPrioritizeTimeOverSizeThresholds(true)
    .build()
```

### Why These Values?

| Parameter | Value | Rationale |
|---|---|---|
| `bufferForPlaybackMs` | 2.5s | Fast first-frame: start playing ASAP |
| `minBufferMs` | 15s | Enough to survive short network hiccups |
| `maxBufferMs` | 60s | TV devices have more RAM, buffer aggressively |
| `bufferAfterRebuffer` | 5s | After a stall, build a bigger buffer before resuming |
| `targetBufferBytes` | 50MB | Prevent OOM on low-memory Fire TV Stick |

---

## Channel Switching Optimization

### The Problem
Naively switching channels means: stop → destroy player → create player → set source → buffer → play. This takes 3-5 seconds.

### The Solution: Player Reuse

```kotlin
// DON'T DO THIS:
player.release()
player = ExoPlayer.Builder(context).build()
player.setMediaItem(newItem)
player.prepare()

// DO THIS:
player.setMediaItem(newItem)  // Swap source in-place
player.prepare()               // Reconnects immediately
player.play()                  // Starts as soon as buffer threshold met
```

### Result
Channel switch time: **< 500ms** (first frame) on typical IPTV streams.

---

## Monitoring & Logging

For debugging in development builds:

```kotlin
player.addAnalyticsListener(object : AnalyticsListener {
    override fun onLoadStarted(eventTime, loadEventInfo, mediaLoadData) {
        Log.d("Player", "Loading: ${loadEventInfo.uri}")
    }
    override fun onLoadCompleted(eventTime, loadEventInfo, mediaLoadData) {
        Log.d("Player", "Loaded: ${mediaLoadData.trackFormat?.sampleMimeType}")
    }
    override fun onVideoSizeChanged(eventTime, videoSize) {
        Log.d("Player", "Video: ${videoSize.width}x${videoSize.height}")
    }
    override fun onDroppedVideoFrames(eventTime, droppedFrames, elapsedMs) {
        Log.w("Player", "Dropped $droppedFrames frames in ${elapsedMs}ms")
    }
})
```
