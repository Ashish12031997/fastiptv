package com.fastiptv.ui.components

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import android.view.KeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.fastiptv.ui.navigation.Screen
import com.fastiptv.ui.theme.AccentBlue
import com.fastiptv.ui.theme.DarkSurface
import com.fastiptv.ui.theme.DarkSurfaceElevated
import com.fastiptv.ui.theme.GlassBorder
import com.fastiptv.ui.theme.TextMuted
import com.fastiptv.ui.theme.TextWhite

data class NavItem(val screen: Screen, val label: String)

val NavItems = listOf(
    NavItem(Screen.Movies, "Movies"),
    NavItem(Screen.LiveTv, "Live TV"),
    NavItem(Screen.Series, "Series")
)

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
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
    var selectedTabIndex by remember(currentRoute) { mutableIntStateOf(activeIndex) }
    var focusedTabIndex by remember { mutableIntStateOf(-1) }
    var isSettingsFocused by remember { mutableStateOf(false) }
    val isSettingsSelected = currentRoute == Screen.Settings.route

    val settingsFocusRequester = remember { FocusRequester() }
    val seriesTabFocusRequester = remember { FocusRequester() }

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
                .padding(horizontal = 24.dp),
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

            // Exactly 3 Core Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                indicator = { tabPositions, doesTabRowHaveFocus ->
                    if (selectedTabIndex in tabPositions.indices) {
                        androidx.tv.material3.TabRowDefaults.PillIndicator(
                            currentTabPosition = tabPositions[selectedTabIndex],
                            doesTabRowHaveFocus = doesTabRowHaveFocus,
                            activeColor = Color(0xFF2563EB),
                            inactiveColor = Color(0xFF1E3A8A).copy(alpha = 0.6f)
                        )
                    }
                },
                modifier = Modifier
                    .onFocusChanged { focusState ->
                        if (!focusState.hasFocus) {
                            focusedTabIndex = -1
                            selectedTabIndex = activeIndex
                        }
                    }
            ) {
                NavItems.forEachIndexed { index, navItem ->
                    val isSelected = currentRoute == navItem.screen.route ||
                            (navItem.screen == Screen.LiveTv && currentRoute?.startsWith("live_tv") == true)
                    val isTabFocused = focusedTabIndex == index
                    val isLastTab = index == NavItems.lastIndex

                    Tab(
                        selected = isSelected,
                        onFocus = {
                            focusedTabIndex = index
                            selectedTabIndex = index
                        },
                        onClick = {
                            focusedTabIndex = index
                            selectedTabIndex = index
                            if (!isSelected) {
                                onNavigate(navItem.screen)
                            }
                        },
                        colors = androidx.tv.material3.TabDefaults.pillIndicatorTabColors(
                            contentColor = TextMuted,
                            inactiveContentColor = TextMuted,
                            selectedContentColor = Color(0xFF60A5FA),
                            focusedContentColor = Color.White,
                            focusedSelectedContentColor = Color.White
                        ),
                        modifier = Modifier
                            .then(if (isSelected && topNavFocusRequester != null) Modifier.focusRequester(topNavFocusRequester) else Modifier)
                            .then(if (isLastTab) Modifier.focusRequester(seriesTabFocusRequester) else Modifier)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                    if (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                                        if (!isSelected) {
                                            onNavigate(navItem.screen)
                                            true
                                        } else false
                                    } else false
                                } else false
                            }
                            .focusProperties {
                                if (contentFocusRequester != null) {
                                    down = contentFocusRequester
                                }
                                if (isLastTab) {
                                    right = settingsFocusRequester
                                }
                            }
                    ) {
                        // High-contrast large 10-foot TV tab label for elderly users
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(22.dp))
                                .then(
                                    if (isTabFocused) {
                                        Modifier
                                            .background(Color(0xFF2563EB))
                                            .border(2.5.dp, Color(0xFF93C5FD), RoundedCornerShape(22.dp))
                                    } else if (isSelected) {
                                        Modifier.background(DarkSurfaceElevated.copy(alpha = 0.7f))
                                    } else Modifier
                                )
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = navItem.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isTabFocused || isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isTabFocused) Color.White else if (isSelected) Color(0xFF93C5FD) else TextMuted,
                                fontSize = 17.sp
                            )
                        }
                    }
                }
            }

            // Discrete Settings Icon Button on Far Right
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (isSettingsFocused) {
                            Modifier
                                .background(Color(0xFF2563EB))
                                .border(2.5.dp, Color(0xFF93C5FD), RoundedCornerShape(20.dp))
                        } else if (isSettingsSelected) {
                            Modifier
                                .background(DarkSurfaceElevated)
                                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        } else Modifier
                    )
                    .focusRequester(settingsFocusRequester)
                    .focusProperties {
                        left = seriesTabFocusRequester
                        if (contentFocusRequester != null) {
                            down = contentFocusRequester
                        }
                    }
                    .focusable()
                    .clickable { onNavigate(Screen.Settings) }
                    .onFocusChanged { isSettingsFocused = it.isFocused }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "⚙",
                        fontSize = 17.sp,
                        color = if (isSettingsFocused) Color.White else if (isSettingsSelected) Color(0xFF93C5FD) else TextMuted
                    )
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSettingsFocused || isSettingsSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSettingsFocused) Color.White else if (isSettingsSelected) Color(0xFF93C5FD) else TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
