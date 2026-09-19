package com.fastiptv.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
    }

    @Volatile
    private var cachedStreamFormat: String = "ts"

    @Volatile
    private var cachedConfig: ServerConfig? = null

    val preferredStreamFormatFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_STREAM_FORMAT] ?: "ts"
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
    }

    fun getCachedConfig(): ServerConfig? = cachedConfig

    fun getCachedStreamFormat(): String = cachedStreamFormat

    suspend fun savePreferredStreamFormat(format: String) {
        cachedStreamFormat = format
        context.dataStore.edit { preferences ->
            preferences[KEY_STREAM_FORMAT] = format
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
