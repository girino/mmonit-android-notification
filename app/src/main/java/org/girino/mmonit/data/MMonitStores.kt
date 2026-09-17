package org.girino.mmonit.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.girino.mmonit.domain.MMonitLevel
import org.girino.mmonit.domain.MMonitStatusSnapshot

private val Context.configDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mmonit_config",
)

private val Context.statusDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mmonit_status",
)

class ConfigStore(private val context: Context) {
    private val secureValues = SecureValueStore()

    suspend fun load(): MMonitConfig? {
        val preferences = context.configDataStore.data.first()
        val serverUrl = preferences[Keys.SERVER_URL] ?: return null
        val username = preferences[Keys.USERNAME]?.let(secureValues::decrypt) ?: return null
        val password = preferences[Keys.PASSWORD]?.let(secureValues::decrypt) ?: return null
        return MMonitConfig(serverUrl, username, password)
    }

    suspend fun save(config: MMonitConfig) {
        val normalized = config.normalized()
        context.configDataStore.edit { preferences ->
            preferences[Keys.SERVER_URL] = normalized.serverUrl
            preferences[Keys.USERNAME] = secureValues.encrypt(normalized.username)
            preferences[Keys.PASSWORD] = secureValues.encrypt(normalized.password)
        }
    }

    suspend fun clear() {
        context.configDataStore.edit { it.clear() }
    }

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
    }
}

class StatusStore(private val context: Context) {
    fun observe(): Flow<MMonitStatusSnapshot?> = context.statusDataStore.data.map { preferences ->
        val level = preferences[Keys.LEVEL]?.let { name ->
            MMonitLevel.values().firstOrNull { it.name == name }
        } ?: return@map null
        MMonitStatusSnapshot(
            level = level,
            detail = preferences[Keys.DETAIL].orEmpty(),
            checkedAt = preferences[Keys.CHECKED_AT] ?: 0L,
        )
    }

    suspend fun save(status: MMonitStatusSnapshot) {
        context.statusDataStore.edit { preferences ->
            preferences[Keys.LEVEL] = status.level.name
            preferences[Keys.DETAIL] = status.detail
            preferences[Keys.CHECKED_AT] = status.checkedAt
        }
    }

    suspend fun clear() {
        context.statusDataStore.edit { it.clear() }
    }

    private object Keys {
        val LEVEL = stringPreferencesKey("level")
        val DETAIL = stringPreferencesKey("detail")
        val CHECKED_AT = longPreferencesKey("checked_at")
    }
}
