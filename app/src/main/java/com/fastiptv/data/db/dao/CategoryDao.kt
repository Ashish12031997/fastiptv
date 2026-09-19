package com.fastiptv.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fastiptv.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY name ASC")
    fun observeCategories(type: String): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE type = :type")
    suspend fun deleteByType(type: String)

    @Query("SELECT COUNT(*) FROM categories WHERE type = :type")
    suspend fun countByType(type: String): Int

    @Query("""
        SELECT * FROM categories 
        WHERE name LIKE '%' || :query || '%'
           OR (:fallback != '' AND name LIKE '%' || :fallback || '%')
        ORDER BY name ASC 
        LIMIT 10
    """)
    fun searchCategories(query: String, fallback: String): Flow<List<CategoryEntity>>
}
