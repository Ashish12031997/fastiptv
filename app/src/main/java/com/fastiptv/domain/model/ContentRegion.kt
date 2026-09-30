package com.fastiptv.domain.model

import java.util.Locale
import java.util.TimeZone

enum class ContentRegion(
    val id: String,
    val displayName: String,
    val flag: String,
    val description: String
) {
    AUTO("AUTO", "Auto-Detect", "🌐", "Automatically detects your region based on TV system locale & timezone"),
    INDIA("INDIA", "India & South Asia", "🇮🇳", "Prioritizes Indian channels, Cricket, Hindi & regional languages"),
    USA_CANADA("USA_CANADA", "USA & Canada", "🇺🇸", "Prioritizes US/Canadian networks, ESPN, HBO & American sports"),
    UK("UK", "United Kingdom & Ireland", "🇬🇧", "Prioritizes Sky, BBC, Premier League football & UK entertainment"),
    ARABIC("ARABIC", "Middle East & Arabic", "🇦🇪", "Prioritizes BeIN Sports, OSN, MBC & Arabic news and drama"),
    LATINO("LATINO", "Latin America & Spain", "🇪🇸", "Prioritizes Spanish & Latino channels, Deportes, Películas"),
    GLOBAL("GLOBAL", "Global / Neutral", "🌍", "General Sports, News & Movies prioritized; zero regional demotions");

    companion object {
        fun fromId(id: String?): ContentRegion {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: AUTO
        }

        /**
         * Detects the best matching regional profile from system Locale and TimeZone.
         */
        fun detectFromSystem(): ContentRegion {
            val country = try {
                Locale.getDefault().country.uppercase()
            } catch (_: Exception) {
                ""
            }

            when (country) {
                "IN", "PK", "BD", "LK", "NP" -> return INDIA
                "US", "CA" -> return USA_CANADA
                "GB", "IE", "UK" -> return UK
                "AE", "SA", "EG", "QA", "KW", "OM", "BH", "JO", "LB", "IQ", "DZ", "MA", "TN", "YE", "LY", "SD" -> return ARABIC
                "ES", "MX", "AR", "CO", "CL", "PE", "VE", "EC", "GT", "CU", "BO", "DO", "HN", "PY", "SV", "NI", "CR", "PA", "UY", "PR" -> return LATINO
            }

            // Fallback: Check TimeZone ID
            val tzId = try {
                TimeZone.getDefault().id.uppercase()
            } catch (_: Exception) {
                ""
            }

            return when {
                tzId.contains("KOLKATA") || tzId.contains("CALCUTTA") || tzId.contains("ASIA/COLOMBO") -> INDIA
                tzId.contains("NEW_YORK") || tzId.contains("CHICAGO") || tzId.contains("LOS_ANGELES") ||
                    tzId.contains("DENVER") || tzId.contains("TORONTO") || tzId.contains("VANCOUVER") ||
                    tzId.contains("AMERICA/") -> USA_CANADA
                tzId.contains("LONDON") || tzId.contains("DUBLIN") || tzId.contains("EUROPE/BELFAST") -> UK
                tzId.contains("DUBAI") || tzId.contains("RIYADH") || tzId.contains("CAIRO") ||
                    tzId.contains("DOHA") || tzId.contains("KUWAIT") || tzId.contains("BAGHDAD") -> ARABIC
                tzId.contains("MADRID") || tzId.contains("MEXICO") || tzId.contains("BOGOTA") ||
                    tzId.contains("BUENOS_AIRES") || tzId.contains("SANTIAGO") || tzId.contains("LIMA") -> LATINO
                else -> GLOBAL
            }
        }
    }
}

/**
 * Defines priority keyword tiers and demoted low-priority keywords for each region.
 */
data class RegionPriorityProfile(
    val highPriorityTiers: List<List<String>>,
    val lowPriorityKeywords: List<String>
)

object RegionProfileCatalog {

