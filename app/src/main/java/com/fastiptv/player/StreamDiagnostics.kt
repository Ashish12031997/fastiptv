package com.fastiptv.player

data class StreamDiagnostics(
    val resolution: String = "Detecting...",
    val frameRate: String = "N/A",
    val videoCodec: String = "N/A",
    val audioCodec: String = "N/A",
    val audioChannels: String = "Stereo",
    val bitrateFormatted: String = "N/A",
    val bufferHealthSec: Float = 0f,
    val droppedFrames: Int = 0,
    val streamFormat: String = "HLS",
    val isHardwareAccelerated: Boolean = true
)

data class PlayerTrackOption(
    val id: String,
    val label: String,
    val language: String? = null,
    val isSelected: Boolean = false,
    val groupIndex: Int = 0,
    val trackIndex: Int = 0
)

enum class AspectRatioMode(val title: String, val modeInt: Int) {
    FIT("Fit (Original)", 0),
    FILL("Stretch (Full Screen)", 3),
    ZOOM("Zoom (Crop Borders)", 4)
}
