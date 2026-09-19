package com.fastiptv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.fastiptv.domain.model.Category
import com.fastiptv.ui.theme.AccentBlue
import com.fastiptv.ui.theme.DarkSurface
import com.fastiptv.ui.theme.DarkSurfaceElevated
import com.fastiptv.ui.theme.GlassBorder
import com.fastiptv.ui.theme.TextMuted
import com.fastiptv.ui.theme.TextWhite

@Composable
fun CatalogCategorySidebar(
    categories: List<Category>,
    selectedCategory: Category?,
    onSelectCategory: (Category) -> Unit,
    modifier: Modifier = Modifier,
    contentFocusRequester: FocusRequester? = null,
    sidebarFirstItemFocusRequester: FocusRequester? = null,
    topNavFocusRequester: FocusRequester? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("ALL") }
    var isFilterEditing by remember { mutableStateOf(false) }
    var isFilterBoxFocused by remember { mutableStateOf(false) }
    val filterBoxFocusRequester = remember { FocusRequester() }
    val filterTextFieldFocusRequester = remember { FocusRequester() }

    BackHandler(enabled = isFilterEditing) {
        isFilterEditing = false
        try {
            filterBoxFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    LaunchedEffect(isFilterEditing) {
        if (isFilterEditing) {
            try {
                filterTextFieldFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val parsedCategories = remember(categories) {
        categories.map { CategoryGroupHelper.parse(it) }
    }

    val topGroups = remember(parsedCategories) {
        CategoryGroupHelper.extractTopGroups(parsedCategories)
    }

    val filteredCategories = remember(parsedCategories, selectedGroup, searchQuery) {
        parsedCategories.filter { item ->
            val matchesGroup = selectedGroup == "ALL" || item.groupName == selectedGroup
            val matchesQuery = searchQuery.isBlank() ||
                    item.cleanName.contains(searchQuery, ignoreCase = true) ||
                    item.groupName.contains(searchQuery, ignoreCase = true)
            matchesGroup && matchesQuery
        }
    }

    val activeFocusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(Color(0xE60E121C))
            .border(width = 1.dp, color = GlassBorder)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CATEGORIES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue,
                        letterSpacing = 1.2.sp,
                        fontSize = 12.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceElevated)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${filteredCategories.size}",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // In-Sidebar Quick Filter Box (Remote-First: does NOT open keyboard until explicitly clicked/pressed)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFilterBoxFocused) DarkSurfaceElevated else DarkSurface)
                        .border(
                            width = if (isFilterBoxFocused) 2.dp else 1.dp,
                            color = if (isFilterBoxFocused) AccentBlue else GlassBorder,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .focusRequester(filterBoxFocusRequester)
                        .onFocusChanged { isFilterBoxFocused = it.isFocused }
                        .focusProperties {
                            if (sidebarFirstItemFocusRequester != null) {
                                down = sidebarFirstItemFocusRequester
                            }
                            if (topNavFocusRequester != null) {
                                up = topNavFocusRequester
                            }
                        }
                        .clickable {
                            isFilterEditing = true
                        }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (!isFilterEditing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "🔍", fontSize = 11.sp)
                                Text(
                                    text = if (searchQuery.isEmpty()) "Filter categories..." else searchQuery,
                                    color = if (searchQuery.isEmpty()) TextMuted.copy(alpha = 0.6f) else TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = if (searchQuery.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                Text(
                                    text = "✕",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { searchQuery = "" }
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "🔍", fontSize = 11.sp)
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                cursorBrush = SolidColor(AccentBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(filterTextFieldFocusRequester)
                            )
                            if (searchQuery.isNotEmpty()) {
                                Text(
                                    text = "✕",
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable {
                                        searchQuery = ""
                                        isFilterEditing = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Top Group Chips (if meaningful groups exist)
                if (topGroups.size > 2) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(topGroups) { group ->
                            val isGroupSelected = group == selectedGroup
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isGroupSelected) AccentBlue.copy(alpha = 0.25f)
                                        else DarkSurface
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isGroupSelected) AccentBlue else GlassBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedGroup = group }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = group,
                                    color = if (isGroupSelected) AccentBlue else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = if (isGroupSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GlassBorder)
            )

            // Category Vertical List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(filteredCategories, key = { _, item -> item.category.id }) { index, item ->
                    val isSelected = item.category.id == selectedCategory?.id

                    val itemModifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .then(
                            if (index == 0 && sidebarFirstItemFocusRequester != null) {
                                Modifier.focusRequester(sidebarFirstItemFocusRequester)
                            } else Modifier
                        )
                        .then(
                            if (isSelected) Modifier.focusRequester(activeFocusRequester) else Modifier
                        )
                        .focusProperties {
                            if (index == 0) {
                                up = filterBoxFocusRequester
                            }
                            if (contentFocusRequester != null) {
                                right = contentFocusRequester
                            }
                        }
                        .clickable { onSelectCategory(item.category) }

                    Card(
                        onClick = { onSelectCategory(item.category) },
                        modifier = itemModifier,
                        colors = CardDefaults.colors(
                            containerColor = if (isSelected) AccentBlue.copy(alpha = 0.18f) else Color.Transparent,
                            focusedContainerColor = DarkSurfaceElevated
                        ),
                        border = CardDefaults.border(
                            border = Border(
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) AccentBlue.copy(alpha = 0.5f) else Color.Transparent
                                )
                            ),
                            focusedBorder = Border(border = BorderStroke(2.dp, AccentBlue))
                        ),
                        shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Glowing indicator bar if active
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(AccentBlue)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(TextMuted.copy(alpha = 0.3f))
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.cleanName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TextWhite else TextMuted,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.groupName != "GENERAL" && selectedGroup == "ALL") {
                                    Text(
                                        text = item.groupName,
                                        fontSize = 9.sp,
                                        color = AccentBlue.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
