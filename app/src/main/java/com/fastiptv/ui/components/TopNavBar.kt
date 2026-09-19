package com.fastiptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import com.fastiptv.ui.navigation.Screen
import com.fastiptv.ui.theme.AccentBlue
import com.fastiptv.ui.theme.DarkSurface
import com.fastiptv.ui.theme.DarkSurfaceElevated
import com.fastiptv.ui.theme.GlassBorder
import com.fastiptv.ui.theme.TextMuted
import com.fastiptv.ui.theme.TextWhite

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
                .background(DarkSurface.copy(alpha = 0.85f))
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
                            activeColor = AccentBlue,
                            inactiveColor = DarkSurfaceElevated
                        )
                    }
                },
                modifier = Modifier.focusProperties {
                    if (topNavFocusRequester != null) {
                        onEnter = { topNavFocusRequester }
                    }
                }
            ) {
                NavItems.forEachIndexed { index, navItem ->
                    val isSelected = currentRoute == navItem.screen.route
                    Tab(
                        selected = isSelected,
                        onFocus = {
                            if (currentRoute != navItem.screen.route) {
                                val diff = kotlin.math.abs(index - activeIndex)
                                if (diff <= 1) {
                                    selectedTabIndex = index
                                    onNavigate(navItem.screen)
                                }
                            } else {
                                selectedTabIndex = index
                            }
                        },
                        onClick = {
                            selectedTabIndex = index
                            if (currentRoute != navItem.screen.route) {
                                onNavigate(navItem.screen)
                            }
                        },
                        colors = androidx.tv.material3.TabDefaults.pillIndicatorTabColors(
                            contentColor = TextMuted,
                            inactiveContentColor = TextMuted,
                            selectedContentColor = AccentBlue,
                            focusedContentColor = Color.White,
                            focusedSelectedContentColor = Color.White
                        ),
                        modifier = Modifier
                            .then(if (isSelected && topNavFocusRequester != null) Modifier.focusRequester(topNavFocusRequester) else Modifier)
                    ) {
                        Text(
                            text = navItem.label,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
