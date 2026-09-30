package com.fastiptv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.lazy.rememberLazyListState
import com.fastiptv.domain.model.Category
import com.fastiptv.domain.model.ContentRegion
import com.fastiptv.ui.theme.AccentBlue
import com.fastiptv.ui.theme.DarkSurface
import com.fastiptv.ui.theme.DarkSurfaceElevated
import com.fastiptv.ui.theme.GlassBorder
import com.fastiptv.ui.theme.TextMuted
import com.fastiptv.ui.theme.TextWhite

/**
 * Sealed class representing items in the grouped sidebar list.
 */
private sealed class SidebarItem {
    data class GroupHeader(
        val group: CategoryGroup,
        val isExpanded: Boolean
    ) : SidebarItem()

    data class CategoryItem(
        val parsed: ParsedCategory,
        val groupName: String
    ) : SidebarItem()

    data class Separator(val id: String) : SidebarItem()
}

@Composable
fun CatalogCategorySidebar(
    categories: List<Category>,
    selectedCategory: Category?,
    onSelectCategory: (Category) -> Unit,
    modifier: Modifier = Modifier,
    contentFocusRequester: FocusRequester? = null,
    sidebarFirstItemFocusRequester: FocusRequester? = null,
    topNavFocusRequester: FocusRequester? = null,
    region: ContentRegion = ContentRegion.AUTO,
    pinnedGroups: Set<String> = emptySet(),
    onTogglePinGroup: ((String) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var isFilterEditing by remember { mutableStateOf(false) }
    var isFilterBoxFocused by remember { mutableStateOf(false) }
    val filterBoxFocusRequester = remember { FocusRequester() }
    val filterTextFieldFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

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

    // Build grouped structure
    val groupedCategories = remember(categories, region, pinnedGroups) {
        CategoryGroupHelper.buildGroups(categories, region, pinnedGroups)
    }

    // Track which groups are expanded
    var expandedGroups by remember(groupedCategories) {
        // Auto-expand the group containing the selected category, or the first HIGH priority group
        val selectedGroupName = if (selectedCategory != null) {
            groupedCategories.find { group ->
                group.categories.any { it.category.id == selectedCategory.id }
            }?.groupName
        } else null

        val initialExpanded = mutableSetOf<String>()
        if (selectedGroupName != null) {
            initialExpanded.add(selectedGroupName)
        } else {
            // Auto-expand the first HIGH priority group
            groupedCategories.firstOrNull { it.priority == GroupPriority.HIGH }?.let {
                initialExpanded.add(it.groupName)
            }
        }
        mutableStateOf<Set<String>>(initialExpanded)
    }

    // When selected category changes, auto-expand its group
    LaunchedEffect(selectedCategory?.id) {
        if (selectedCategory != null) {
            val containingGroup = groupedCategories.find { group ->
                group.categories.any { it.category.id == selectedCategory.id }
            }
            if (containingGroup != null && containingGroup.groupName !in expandedGroups) {
                expandedGroups = expandedGroups + containingGroup.groupName
            }
        }
    }

    // Build flat list for LazyColumn from grouped data, applying search filter
    val sidebarItems = remember(groupedCategories, expandedGroups, searchQuery) {
        val isSearching = searchQuery.isNotBlank()
        val items = mutableListOf<SidebarItem>()
        var lastPriority: GroupPriority? = null

        for (group in groupedCategories) {
            val matchingCategories = if (isSearching) {
                group.categories.filter { item ->
                    item.cleanName.contains(searchQuery, ignoreCase = true) ||
                            item.groupName.contains(searchQuery, ignoreCase = true) ||
                            item.category.name.contains(searchQuery, ignoreCase = true)
                }
            } else {
                group.categories
            }

            if (matchingCategories.isEmpty()) continue

            // Add separator between priority sections
            if (lastPriority != null && lastPriority != group.priority &&
                (lastPriority == GroupPriority.HIGH || group.priority == GroupPriority.LOW)) {
                items.add(SidebarItem.Separator("sep_${group.priority.name}"))
            }
            lastPriority = group.priority

            // Groups with only 1 category → show directly (no nesting)
            if (matchingCategories.size == 1 && !isSearching) {
                items.add(SidebarItem.CategoryItem(
                    parsed = matchingCategories.first(),
                    groupName = group.groupName
                ))
                continue
            }

            val isExpanded = isSearching || group.groupName in expandedGroups
            items.add(SidebarItem.GroupHeader(
                group = group.copy(categories = matchingCategories),
                isExpanded = isExpanded
            ))

            if (isExpanded) {
                for (cat in matchingCategories) {
                    items.add(SidebarItem.CategoryItem(
                        parsed = cat,
                        groupName = group.groupName
                    ))
                }
            }
        }
        items.toList()
    }

    // Total visible categories count
    val totalCategoryCount = remember(sidebarItems) {
        sidebarItems.count { it is SidebarItem.CategoryItem }
    }

    val activeFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    var debounceSelectJob by remember { mutableStateOf<Job?>(null) }

    // Find the index of the selected category in the flat list
    val selectedItemIndex = remember(sidebarItems, selectedCategory) {
        sidebarItems.indexOfFirst {
            it is SidebarItem.CategoryItem && it.parsed.category.id == selectedCategory?.id
        }
    }

    LaunchedEffect(selectedItemIndex) {
        if (selectedItemIndex >= 0) {
            try {
                listState.scrollToItem((selectedItemIndex - 2).coerceAtLeast(0))
            } catch (_: Exception) {}
        }
    }

    // BackHandler: if inside an expanded group, collapse it on back
    val focusedGroupName = remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = focusedGroupName.value != null && focusedGroupName.value in expandedGroups) {
        focusedGroupName.value?.let { groupName ->
            expandedGroups = expandedGroups - groupName
            focusedGroupName.value = null
        }
    }

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
                            text = "${groupedCategories.size} groups",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // In-Sidebar Quick Filter Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFilterBoxFocused) Color(0xFF1E293B) else DarkSurface)
                        .border(
                            width = if (isFilterBoxFocused) 2.5.dp else 1.dp,
                            color = if (isFilterBoxFocused) Color(0xFF38BDF8) else GlassBorder,
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
                            if (contentFocusRequester != null) {
                                right = contentFocusRequester
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
                                    text = if (searchQuery.isEmpty()) "Search categories..." else searchQuery,
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
            }

            // Divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GlassBorder)
            )

            // Grouped Category List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                itemsIndexed(
                    sidebarItems,
                    key = { _, item ->
                        when (item) {
                            is SidebarItem.GroupHeader -> "group_${item.group.groupName}"
                            is SidebarItem.CategoryItem -> "cat_${item.parsed.category.id}"
                            is SidebarItem.Separator -> item.id
                        }
                    }
                ) { index, item ->
                    when (item) {
                        is SidebarItem.Separator -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .height(1.dp)
                                    .background(GlassBorder.copy(alpha = 0.4f))
                            )
                        }

                        is SidebarItem.GroupHeader -> {
                            GroupHeaderRow(
                                group = item.group,
                                isExpanded = item.isExpanded,
                                onToggle = {
                                    expandedGroups = if (item.group.groupName in expandedGroups) {
                                        expandedGroups - item.group.groupName
                                    } else {
                                        expandedGroups + item.group.groupName
                                    }
                                },
                                onTogglePin = { onTogglePinGroup?.invoke(item.group.groupName) },
                                modifier = Modifier.then(
                                    if (index == 0 && sidebarFirstItemFocusRequester != null) {
                                        Modifier.focusRequester(sidebarFirstItemFocusRequester)
                                    } else Modifier
                                ),
                                upFocusRequester = if (index == 0) filterBoxFocusRequester else null,
                                rightFocusRequester = contentFocusRequester
                            )
                        }

                        is SidebarItem.CategoryItem -> {
                            val isSelected = item.parsed.category.id == selectedCategory?.id
                            var isItemFocused by remember { mutableStateOf(false) }

                            // Determine if this item should get the sidebar focus requester
                            val isFirstFocusable = index == 0 ||
                                    (sidebarItems.take(index).none { it is SidebarItem.CategoryItem || it is SidebarItem.GroupHeader })
                            val isSelectedItem = selectedItemIndex == index

                            val shouldAttachSidebarFocus = if (selectedItemIndex >= 0) {
                                isSelectedItem
                            } else {
                                isFirstFocusable
                            }

                            val isInsideGroup = item.groupName != "GENERAL" &&
                                    sidebarItems.any { it is SidebarItem.GroupHeader && (it as SidebarItem.GroupHeader).group.groupName == item.groupName }

                            val itemModifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .then(
                                    if (shouldAttachSidebarFocus && sidebarFirstItemFocusRequester != null &&
                                        sidebarItems.getOrNull(0) !is SidebarItem.GroupHeader) {
                                        Modifier.focusRequester(sidebarFirstItemFocusRequester)
                                    } else Modifier
                                )
                                .focusProperties {
                                    if (index == 0) {
                                        up = filterBoxFocusRequester
                                    }
                                    if (contentFocusRequester != null) {
                                        right = contentFocusRequester
                                    }
                                }
                                .onFocusChanged { state ->
                                    isItemFocused = state.isFocused
                                    if (state.isFocused) {
                                        focusedGroupName.value = item.groupName
                                        if (item.parsed.category.id != selectedCategory?.id) {
                                            debounceSelectJob?.cancel()
                                            debounceSelectJob = coroutineScope.launch {
                                                delay(150)
                                                onSelectCategory(item.parsed.category)
                                            }
                                        }
                                    }
                                }

                            Card(
                                onClick = {
                                    debounceSelectJob?.cancel()
                                    onSelectCategory(item.parsed.category)
                                    try {
                                        contentFocusRequester?.requestFocus()
                                    } catch (_: Exception) {}
                                },
                                modifier = itemModifier,
                                scale = CardDefaults.scale(focusedScale = 1.04f),
                                colors = CardDefaults.colors(
                                    containerColor = if (isSelected) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color.Transparent,
                                    focusedContainerColor = Color(0xFF1E3A8A)
                                ),
                                border = CardDefaults.border(
                                    border = Border(
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color.Transparent
                                        )
                                    ),
                                    focusedBorder = Border(border = BorderStroke(3.dp, Color(0xFF38BDF8)))
                                ),
                                shape = CardDefaults.shape(shape = RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            start = if (isInsideGroup) 20.dp else 10.dp,
                                            end = 10.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Indicator
                                    if (isItemFocused) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(24.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFF38BDF8))
                                        )
                                    } else if (isSelected) {
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
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(TextMuted.copy(alpha = 0.3f))
                                        )
                                    }

                                    Text(
                                        text = item.parsed.cleanName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = when {
                                            isItemFocused -> FontWeight.Black
                                            isSelected -> FontWeight.Bold
                                            else -> FontWeight.Medium
                                        },
                                        color = when {
                                            isItemFocused -> Color.White
                                            isSelected -> TextWhite
                                            else -> TextMuted
                                        },
                                        fontSize = if (isItemFocused) 13.sp else 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
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

/**
 * Group header row — shows group name, category count, and expand/collapse arrow.
 */
@Composable
private fun GroupHeaderRow(
    group: CategoryGroup,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onTogglePin: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    upFocusRequester: FocusRequester? = null,
    rightFocusRequester: FocusRequester? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    var keyDownTime by remember { mutableStateOf(0L) }

    val priorityColor = when {
        group.isPinned -> Color(0xFFFDE047)
        group.priority == GroupPriority.HIGH -> Color(0xFF38BDF8) // bright blue
        group.priority == GroupPriority.NORMAL -> TextMuted
        group.priority == GroupPriority.LOW -> TextMuted.copy(alpha = 0.5f)
        else -> TextMuted
    }

    Card(
        onClick = { onToggle() },
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusProperties {
                if (upFocusRequester != null) up = upFocusRequester
                if (rightFocusRequester != null) right = rightFocusRequester
            }
            .onKeyEvent { keyEvent ->
                val nativeEvent = keyEvent.nativeKeyEvent
                val code = nativeEvent.keyCode
                if (code == AndroidKeyEvent.KEYCODE_DPAD_CENTER || code == AndroidKeyEvent.KEYCODE_ENTER || code == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER) {
                    if (nativeEvent.action == AndroidKeyEvent.ACTION_DOWN) {
                        if (keyDownTime == 0L) {
                            keyDownTime = System.currentTimeMillis()
                        }
                        false
                    } else if (nativeEvent.action == AndroidKeyEvent.ACTION_UP) {
                        val duration = System.currentTimeMillis() - keyDownTime
                        keyDownTime = 0L
                        if (duration > 500L && onTogglePin != null) {
                            onTogglePin()
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                } else if (nativeEvent.action == AndroidKeyEvent.ACTION_UP && (
                        code == AndroidKeyEvent.KEYCODE_PROG_YELLOW ||
                        code == AndroidKeyEvent.KEYCODE_BOOKMARK ||
                        code == AndroidKeyEvent.KEYCODE_F
                    )) {
                    if (onTogglePin != null) {
                        onTogglePin()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            },
        scale = CardDefaults.scale(focusedScale = 1.03f),
        colors = CardDefaults.colors(
            containerColor = when {
                group.isPinned -> Color(0xFF2E2408).copy(alpha = 0.65f)
                isExpanded -> Color(0xFF1E293B).copy(alpha = 0.6f)
                else -> Color(0xFF0F172A).copy(alpha = 0.5f)
            },
            focusedContainerColor = if (group.isPinned) Color(0xFF854D0E).copy(alpha = 0.85f) else Color(0xFF1E3A8A).copy(alpha = 0.8f)
        ),
        border = CardDefaults.border(
            border = Border(
                border = BorderStroke(
                    1.dp,
                    when {
                        group.isPinned -> Color(0xFFFDE047).copy(alpha = 0.5f)
                        isExpanded -> Color(0xFF38BDF8).copy(alpha = 0.3f)
                        else -> GlassBorder.copy(alpha = 0.3f)
                    }
                )
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, if (group.isPinned) Color(0xFFFDE047) else Color(0xFF60A5FA))
            )
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
            // Priority or Pinned indicator
            if (group.isPinned) {
                Text(
                    text = "⭐",
                    fontSize = 11.sp
                )
            } else if (group.priority == GroupPriority.HIGH) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF38BDF8))
                )
            }

            // Expand/Collapse arrow
            Text(
                text = if (isExpanded) "▼" else "▶",
                fontSize = 10.sp,
                color = if (isFocused) Color.White else priorityColor
            )

            // Group name
            Text(
                text = group.groupName,
                fontSize = if (isFocused) 13.sp else 12.sp,
                fontWeight = if (isFocused || isExpanded || group.isPinned) FontWeight.Bold else FontWeight.SemiBold,
                color = when {
                    isFocused -> Color.White
                    group.isPinned -> Color(0xFFFEF08A)
                    isExpanded -> TextWhite
                    group.priority == GroupPriority.HIGH -> Color(0xFFBFDBFE)
                    else -> TextMuted
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Category count badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isFocused) {
                            if (group.isPinned) Color(0xFF713F12) else Color(0xFF1E40AF)
                        } else DarkSurfaceElevated.copy(alpha = 0.6f)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${group.categories.size}",
                    color = when {
                        isFocused -> Color.White
                        group.isPinned -> Color(0xFFFDE047)
                        else -> TextMuted.copy(alpha = 0.7f)
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