    fun getProfile(region: ContentRegion): RegionPriorityProfile {
        val effectiveRegion = if (region == ContentRegion.AUTO) {
            ContentRegion.detectFromSystem()
        } else {
            region
        }

        return when (effectiveRegion) {
            ContentRegion.INDIA -> RegionPriorityProfile(
                highPriorityTiers = listOf(
                    listOf("INDIA", "INDIAN", "HINDI", "DESI"),
                    listOf("SPORT", "SPORTS"),
                    listOf("CRICKET"),
                    listOf("NEWS", "SAMACHAAR"),
                    listOf("KIDS", "CHILDREN", "CARTOON"),
                    listOf("BHAKTI", "DEVOTIONAL", "RELIGIOUS"),
                    listOf("GUJARATI"),
                    listOf("PUNJABI"),
                    listOf("TAMIL", "TELUGU", "BENGALI", "MALAYALAM", "KANNADA", "MARATHI")
                ),
                lowPriorityKeywords = listOf(
                    "ARABIC", "ARAB", "MIDDLE EAST", "AUSTRALIA", "AUSTRALIAN",
                    "AFRICA", "AFRICAN", "LATINO", "LATIN", "TURKISH", "TURK",
                    "FILIPINO", "PINOY", "PERSIAN", "IRAN", "KOREAN", "JAPANESE",
                    "CHINESE", "AFGHAN"
                )
            )

            ContentRegion.USA_CANADA -> RegionPriorityProfile(
                highPriorityTiers = listOf(
                    listOf("USA", "US", "UNITED STATES", "CAN", "CANADA"),
                    listOf("SPORT", "SPORTS", "ESPN", "NFL", "NBA", "MLB", "NHL", "FOX SPORTS"),
                    listOf("NEWS", "CNN", "FOX NEWS", "MSNBC", "ABC NEWS", "NBC NEWS", "CBS NEWS"),
                    listOf("MOVIES", "CINEMA", "HBO", "PREMIUM", "SHOWTIME", "STARZ"),
                    listOf("KIDS", "CHILDREN", "CARTOON", "DISNEY", "NICKELODEON"),
                    listOf("DOCUMENTARY", "DISCOVERY", "HISTORY", "NAT GEO")
                ),
                lowPriorityKeywords = listOf(
                    "ARABIC", "AFRICA", "TURKISH", "FILIPINO", "PERSIAN", "IRAN",
                    "KOREAN", "JAPANESE", "CHINESE", "RUSSIAN", "POLISH"
                )
            )

            ContentRegion.UK -> RegionPriorityProfile(
                highPriorityTiers = listOf(
                    listOf("UK", "UNITED KINGDOM", "ENGLAND", "BRITISH", "IRELAND"),
                    listOf("SKY", "BBC", "ITV", "CHANNEL 4", "BT SPORT", "TNT SPORTS"),
                    listOf("SPORT", "SPORTS", "FOOTBALL", "PREMIER LEAGUE", "EPL", "RACING"),
                    listOf("NEWS", "SKY NEWS", "BBC NEWS"),
                    listOf("MOVIES", "CINEMA", "SKY CINEMA"),
                    listOf("KIDS", "CBBC", "CARTOON"),
                    listOf("DOCUMENTARY")
                ),
                lowPriorityKeywords = listOf(
                    "ARABIC", "AFRICA", "TURKISH", "FILIPINO", "PERSIAN", "KOREAN",
                    "JAPANESE", "CHINESE", "RUSSIAN"
                )
            )

            ContentRegion.ARABIC -> RegionPriorityProfile(
                highPriorityTiers = listOf(
                    listOf("ARABIC", "ARAB", "NILESAT", "MENA"),
                    listOf("BEIN", "OSN", "MBC", "ROTANA", "AL JAZEERA", "AL ARABIYA"),
                    listOf("SPORT", "SPORTS", "KOURA"),
                    listOf("NEWS", "AKHBAR"),
                    listOf("ISLAMIC", "QURAN", "RELIGIOUS"),
                    listOf("KIDS", "SPACETOON"),
                    listOf("MOVIES", "CINEMA", "DRAMA")
                ),
                lowPriorityKeywords = listOf(
                    "AUSTRALIA", "AFRICA", "LATINO", "FILIPINO", "KOREAN", "JAPANESE"
                )
            )

            ContentRegion.LATINO -> RegionPriorityProfile(
                highPriorityTiers = listOf(
                    listOf("LATINO", "LATIN", "SPAIN", "ESPAÑA", "MEXICO", "COLOMBIA", "ARGENTINA"),
                    listOf("DEPORTES", "SPORT", "SPORTS", "FUTBOL", "LALIGA", "ESPN DEPORTES"),
                    listOf("NOTICIAS", "NEWS", "TELEVISA", "UNIVISION", "TELEMUNDO"),
                    listOf("PELICULAS", "CINEMA", "MOVIES", "CINE"),
                    listOf("INFANTIL", "KIDS", "DIBUJOS"),
                    listOf("NOVELAS", "ENTRETENIMIENTO")
                ),
                lowPriorityKeywords = listOf(
                    "ARABIC", "AFRICA", "TURKISH", "FILIPINO", "PERSIAN", "KOREAN",
                    "JAPANESE", "CHINESE", "RUSSIAN"
                )
            )

            ContentRegion.GLOBAL, ContentRegion.AUTO -> RegionPriorityProfile(
                highPriorityTiers = listOf(
                    listOf("SPORT", "SPORTS", "FOOTBALL", "CRICKET", "BASKETBALL"),
                    listOf("NEWS"),
                    listOf("MOVIES", "CINEMA"),
                    listOf("KIDS", "ANIMATION", "CARTOON"),
                    listOf("DOCUMENTARY"),
                    listOf("ENTERTAINMENT")
                ),
                lowPriorityKeywords = emptyList() // No country is demoted in Global mode!
            )
        }
    }
}
