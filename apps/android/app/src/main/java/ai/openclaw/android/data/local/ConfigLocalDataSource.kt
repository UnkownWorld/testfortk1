package ai.openclaw.android.data.local

import ai.openclaw.android.data.remote.config.ModelsConfigResponse
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 配置本地数据源
 *
 * 缓存远程配置到本地
 */
@Singleton
class ConfigLocalDataSource @Inject constructor(
    private val context: Context,
    private val json: Json
) {
    companion object {
        private const val DATA_STORE_NAME = "model_config"
        private val Context.configDataStore: DataStore<Preferences> by preferencesDataStore(name = DATA_STORE_NAME)
        
        private val CONFIG_KEY = stringPreferencesKey("models_config")
        private val VERSION_KEY = stringPreferencesKey("config_version")
        private val LAST_UPDATE_KEY = stringPreferencesKey("last_update_time")
    }

    private val dataStore = context.configDataStore

    /**
     * 保存配置
     */
    suspend fun saveConfig(config: ModelsConfigResponse) {
        try {
            dataStore.edit { preferences ->
                preferences[CONFIG_KEY] = json.encodeToString(config)
                preferences[VERSION_KEY] = config.version
                preferences[LAST_UPDATE_KEY] = System.currentTimeMillis().toString()
            }
            Timber.d("Saved config: version=${config.version}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save config")
        }
    }

    /**
     * 获取配置
     */
    suspend fun getConfig(): ModelsConfigResponse? {
        return try {
            val preferences = dataStore.data.first()
            val configJson = preferences[CONFIG_KEY] ?: return null
            json.decodeFromString<ModelsConfigResponse>(configJson)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get config")
            null
        }
    }

    /**
     * 获取配置版本
     */
    suspend fun getConfigVersion(): String? {
        return try {
            val preferences = dataStore.data.first()
            preferences[VERSION_KEY]
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取上次更新时间
     */
    suspend fun getLastUpdateTime(): Long? {
        return try {
            val preferences = dataStore.data.first()
            preferences[LAST_UPDATE_KEY]?.toLong()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查是否需要更新（默认每24小时检查一次）
     */
    suspend fun needsUpdate(checkIntervalMs: Long = 24 * 60 * 60 * 1000L): Boolean {
        val lastUpdate = getLastUpdateTime() ?: return true
        return System.currentTimeMillis() - lastUpdate > checkIntervalMs
    }

    /**
     * 清除配置
     */
    suspend fun clearConfig() {
        try {
            dataStore.edit { preferences ->
                preferences.remove(CONFIG_KEY)
                preferences.remove(VERSION_KEY)
                preferences.remove(LAST_UPDATE_KEY)
            }
            Timber.d("Cleared config")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear config")
        }
    }
}
