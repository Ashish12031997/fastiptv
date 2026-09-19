package com.fastiptv.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fastiptv.data.db.entity.RecentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDao {
    @Query("SELECT * FROM recents ORDER BY last_watched DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<RecentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecent(recent: RecentEntity)

    @Query("SELECT * FROM recents WHERE stream_id = :streamId LIMIT 1")
    suspend fun getRecentByStreamId(streamId: Int): RecentEntity?

    @Query("UPDATE recents SET watch_position_ms = :positionMs, duration_ms = :durationMs, last_watched = :lastWatched WHERE stream_id = :streamId")
    suspend fun updatePlaybackPosition(
        streamId: Int,
        positionMs: Long,
        durationMs: Long,
        lastWatched: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM recents WHERE stream_id = :streamId")
    suspend fun deleteRecent(streamId: Int)

    @Query("DELETE FROM recents")
    suspend fun clearRecents()
}
