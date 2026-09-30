package com.fastiptv.ui.components

import com.fastiptv.domain.model.Category
import com.fastiptv.domain.model.ContentRegion
import com.fastiptv.domain.model.RegionProfileCatalog

data class ParsedCategory(
    val category: Category,
    val groupName: String,
    val cleanName: String
)

data class CategoryGroup(
    val groupName: String,
    val categories: List<ParsedCategory>,
    val priority: GroupPriority,
    val priorityRank: Int = Int.MAX_VALUE,
    val isPinned: Boolean = false
)

enum class GroupPriority { HIGH, NORMAL, LOW }

object CategoryGroupHelper {

    private val DELIMITERS = listOf("|", "—", "-", "/", "•", ":")

    fun parse(category: Category): ParsedCategory {
        val raw = category.name.trim()
        for (delim in DELIMITERS) {
            if (raw.contains(delim)) {
                val parts = raw.split(delim, limit = 2)
                val prefix = parts[0].trim()
                val suffix = parts[1].trim()
                if (prefix.length in 2..24 && suffix.isNotEmpty()) {
                    return ParsedCategory(
                        category = category,
                        groupName = prefix.uppercase(),
                        cleanName = suffix
                    )
                }
            }
        }
        return ParsedCategory(
            category = category,
            groupName = "GENERAL",
            cleanName = raw
        )
    }

    fun extractTopGroups(parsedList: List<ParsedCategory>): List<String> {
        val groupCounts = parsedList.groupingBy { it.groupName }.eachCount()
        val validGroups = groupCounts.filter { (grp, count) ->
            grp != "GENERAL" && count >= 2
        }.keys.sorted()

        return listOf("ALL") + validGroups
    }

    /**
     * Build a grouped, priority-sorted list of CategoryGroups.
     *
     * Priority order:
     *   1. Pinned groups (marked with ⭐, highest priority)
     *   2. HIGH priority groups (ordered by region profile tier rank, then alphabetical)
     *   3. NORMAL priority groups (alphabetical)
     *   4. LOW priority groups (alphabetical)
     *   5. GENERAL group (always last)
     */
    fun buildGroups(
        categories: List<Category>,
        region: ContentRegion = ContentRegion.AUTO,
        pinnedGroups: Set<String> = emptySet()
    ): List<CategoryGroup> {
        val parsed = categories.map { parse(it) }
        val pinnedUpper = pinnedGroups.map { it.trim().uppercase() }.toSet()
        val profile = RegionProfileCatalog.getProfile(region)

        return parsed
            .groupBy { it.groupName }
            .map { (name, items) ->
                val upperName = name.uppercase()
                val isPinned = upperName in pinnedUpper
                val (priority, rank) = if (isPinned) {
                    GroupPriority.HIGH to -1
                } else {
                    classifyGroup(upperName, profile.highPriorityTiers, profile.lowPriorityKeywords)
                }

                CategoryGroup(
                    groupName = name,
                    categories = items,
                    priority = priority,
                    priorityRank = rank,
                    isPinned = isPinned
                )
            }
            .sortedWith(
                compareBy<CategoryGroup> { groupSortBucket(it) }
                    .thenBy { it.priorityRank }
                    .thenBy { it.groupName }
            )
    }

    /**
     * Classify a group name into HIGH/NORMAL/LOW priority and a sub-rank
     * within the HIGH tier using the active regional profile.
     */
    private fun classifyGroup(
        upper: String,
        highPriorityTiers: List<List<String>>,
        lowPriorityKeywords: List<String>
    ): Pair<GroupPriority, Int> {
        // GENERAL always goes last
        if (upper == "GENERAL") {
            return GroupPriority.LOW to Int.MAX_VALUE
        }

        // Check HIGH priority tiers in order
        for ((tierIndex, keywords) in highPriorityTiers.withIndex()) {
            if (keywords.any { upper.contains(it) }) {
                return GroupPriority.HIGH to tierIndex
            }
        }

        // Check LOW priority
        if (lowPriorityKeywords.any { upper.contains(it) }) {
            return GroupPriority.LOW to Int.MAX_VALUE
        }

        return GroupPriority.NORMAL to Int.MAX_VALUE
    }

    /**
     * Assign a top-level sort bucket:
     * -1 -> Pinned groups (absolute top)
     *  0 -> HIGH priority
     *  1 -> NORMAL priority
     *  2 -> LOW priority
     *  3 -> GENERAL (dead last)
     */
    private fun groupSortBucket(group: CategoryGroup): Int {
        return when {
            group.isPinned -> -1
            group.priority == GroupPriority.HIGH -> 0
            group.priority == GroupPriority.NORMAL -> 1
            group.groupName == "GENERAL" -> 3
            else -> 2 // LOW
        }
    }
}
