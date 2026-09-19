package com.fastiptv.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fastiptv.data.db.entity.ChannelEntity
import com.fastiptv.domain.model.Channel
import kotlinx.coroutines.flow.Flow

data class ChannelWithFavorite(
    @ColumnInfo(name = "stream_id") val streamId: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "icon_url") val iconUrl: String?,
    @ColumnInfo(name = "category_id") val categoryId: String?,
    @ColumnInfo(name = "epg_channel_id") val epgChannelId: String?,
    @ColumnInfo(name = "stream_type") val streamType: String?,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean
) {
    fun toDomain(): Channel = Channel(
        id = streamId,
        name = name,
        logoUrl = iconUrl,
        categoryId = categoryId,
        epgChannelId = epgChannelId,
        isFavorite = isFavorite
    )
}

@Dao
interface ChannelDao {

    @Query("""
        SELECT c.*, (f.stream_id IS NOT NULL) AS is_favorite 
        FROM channels c
        LEFT JOIN favorites f ON c.stream_id = f.stream_id
        WHERE c.category_id = :categoryId
        ORDER BY c.stream_id ASC
    """)
    fun observeChannelsByCategory(categoryId: String): Flow<List<ChannelWithFavorite>>

    @Query("""
        SELECT c.*, 1 AS is_favorite
        FROM channels c
        INNER JOIN favorites f ON c.stream_id = f.stream_id
        ORDER BY f.added_at DESC
    """)
    fun observeFavorites(): Flow<List<ChannelWithFavorite>>

    @Query("""
        SELECT c.*, (f.stream_id IS NOT NULL) AS is_favorite
        FROM channels c
        LEFT JOIN favorites f ON c.stream_id = f.stream_id
        LEFT JOIN categories cat ON c.category_id = cat.category_id
        WHERE c.name LIKE '%' || :query || '%'
           OR cat.name LIKE '%' || :query || '%'
           OR (:fallback != '' AND (c.name LIKE '%' || :fallback || '%' OR cat.name LIKE '%' || :fallback || '%'))
        ORDER BY 
           CASE 
             WHEN c.name LIKE :query || '%' THEN 1
             WHEN c.name LIKE '%' || :query || '%' THEN 2
             WHEN :fallback != '' AND c.name LIKE '%' || :fallback || '%' THEN 3
             ELSE 4
           END,
           c.name ASC
        LIMIT 100
    """)
    fun searchChannels(query: String, fallback: String): Flow<List<ChannelWithFavorite>>

    @Upsert
    suspend fun upsertAll(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE category_id = :categoryId")
    suspend fun deleteByCategory(categoryId: String)

    @Query("SELECT COUNT(*) FROM channels WHERE category_id = :categoryId")
    suspend fun countByCategory(categoryId: String): Int

    @Query("""
        SELECT c.*, (f.stream_id IS NOT NULL) AS is_favorite 
        FROM channels c
        LEFT JOIN favorites f ON c.stream_id = f.stream_id
        WHERE c.stream_id = :streamId
        LIMIT 1
    """)
    suspend fun getChannelById(streamId: Int): ChannelWithFavorite?
}
