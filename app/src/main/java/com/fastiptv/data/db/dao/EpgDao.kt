package com.fastiptv.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fastiptv.data.db.entity.EpgProgramEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgDao {
    @Query("SELECT * FROM epg_programs WHERE channel_id = :channelId AND end_time >= :currentTime ORDER BY start_time ASC LIMIT :limit")
    fun observeProgramsForChannel(channelId: String, currentTime: Long, limit: Int = 10): Flow<List<EpgProgramEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<EpgProgramEntity>)

    @Query("DELETE FROM epg_programs WHERE channel_id = :channelId")
    suspend fun deleteForChannel(channelId: String)

    @Query("DELETE FROM epg_programs WHERE end_time < :olderThanTime")
    suspend fun purgeOldPrograms(olderThanTime: Long)
}
