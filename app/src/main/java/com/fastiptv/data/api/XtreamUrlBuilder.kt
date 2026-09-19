package com.fastiptv.data.api

import com.fastiptv.domain.model.ServerConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XtreamUrlBuilder @Inject constructor() {

    fun buildLiveStreamUrl(config: ServerConfig, streamId: Int, format: String = "m3u8"): String {
        return if (format.lowercase() == "m3u8") {
            "${config.baseUrl}/live/${config.username}/${config.password}/$streamId.m3u8"
        } else {
            "${config.baseUrl}/live/${config.username}/${config.password}/$streamId.ts"
        }
    }

    fun buildVodUrl(config: ServerConfig, streamId: Int, containerExtension: String = "mp4"): String {
        val ext = containerExtension.trim().removePrefix(".")
        return "${config.baseUrl}/movie/${config.username}/${config.password}/$streamId.$ext"
    }

    fun buildSeriesEpisodeUrl(config: ServerConfig, episodeId: Int, containerExtension: String = "mkv"): String {
        val ext = containerExtension.trim().removePrefix(".")
        return "${config.baseUrl}/series/${config.username}/${config.password}/$episodeId.$ext"
    }
}
