package ai.openclaw.android.data.local

import ai.openclaw.android.domain.model.AuthConfig
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
 * 认证本地数据源
 *
 * 使用 DataStore 存储认证配置，提供持久化支持
 */
@Singleton
class AuthLocalDataSource @Inject constructor(
    private val context: Context,
    private val json: Json
) {
    companion object {
        private const val DATA_STORE_NAME = "auth_configs"
        private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = DATA_STORE_NAME)
    }

    private val dataStore: DataStore<Preferences> = context.authDataStore

    /**
     * 保存认证配置
     *
     * @param config 认证配置
     */
    suspend fun saveAuthConfig(config: AuthConfig) {
        try {
            dataStore.edit { preferences ->
                val key = stringPreferencesKey(getKeyForProvider(config.provider))
                preferences[key] = json.encodeToString(config)
            }
            Timber.d("Saved auth config for provider: ${config.provider}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save auth config for provider: ${config.provider}")
            throw e
        }
    }

    /**
     * 获取认证配置
     *
     * @param providerId 提供商 ID
     * @return 认证配置，如果不存在则返回 null
     */
    suspend fun getAuthConfig(providerId: String): AuthConfig? {
        return try {
            val preferences = dataStore.data.first()
            val key = stringPreferencesKey(getKeyForProvider(providerId))
            val configJson = preferences[key] ?: return null

            json.decodeFromString<AuthConfig>(configJson)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get auth config for provider: $providerId")
            null
        }
    }

    /**
     * 删除认证配置
     *
     * @param providerId 提供商 ID
     */
    suspend fun deleteAuthConfig(providerId: String) {
        try {
            dataStore.edit { preferences ->
                val key = stringPreferencesKey(getKeyForProvider(providerId))
                preferences.remove(key)
            }
            Timber.d("Deleted auth config for provider: $providerId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete auth config for provider: $providerId")
            throw e
        }
    }

    /**
     * 获取所有认证配置
     *
     * @return 认证配置列表
     */
    suspend fun getAllAuthConfigs(): List<AuthConfig> {
        return try {
            val preferences = dataStore.data.first()
            preferences.asMap()
                .filterKeys { it.name.startsWith("auth_") }
                .mapNotNull { (_, value) ->
                    try {
                        json.decodeFromString<AuthConfig>(value as String)
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to parse auth config")
                        null
                    }
                }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get all auth configs")
            emptyList()
        }
    }

    /**
     * 检查是否存在认证配置
     *
     * @param providerId 提供商 ID
     * @return 如果存在则返回 true
     */
    suspend fun hasAuthConfig(providerId: String): Boolean {
        return try {
            val preferences = dataStore.data.first()
            val key = stringPreferencesKey(getKeyForProvider(providerId))
            preferences.contains(key)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 清除所有认证配置
     */
    suspend fun clearAll() {
        try {
            dataStore.edit { preferences ->
                val keysToRemove = preferences.keys.filter { it.name.startsWith("auth_") }
                keysToRemove.forEach { preferences.remove(it) }
            }
            Timber.d("Cleared all auth configs")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear all auth configs")
            throw e
        }
    }

    private fun getKeyForProvider(providerId: String): String = "auth_$providerId"
}
