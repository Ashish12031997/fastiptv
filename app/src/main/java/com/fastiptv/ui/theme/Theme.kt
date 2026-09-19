package com.fastiptv.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val DarkTvColorScheme = darkColorScheme(
    primary = AccentBlue,
    primaryContainer = AccentBlueVariant,
    secondary = LiveRed,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceElevated,
    onPrimary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextMuted
)

@Composable
fun FastIptvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkTvColorScheme,
        typography = TvTypography,
        content = content
    )
}
