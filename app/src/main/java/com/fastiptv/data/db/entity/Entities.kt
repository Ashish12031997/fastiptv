package com.fastiptv.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "category_id")
    val categoryId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "type")
    val type: String
)

@Entity(
    tableName = "channels",
    indices = [Index(value = ["category_id"])]
)
data class ChannelEntity(
    @PrimaryKey
    @ColumnInfo(name = "stream_id")
    val streamId: Int,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "icon_url")
    val iconUrl: String?,
    @ColumnInfo(name = "category_id")
    val categoryId: String?,
    @ColumnInfo(name = "epg_channel_id")
    val epgChannelId: String?,
    @ColumnInfo(name = "stream_type")
    val streamType: String? = "live"
)

@Entity(tableName = "channels_fts")
@Fts4(contentEntity = ChannelEntity::class)
data class ChannelFtsEntity(
    @ColumnInfo(name = "name")
    val name: String
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "stream_id")
    val streamId: Int,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recents")
data class RecentEntity(
    @PrimaryKey
    @ColumnInfo(name = "stream_id")
    val streamId: Int,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "icon_url")
    val iconUrl: String?,
    @ColumnInfo(name = "last_watched")
    val lastWatched: Long,
    @ColumnInfo(name = "watch_position_ms")
    val watchPositionMs: Long = 0L,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0L
)

@Entity(
    tableName = "vod_streams",
    indices = [Index(value = ["category_id"])]
)
data class VodEntity(
    @PrimaryKey
    @ColumnInfo(name = "stream_id")
    val streamId: Int,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "poster_url")
    val posterUrl: String?,
    @ColumnInfo(name = "category_id")
    val categoryId: String?,
    @ColumnInfo(name = "container_ext")
    val containerExt: String,
    @ColumnInfo(name = "rating")
    val rating: String?
)

@Entity(tableName = "vod_fts")
@Fts4(contentEntity = VodEntity::class)
data class VodFtsEntity(
    @ColumnInfo(name = "name")
    val name: String
)

@Entity(
    tableName = "series",
    indices = [Index(value = ["category_id"])]
)
data class SeriesEntity(
    @PrimaryKey
    @ColumnInfo(name = "series_id")
    val seriesId: Int,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "cover_url")
    val coverUrl: String?,
    @ColumnInfo(name = "category_id")
    val categoryId: String?,
    @ColumnInfo(name = "rating")
    val rating: String?,
    @ColumnInfo(name = "genre")
    val genre: String?
)

@Entity(
    tableName = "epg_programs",
    indices = [
        Index(value = ["channel_id"]),
        Index(value = ["start_time"])
    ]
)
data class EpgProgramEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "channel_id")
    val channelId: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "start_time")
    val startTime: Long,
    @ColumnInfo(name = "end_time")
    val endTime: Long
)
