package com.fastiptv.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastiptv.data.api.XtreamApi
import com.fastiptv.data.session.SessionManager
import com.fastiptv.domain.model.ServerConfig
import com.fastiptv.domain.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import com.fastiptv.data.sync.SyncManager
import com.fastiptv.data.sync.SyncStatus
import kotlinx.coroutines.launch
import com.fastiptv.ota.OtaUpdateManager
import com.fastiptv.ota.OtaUpdateState
import com.fastiptv.ota.UpdateInfo
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val api: XtreamApi,
    private val repository: IptvRepository,
    private val syncManager: SyncManager,
    private val otaUpdateManager: OtaUpdateManager
) : ViewModel() {

    val currentConfig: StateFlow<ServerConfig?> = sessionManager.serverConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val preferredStreamFormat: StateFlow<String> = sessionManager.preferredStreamFormatFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ts")

    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus

    val updateState: StateFlow<OtaUpdateState> = otaUpdateManager.updateState

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun checkForUpdates() {
        viewModelScope.launch {
            otaUpdateManager.checkForUpdates()
        }
    }

    fun downloadAndInstallUpdate(updateInfo: UpdateInfo) {
        viewModelScope.launch {
            otaUpdateManager.downloadAndInstall(updateInfo)
        }
    }

    fun installDownloadedApk(apkFile: File) {
        otaUpdateManager.launchPackageInstaller(apkFile)
    }

    fun setPreferredStreamFormat(format: String) {
        viewModelScope.launch {
            sessionManager.savePreferredStreamFormat(format)
            _statusMessage.value = "Stream format set to ${format.uppercase()}."
        }
    }

    fun saveConfig(host: String, portStr: String, username: String, pass: String) {
        val cleanHost = host.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
        val port = portStr.toIntOrNull() ?: 80

        android.util.Log.d("FastIPTV", "saveConfig: host=$cleanHost, port=$port, username=$username")

        if (cleanHost.isBlank() || username.isBlank() || pass.isBlank()) {
            _statusMessage.value = "Error: All fields are required."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _statusMessage.value = "Connecting to server..."
            try {
                val config = ServerConfig(
                    host = cleanHost,
                    port = port,
                    username = username.trim(),
                    password = pass.trim()
                )
                sessionManager.saveServerConfig(config)
                android.util.Log.d("FastIPTV", "saveConfig: saved to SessionManager")

                // Test authentication
                val auth = api.authenticate()
                android.util.Log.d("FastIPTV", "saveConfig: authenticate result auth=${auth.userInfo?.auth}")
                if (auth.userInfo?.auth == 1) {
                    _statusMessage.value = "Connected! Account: ${auth.userInfo.status ?: "Active"}. Starting full sync..."
                    syncManager.startFullSync(force = true)
                } else {
                    _statusMessage.value = "Connected, but server reported inactive auth."
                }
            } catch (e: Exception) {
                android.util.Log.e("FastIPTV", "saveConfig error: ${e.message}", e)
                _statusMessage.value = "Saved, but connection test failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshAllCategories() {
        syncManager.startFullSync(force = true)
        _statusMessage.value = "Full catalog background sync started."
    }

    fun clearSession() {
        viewModelScope.launch {
            sessionManager.clearSession()
            _statusMessage.value = "Credentials cleared."
        }
    }
}
