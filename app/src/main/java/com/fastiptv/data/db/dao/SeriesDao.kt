package com.fastiptv.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fastiptv.data.db.entity.SeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series WHERE category_id = :categoryId ORDER BY series_id DESC")
    fun observeSeriesByCategory(categoryId: String): Flow<List<SeriesEntity>>

    @Query("""
        SELECT s.* 
        FROM series s
        LEFT JOIN categories cat ON s.category_id = cat.category_id
        WHERE s.name LIKE '%' || :query || '%'
           OR cat.name LIKE '%' || :query || '%'
           OR (:fallback != '' AND (s.name LIKE '%' || :fallback || '%' OR cat.name LIKE '%' || :fallback || '%'))
        ORDER BY 
           CASE 
             WHEN s.name LIKE :query || '%' THEN 1
             WHEN s.name LIKE '%' || :query || '%' THEN 2
             WHEN :fallback != '' AND s.name LIKE '%' || :fallback || '%' THEN 3
             ELSE 4
           END,
           s.name ASC
        LIMIT 50
    """)
    fun searchSeries(query: String, fallback: String): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE series_id = :seriesId LIMIT 1")
    suspend fun getSeriesById(seriesId: Int): SeriesEntity?

    @Upsert
    suspend fun upsertAll(seriesList: List<SeriesEntity>)

    @Query("DELETE FROM series WHERE category_id = :categoryId")
    suspend fun deleteByCategory(categoryId: String)
}
