package com.fastiptv.ui.components

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.fastiptv.ui.navigation.Screen
import com.fastiptv.ui.theme.AccentBlue
import com.fastiptv.ui.theme.DarkSurface
import com.fastiptv.ui.theme.DarkSurfaceElevated
import com.fastiptv.ui.theme.GlassBorder
import com.fastiptv.ui.theme.TextMuted
import com.fastiptv.ui.theme.TextWhite

data class NavItem(val screen: Screen, val label: String, val icon: String? = null)

val NavItems = listOf(
    NavItem(Screen.Movies, "Movies"),
    NavItem(Screen.LiveTv, "Live TV"),
    NavItem(Screen.Series, "Series"),
    NavItem(Screen.Settings, "Settings", "⚙")
)

@Composable
fun TopNavBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    topNavFocusRequester: FocusRequester? = null,
    contentFocusRequester: FocusRequester? = null
) {
    val activeIndex = remember(currentRoute) {
        val idx = NavItems.indexOfFirst {
            it.screen.route == currentRoute ||
                    (it.screen == Screen.LiveTv && currentRoute?.startsWith("live_tv") == true)
        }
        if (idx >= 0) idx else 0
    }

    var focusedIndex by remember { mutableIntStateOf(-1) }
    val focusRequesters = remember { List(NavItems.size) { FocusRequester() } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(DarkSurface.copy(alpha = 0.92f))
                .border(1.dp, GlassBorder, RoundedCornerShape(30.dp))
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo with glowing dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(AccentBlue)
                )
                Text(
                    text = "FAST",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = AccentBlue,
                    fontSize = 22.sp
                )
                Text(
                    text = "IPTV",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontSize = 22.sp
                )
            }

            // Exactly 4 Navigation Tabs in a continuous circular loop
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NavItems.forEachIndexed { index, navItem ->
                    val isSelected = index == activeIndex
                    val isFocused = index == focusedIndex
                    val prevIndex = if (index - 1 < 0) NavItems.lastIndex else index - 1
                    val nextIndex = (index + 1) % NavItems.size

                    Box(
                        modifier = Modifier
                            .focusRequester(focusRequesters[index])
                            .then(
                                if (isSelected && topNavFocusRequester != null) {
                                    Modifier.focusRequester(topNavFocusRequester)
                                } else Modifier
                            )
                            .focusProperties {
                                left = focusRequesters[prevIndex]
                                right = focusRequesters[nextIndex]
                                if (contentFocusRequester != null) {
                                    down = contentFocusRequester
                                }
                            }
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                    when (keyEvent.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                            try {
                                                focusRequesters[nextIndex].requestFocus()
                                                true
                                            } catch (_: Exception) {
                                                false
                                            }
                                        }
                                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                                            try {
                                                focusRequesters[prevIndex].requestFocus()
                                                true
                                            } catch (_: Exception) {
                                                false
                                            }
                                        }
                                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            if (!isSelected) {
                                                onNavigate(navItem.screen)
                                            }
                                            if (contentFocusRequester != null) {
                                                try {
                                                    contentFocusRequester.requestFocus()
                                                    true
                                                } catch (_: Exception) {
                                                    false
                                                }
                                            } else false
                                        }
                                        KeyEvent.KEYCODE_DPAD_CENTER,
                                        KeyEvent.KEYCODE_ENTER,
                                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                            if (!isSelected) {
                                                onNavigate(navItem.screen)
                                            }
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    focusedIndex = index
                                } else if (focusedIndex == index) {
                                    focusedIndex = -1
                                }
                            }
                            .focusable()
                            .clickable {
                                if (!isSelected) {
                                    onNavigate(navItem.screen)
                                }
                            }
                            .clip(RoundedCornerShape(22.dp))
                            .then(
                                if (isFocused) {
                                    Modifier
                                        .background(Color(0xFF2563EB))
                                        .border(2.5.dp, Color(0xFF93C5FD), RoundedCornerShape(22.dp))
                                } else if (isSelected) {
                                    Modifier
                                        .background(DarkSurfaceElevated.copy(alpha = 0.85f))
                                        .border(1.dp, GlassBorder, RoundedCornerShape(22.dp))
                                } else Modifier
                            )
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (navItem.icon != null) {
                                Text(
                                    text = navItem.icon,
                                    fontSize = 15.sp,
                                    color = if (isFocused) Color.White else if (isSelected) Color(0xFF93C5FD) else TextMuted
                                )
                            }
                            Text(
                                text = navItem.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isFocused || isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isFocused) Color.White else if (isSelected) Color(0xFF93C5FD) else TextMuted,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
