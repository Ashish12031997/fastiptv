package com.fastiptv.ota

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.fastiptv.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val apkSize: Long = 0L
)

sealed interface OtaUpdateState {
    data object Idle : OtaUpdateState
    data object Checking : OtaUpdateState
    data class UpToDate(val currentVersion: String) : OtaUpdateState
    data class UpdateAvailable(val updateInfo: UpdateInfo) : OtaUpdateState
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : OtaUpdateState
    data class ReadyToInstall(val apkFile: File) : OtaUpdateState
    data class Error(val message: String) : OtaUpdateState
}

@Singleton
class OtaUpdateManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "OtaUpdateManager"
        private const val GITHUB_OWNER = "Ashish12031997"
        private const val GITHUB_REPO = "fastiptv"
        private const val LATEST_RELEASE_API =
            "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    }

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonParser = Json { ignoreUnknownKeys = true }

    private val _updateState = MutableStateFlow<OtaUpdateState>(OtaUpdateState.Idle)
    val updateState: StateFlow<OtaUpdateState> = _updateState.asStateFlow()

    suspend fun checkForUpdates(): OtaUpdateState = withContext(Dispatchers.IO) {
        _updateState.value = OtaUpdateState.Checking
        try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_API)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "FastIPTV-AndroidTV/${BuildConfig.VERSION_NAME}")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                if (response.code == 404) {
                    // No release created yet on repository
                    val state = OtaUpdateState.UpToDate(BuildConfig.VERSION_NAME)
                    _updateState.value = state
                    return@withContext state
                }
                val error = "Failed to check updates (HTTP ${response.code})"
                _updateState.value = OtaUpdateState.Error(error)
                return@withContext OtaUpdateState.Error(error)
            }

            val body = response.body.string()
            if (body.isBlank()) {
                val state = OtaUpdateState.UpToDate(BuildConfig.VERSION_NAME)
                _updateState.value = state
                return@withContext state
            }

            val releaseJson = jsonParser.parseToJsonElement(body).jsonObject
            val tagName = releaseJson["tag_name"]?.jsonPrimitive?.content.orEmpty()
            val cleanRemoteVersion = tagName.removePrefix("v").removePrefix("V").trim()
            val releaseNotes = releaseJson["body"]?.jsonPrimitive?.content.orEmpty()

            val assets = releaseJson["assets"]?.jsonArray.orEmpty()
            var apkDownloadUrl: String? = null
            var apkSize = 0L

            for (asset in assets) {
                val assetObj = asset.jsonObject
                val name = assetObj["name"]?.jsonPrimitive?.content.orEmpty()
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkDownloadUrl = assetObj["browser_download_url"]?.jsonPrimitive?.content
                    apkSize = assetObj["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                    break
                }
            }

            if (apkDownloadUrl.isNullOrBlank()) {
                val state = OtaUpdateState.UpToDate(BuildConfig.VERSION_NAME)
                _updateState.value = state
                return@withContext state
            }

            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V").trim()
            val isNewer = isVersionNewer(cleanRemoteVersion, currentVersion)

            val state = if (isNewer) {
                OtaUpdateState.UpdateAvailable(
                    UpdateInfo(
                        versionName = cleanRemoteVersion,
                        downloadUrl = apkDownloadUrl,
                        releaseNotes = releaseNotes,
                        apkSize = apkSize
                    )
                )
            } else {
                OtaUpdateState.UpToDate(BuildConfig.VERSION_NAME)
            }

            _updateState.value = state
            state
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for OTA updates", e)
            val state = OtaUpdateState.Error(e.message ?: "Failed to check for updates")
            _updateState.value = state
            state
        }
    }

    suspend fun downloadAndInstall(updateInfo: UpdateInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            _updateState.value = OtaUpdateState.Downloading(0, 0L, updateInfo.apkSize)

            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val destinationFile = File(updatesDir, "FastIPTV-v${updateInfo.versionName}.apk")
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val request = Request.Builder()
                .url(updateInfo.downloadUrl)
                .header("User-Agent", "FastIPTV-AndroidTV/${BuildConfig.VERSION_NAME}")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                _updateState.value = OtaUpdateState.Error("Download failed with HTTP ${response.code}")
                return@withContext false
            }

            val body = response.body
            val contentLength = if (body.contentLength() > 0) body.contentLength() else updateInfo.apkSize
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var lastReportTime = System.currentTimeMillis()

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        val now = System.currentTimeMillis()
                        if (now - lastReportTime > 200 || downloadedBytes == contentLength) {
                            val percent = if (contentLength > 0L) {
                                ((downloadedBytes * 100) / contentLength).toInt().coerceIn(0, 100)
                            } else 0
                            _updateState.value = OtaUpdateState.Downloading(percent, downloadedBytes, contentLength)
                            lastReportTime = now
                        }
                    }
                    output.flush()
                }
            }

            _updateState.value = OtaUpdateState.ReadyToInstall(destinationFile)
            launchPackageInstaller(destinationFile)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading/installing update", e)
            _updateState.value = OtaUpdateState.Error(e.message ?: "Download failed")
            false
        }
    }

    fun launchPackageInstaller(apkFile: File) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer", e)
            _updateState.value = OtaUpdateState.Error("Unable to launch installer: ${e.message}")
        }
    }

    private fun isVersionNewer(remote: String, current: String): Boolean {
        if (remote == current) return false
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
