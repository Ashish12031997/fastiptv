package com.fastiptv.ui.components

import com.fastiptv.domain.model.Category

data class ParsedCategory(
    val category: Category,
    val groupName: String,
    val cleanName: String
)

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
}
