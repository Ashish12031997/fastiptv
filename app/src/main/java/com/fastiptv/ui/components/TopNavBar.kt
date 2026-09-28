package com.fastiptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class NavItem(val screen: Screen, val label: String)

val NavItems = listOf(
    NavItem(Screen.Home, "Home"),
    NavItem(Screen.Epg, "TV Guide"),
    NavItem(Screen.Movies, "Movies"),
    NavItem(Screen.Series, "Series"),
    NavItem(Screen.Favorites, "Favorites"),
    NavItem(Screen.Search, "Search"),
    NavItem(Screen.Settings, "Settings")
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
        NavItems.indexOfFirst { it.screen.route == currentRoute }.coerceAtLeast(0)
    }
    var selectedTabIndex by remember(currentRoute) { mutableIntStateOf(activeIndex) }
    var focusedTabIndex by remember { mutableIntStateOf(-1) }
    val coroutineScope = rememberCoroutineScope()
    var navDebounceJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(DarkSurface.copy(alpha = 0.90f))
                .border(1.dp, GlassBorder, RoundedCornerShape(29.dp))
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo with glowing dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
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

            // Navigation Tabs
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
                    .focusProperties {
                        if (topNavFocusRequester != null) {
                            onEnter = { topNavFocusRequester }
                        }
                    }
            ) {
                NavItems.forEachIndexed { index, navItem ->
                    val isSelected = currentRoute == navItem.screen.route
                    val isTabFocused = focusedTabIndex == index

                    Tab(
                        selected = isSelected,
                        onFocus = {
                            focusedTabIndex = index
                            selectedTabIndex = index

                            if (currentRoute != navItem.screen.route) {
                                val diff = kotlin.math.abs(index - activeIndex)
                                if (diff <= 1) {
                                    // Debounce screen transition so rapid D-pad traversal is 60fps smooth
                                    navDebounceJob?.cancel()
                                    navDebounceJob = coroutineScope.launch {
                                        delay(350)
                                        if (currentRoute != navItem.screen.route) {
                                            onNavigate(navItem.screen)
                                        }
                                    }
                                }
                            }
                        },
                        onClick = {
                            focusedTabIndex = index
                            selectedTabIndex = index
                            navDebounceJob?.cancel()
                            if (currentRoute != navItem.screen.route) {
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
                    ) {
                        // High-contrast 10-foot TV tab label with instant focus feedback
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .then(
                                    if (isTabFocused) {
                                        Modifier
                                            .background(Color(0xFF2563EB))
                                            .border(2.dp, Color(0xFF60A5FA), RoundedCornerShape(20.dp))
                                    } else if (isSelected) {
                                        Modifier.background(DarkSurfaceElevated.copy(alpha = 0.6f))
                                    } else Modifier
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = navItem.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isTabFocused || isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isTabFocused) Color.White else if (isSelected) Color(0xFF93C5FD) else TextMuted,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
