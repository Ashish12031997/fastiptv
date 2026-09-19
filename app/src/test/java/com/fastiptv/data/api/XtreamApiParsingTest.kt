package com.fastiptv.data.api

import com.fastiptv.data.model.AuthResponseDto
import com.fastiptv.data.model.CategoryDto
import com.fastiptv.data.model.LiveStreamDto
import com.fastiptv.data.model.SeriesDto
import com.fastiptv.data.model.ShortEpgDto
import com.fastiptv.data.model.VodStreamDto
import com.fastiptv.domain.model.ServerConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class XtreamApiParsingTest {

    private lateinit var json: Json
    private lateinit var urlBuilder: XtreamUrlBuilder

    @Before
    fun setup() {
        json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
        urlBuilder = XtreamUrlBuilder()
    }

    @Test
    fun testAuthResponseDeserialization() {
        val jsonString = """
        {
          "user_info": {
            "username": "fast_user",
            "password": "fast_pass",
            "auth": 1,
            "status": "Active",
            "exp_date": "1735689600",
            "max_connections": "1"
          },
          "server_info": {
            "url": "live.fastiptv.com",
            "port": "8080",
            "server_protocol": "http"
          }
        }
        """.trimIndent()

        val parsed = json.decodeFromString<AuthResponseDto>(jsonString)
        assertNotNull(parsed.userInfo)
        assertEquals("fast_user", parsed.userInfo?.username)
        assertEquals(1, parsed.userInfo?.auth)
        assertEquals("Active", parsed.userInfo?.status)
        assertEquals("live.fastiptv.com", parsed.serverInfo?.url)
    }

    @Test
    fun testLiveStreamsDeserialization() {
        val jsonString = """
        [
          {
            "num": 1,
            "name": "USA: CNN HD",
            "stream_type": "live",
            "stream_id": 10542,
            "stream_icon": "http://img.com/cnn.png",
            "epg_channel_id": "cnn.us",
            "category_id": "1"
          }
        ]
        """.trimIndent()

        val parsed = json.decodeFromString<List<LiveStreamDto>>(jsonString)
        assertEquals(1, parsed.size)
        assertEquals(10542, parsed[0].streamId)
        assertEquals("USA: CNN HD", parsed[0].name)
        assertEquals("1", parsed[0].categoryId)
    }

    @Test
    fun testVodStreamsDeserialization() {
        val jsonString = """
        [
          {
            "num": 10,
            "name": "Inception (2010)",
            "stream_type": "movie",
            "stream_id": 45892,
            "rating": "8.8",
            "rating_5based": 4.4,
            "category_id": "5",
            "container_extension": "mkv"
          }
        ]
        """.trimIndent()

        val parsed = json.decodeFromString<List<VodStreamDto>>(jsonString)
        assertEquals(1, parsed.size)
        assertEquals("Inception (2010)", parsed[0].name)
        assertEquals("mkv", parsed[0].containerExtension)
    }

    @Test
    fun testShortEpgDeserialization() {
        val jsonString = """
        {
          "epg_listings": [
            {
              "id": "123",
              "title": "Evening News",
              "start": "2026-09-14 18:00:00",
              "end": "2026-09-14 19:00:00",
              "now_playing": 1
            }
          ]
        }
        """.trimIndent()

        val parsed = json.decodeFromString<ShortEpgDto>(jsonString)
        assertEquals(1, parsed.epgListings?.size)
        assertEquals("Evening News", parsed.epgListings?.get(0)?.title)
        assertEquals(1, parsed.epgListings?.get(0)?.nowPlaying)
    }

    @Test
    fun testUrlBuilder() {
        val config = ServerConfig("iptv.net", 8080, "demo", "secret", "http")

        val m3u8Url = urlBuilder.buildLiveStreamUrl(config, 1001, "m3u8")
        assertEquals("http://iptv.net:8080/live/demo/secret/1001.m3u8", m3u8Url)

        val tsUrl = urlBuilder.buildLiveStreamUrl(config, 1001, "ts")
        assertEquals("http://iptv.net:8080/live/demo/secret/1001.ts", tsUrl)

        val vodUrl = urlBuilder.buildVodUrl(config, 2002, "mp4")
        assertEquals("http://iptv.net:8080/movie/demo/secret/2002.mp4", vodUrl)

        val seriesUrl = urlBuilder.buildSeriesEpisodeUrl(config, 3003, ".mkv")
        assertEquals("http://iptv.net:8080/series/demo/secret/3003.mkv", seriesUrl)
    }

    @Test
    fun testSeriesInfoParsing() {
        val jsonString = """
        {
          "seasons": [
            {
              "id": 1,
              "name": "Season 1",
              "season_number": 1,
              "episode_count": 2
            }
          ],
          "episodes": {
            "1": [
              {
                "id": "101",
                "episode_num": 1,
                "title": "Pilot",
                "container_extension": "mp4"
              }
            ]
          }
        }
        """.trimIndent()
        val parsed = json.decodeFromString<com.fastiptv.data.model.SeriesInfoDto>(jsonString)
        assertNotNull(parsed)
        assertEquals(1, parsed.seasons?.size)
        assertEquals(1, parsed.episodes?.size)
    }
}


