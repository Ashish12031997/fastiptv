package com.fastiptv.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fastiptv.data.db.entity.VodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VodDao {
    @Query("SELECT * FROM vod_streams WHERE category_id = :categoryId ORDER BY stream_id DESC")
    fun observeMoviesByCategory(categoryId: String): Flow<List<VodEntity>>

    @Query("""
        SELECT v.* 
        FROM vod_streams v
        LEFT JOIN categories cat ON v.category_id = cat.category_id
        WHERE v.name LIKE '%' || :query || '%'
           OR cat.name LIKE '%' || :query || '%'
           OR (:fallback != '' AND (v.name LIKE '%' || :fallback || '%' OR cat.name LIKE '%' || :fallback || '%'))
        ORDER BY 
           CASE 
             WHEN v.name LIKE :query || '%' THEN 1
             WHEN v.name LIKE '%' || :query || '%' THEN 2
             WHEN :fallback != '' AND v.name LIKE '%' || :fallback || '%' THEN 3
             ELSE 4
           END,
           v.name ASC
        LIMIT 50
    """)
    fun searchMovies(query: String, fallback: String): Flow<List<VodEntity>>

    @Query("SELECT * FROM vod_streams WHERE stream_id = :streamId LIMIT 1")
    suspend fun getMovieById(streamId: Int): VodEntity?

    @Upsert
    suspend fun upsertAll(movies: List<VodEntity>)

    @Query("""
        SELECT v.* 
        FROM vod_streams v
        INNER JOIN favorites f ON v.stream_id = f.stream_id
        WHERE f.type = 'vod'
        ORDER BY f.added_at DESC
    """)
    fun observeFavoriteMovies(): Flow<List<VodEntity>>

    @Query("DELETE FROM vod_streams WHERE category_id = :categoryId")
    suspend fun deleteByCategory(categoryId: String)
}
