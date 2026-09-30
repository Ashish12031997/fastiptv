package com.fastiptv.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fastiptv.domain.model.ContentRegion
import com.fastiptv.domain.model.ServerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fastiptv_session")

@Singleton
class SessionManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        val KEY_HOST = stringPreferencesKey("server_host")
        val KEY_PORT = intPreferencesKey("server_port")
        val KEY_USERNAME = stringPreferencesKey("server_username")
        val KEY_PASSWORD = stringPreferencesKey("server_password")
        val KEY_PROTOCOL = stringPreferencesKey("server_protocol")
        val KEY_STREAM_FORMAT = stringPreferencesKey("stream_format")
        val KEY_CONTENT_REGION = stringPreferencesKey("content_region")
        val KEY_PINNED_GROUPS = stringSetPreferencesKey("pinned_category_groups")
    }

    @Volatile
    private var cachedStreamFormat: String = "ts"

    @Volatile
    private var cachedConfig: ServerConfig? = null

    @Volatile
    private var cachedContentRegion: ContentRegion = ContentRegion.AUTO

    @Volatile
    private var cachedPinnedGroups: Set<String> = emptySet()

    val preferredStreamFormatFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_STREAM_FORMAT] ?: "ts"
    }

    val contentRegionFlow: Flow<ContentRegion> = context.dataStore.data.map { preferences ->
        ContentRegion.fromId(preferences[KEY_CONTENT_REGION])
    }

    val pinnedGroupsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_PINNED_GROUPS] ?: emptySet()
    }

    val serverConfigFlow: Flow<ServerConfig?> = context.dataStore.data.map { preferences ->
        val host = preferences[KEY_HOST]
        val port = preferences[KEY_PORT] ?: 80
        val username = preferences[KEY_USERNAME]
        val password = preferences[KEY_PASSWORD]
        val protocol = preferences[KEY_PROTOCOL] ?: "http"

        if (!host.isNullOrBlank() && !username.isNullOrBlank() && !password.isNullOrBlank()) {
            ServerConfig(
                host = host,
                port = port,
                username = username,
                password = password,
                protocol = protocol
            )
        } else {
            null
        }
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            serverConfigFlow.collect { config ->
                cachedConfig = config
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            preferredStreamFormatFlow.collect { format ->
                cachedStreamFormat = format
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            contentRegionFlow.collect { region ->
                cachedContentRegion = region
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            pinnedGroupsFlow.collect { pinned ->
                cachedPinnedGroups = pinned
            }
        }
    }

    fun getCachedConfig(): ServerConfig? = cachedConfig

    fun getCachedStreamFormat(): String = cachedStreamFormat

    fun getCachedContentRegion(): ContentRegion = cachedContentRegion

    fun getCachedPinnedGroups(): Set<String> = cachedPinnedGroups

    suspend fun savePreferredStreamFormat(format: String) {
        cachedStreamFormat = format
        context.dataStore.edit { preferences ->
            preferences[KEY_STREAM_FORMAT] = format
        }
    }

    suspend fun saveContentRegion(region: ContentRegion) {
        cachedContentRegion = region
        context.dataStore.edit { preferences ->
            preferences[KEY_CONTENT_REGION] = region.id
        }
    }

    suspend fun togglePinGroup(groupName: String): Boolean {
        val upper = groupName.trim().uppercase()
        var nowPinned = false
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_PINNED_GROUPS] ?: emptySet()
            val updated = if (current.contains(upper)) {
                nowPinned = false
                current - upper
            } else {
                nowPinned = true
                current + upper
            }
            preferences[KEY_PINNED_GROUPS] = updated
            cachedPinnedGroups = updated
        }
        return nowPinned
    }

    suspend fun unpinGroup(groupName: String) {
        val upper = groupName.trim().uppercase()
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_PINNED_GROUPS] ?: emptySet()
            val updated = current - upper
            preferences[KEY_PINNED_GROUPS] = updated
            cachedPinnedGroups = updated
        }
    }

    suspend fun unpinAllGroups() {
        context.dataStore.edit { preferences ->
            preferences[KEY_PINNED_GROUPS] = emptySet()
            cachedPinnedGroups = emptySet()
        }
    }

    suspend fun saveServerConfig(config: ServerConfig) {
        cachedConfig = config
        context.dataStore.edit { preferences ->
            preferences[KEY_HOST] = config.host
            preferences[KEY_PORT] = config.port
            preferences[KEY_USERNAME] = config.username
            preferences[KEY_PASSWORD] = config.password
            preferences[KEY_PROTOCOL] = config.protocol
        }
    }

    suspend fun clearSession() {
        cachedConfig = null
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
