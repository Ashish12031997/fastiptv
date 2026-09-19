package com.fastiptv.data.sync

import com.fastiptv.domain.repository.IptvRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncStatus {
    object Idle : SyncStatus
    data class Syncing(val message: String, val progress: Float = 0f) : SyncStatus
    data class Success(val message: String, val timestamp: Long = System.currentTimeMillis()) : SyncStatus
    data class Error(val message: String) : SyncStatus
}

@Singleton
class SyncManager @Inject constructor(
    private val repository: IptvRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    @Volatile
    private var isSyncing = false

    fun isSyncInProgress(): Boolean = isSyncing

    fun startFullSync(force: Boolean = false) {
        if (isSyncing) {
            android.util.Log.d("FastIPTV", "SyncManager: Sync already in progress, skipping request")
            return
        }
        isSyncing = true

        scope.launch {
            try {
                android.util.Log.d("FastIPTV", "SyncManager: Phase 1 - Categories starting...")
                _syncStatus.value = SyncStatus.Syncing("Updating categories...", 0.1f)
                repository.syncLiveCategories()
                repository.syncVodCategories()
                repository.syncSeriesCategories()

                android.util.Log.d("FastIPTV", "SyncManager: Phase 2 - Live channels starting...")
                _syncStatus.value = SyncStatus.Syncing("Syncing Live TV (11,000+ channels)...", 0.3f)
                repository.syncAllLiveChannels()

                android.util.Log.d("FastIPTV", "SyncManager: Phase 3 - TV Series starting...")
                _syncStatus.value = SyncStatus.Syncing("Syncing TV Series (17,000+ series)...", 0.6f)
                repository.syncAllSeries()

                android.util.Log.d("FastIPTV", "SyncManager: Phase 4 - Movies starting...")
                _syncStatus.value = SyncStatus.Syncing("Syncing Movies (64,000+ movies)...", 0.85f)
                repository.syncAllMovies()

                android.util.Log.d("FastIPTV", "SyncManager: All phases completed successfully")
                _syncStatus.value = SyncStatus.Success("Full catalog synced!")
            } catch (e: Exception) {
                android.util.Log.e("FastIPTV", "SyncManager: Sync failed - ${e.message}", e)
                _syncStatus.value = SyncStatus.Error("Sync failed: ${e.localizedMessage ?: "Network error"}")
            } finally {
                isSyncing = false
            }
        }
    }
}
