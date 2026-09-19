package com.fastiptv.data.epg

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fastiptv.data.db.dao.EpgDao
import com.fastiptv.data.db.entity.EpgProgramEntity
import com.fastiptv.domain.repository.IptvRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class EpgSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: IptvRepository,
    private val epgDao: EpgDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Purge programs older than 24 hours to keep Room db lean
            val yesterday = (System.currentTimeMillis() / 1000) - (24 * 60 * 60)
            epgDao.purgeOldPrograms(yesterday)

            // 2. Fetch short EPG for favorite channels first
            val favorites = repository.observeFavorites().first()
            for (channel in favorites) {
                repository.getShortEpg(channel.id).onSuccess { programs ->
                    val entities = programs.map { p ->
                        EpgProgramEntity(
                            channelId = channel.id.toString(),
                            title = p.title,
                            description = p.description,
                            startTime = p.startTimestamp,
                            endTime = p.endTimestamp
                        )
                    }
                    epgDao.insertPrograms(entities)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
