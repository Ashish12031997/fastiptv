package com.fastiptv

import android.app.Application
import com.fastiptv.data.session.SessionManager
import com.fastiptv.data.sync.FullSyncWorker
import com.fastiptv.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FastIptvApp : Application() {

    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate() {
        super.onCreate()

        // Schedule periodic 12h background sync via WorkManager
        try {
            FullSyncWorker.schedulePeriodicSync(this)
        } catch (e: Exception) {
            android.util.Log.e("FastIPTV", "Failed to schedule periodic sync", e)
        }

        // Trigger background sync on app start if user is logged in
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val config = sessionManager.serverConfigFlow.first()
                if (config != null && config.host.isNotBlank() && config.username.isNotBlank()) {
                    syncManager.startFullSync()
                }
            } catch (e: Exception) {
                android.util.Log.e("FastIPTV", "Error starting initial sync", e)
            }
        }
    }
}
