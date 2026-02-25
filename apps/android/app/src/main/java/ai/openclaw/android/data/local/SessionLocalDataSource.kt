package ai.openclaw.android.data.local

import ai.openclaw.android.domain.model.Session
import ai.openclaw.android.domain.model.SessionListItem
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 会话本地数据源
 *
 * 使用 DataStore 存储会话数据，提供持久化支持
 */
@Singleton
class SessionLocalDataSource @Inject constructor(
    private val context: Context,
    private val json: Json
) {
    companion object {
        private const val DATA_STORE_NAME = "sessions"
        private const val ACTIVE_SESSION_KEY = "active_session_id"
        private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = DATA_STORE_NAME)
    }

    private val dataStore: DataStore<Preferences> = context.sessionDataStore

    // 内存缓存
    private val sessionCache = mutableMapOf<String, Session>()

    /**
     * 获取所有会话
     *
     * @return 会话列表流
     */
    fun getAllSessions(): Flow<List<Session>> {
        return dataStore.data.map { preferences ->
            preferences.asMap()
                .filterKeys { it.name.startsWith("session_") }
                .mapNotNull { (_, value) ->
                    try {
                        json.decodeFromString<Session>(value as String)
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to parse session")
                        null
                    }
                }
                .sortedByDescending { it.updatedAt }
        }
    }

    /**
     * 获取会话列表项
     *
     * @return 会话列表项流
     */
    fun getSessionListItems(): Flow<List<SessionListItem>> {
        return getAllSessions().map { sessions ->
            sessions.map { SessionListItem.from(it) }
        }
    }

    /**
     * 获取会话
     *
     * @param sessionId 会话 ID
     * @return 会话，如果不存在则返回 null
     */
    suspend fun getSession(sessionId: String): Session? {
        // 先检查缓存
        sessionCache[sessionId]?.let { return it }

        return try {
            val preferences = dataStore.data.first()
            val key = stringPreferencesKey(getKeyForSession(sessionId))
            val sessionJson = preferences[key] ?: return null

            val session = json.decodeFromString<Session>(sessionJson)
            sessionCache[sessionId] = session
            session
        } catch (e: Exception) {
            Timber.e(e, "Failed to get session: $sessionId")
            null
        }
    }

    /**
     * 保存会话
     *
     * @param session 会话
     */
    suspend fun saveSession(session: Session) {
        try {
            dataStore.edit { preferences ->
                val key = stringPreferencesKey(getKeyForSession(session.id))
                preferences[key] = json.encodeToString(session)
            }
            // 更新缓存
            sessionCache[session.id] = session
            Timber.d("Saved session: ${session.id}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save session: ${session.id}")
            throw e
        }
    }

    /**
     * 删除会话
     *
     * @param sessionId 会话 ID
     */
    suspend fun deleteSession(sessionId: String) {
        try {
            dataStore.edit { preferences ->
                val key = stringPreferencesKey(getKeyForSession(sessionId))
                preferences.remove(key)
            }
            // 清除缓存
            sessionCache.remove(sessionId)
            Timber.d("Deleted session: $sessionId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete session: $sessionId")
            throw e
        }
    }

    /**
     * 获取活跃会话 ID
     *
     * @return 活跃会话 ID，如果没有则返回 null
     */
    suspend fun getActiveSessionId(): String? {
        return try {
            val preferences = dataStore.data.first()
            val key = stringPreferencesKey(ACTIVE_SESSION_KEY)
            preferences[key]
        } catch (e: Exception) {
            Timber.e(e, "Failed to get active session id")
            null
        }
    }

    /**
     * 设置活跃会话
     *
     * @param sessionId 会话 ID
     */
    suspend fun setActiveSession(sessionId: String) {
        try {
            dataStore.edit { preferences ->
                val key = stringPreferencesKey(ACTIVE_SESSION_KEY)
                preferences[key] = sessionId
            }
            Timber.d("Set active session: $sessionId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to set active session: $sessionId")
            throw e
        }
    }

    /**
     * 清除活跃会话
     */
    suspend fun clearActiveSession() {
        try {
            dataStore.edit { preferences ->
                val key = stringPreferencesKey(ACTIVE_SESSION_KEY)
                preferences.remove(key)
            }
            Timber.d("Cleared active session")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear active session")
            throw e
        }
    }

    /**
     * 获取会话数量
     *
     * @return 会话数量
     */
    suspend fun getSessionCount(): Int {
        return try {
            val preferences = dataStore.data.first()
            preferences.keys.count { it.name.startsWith("session_") }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 清除所有会话
     */
    suspend fun clearAllSessions() {
        try {
            dataStore.edit { preferences ->
                val keysToRemove = preferences.keys.filter { it.name.startsWith("session_") }
                keysToRemove.forEach { preferences.remove(it) }
            }
            sessionCache.clear()
            Timber.d("Cleared all sessions")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear all sessions")
            throw e
        }
    }

    /**
     * 搜索会话
     *
     * @param query 搜索关键词
     * @return 匹配的会话列表项
     */
    suspend fun searchSessions(query: String): List<SessionListItem> {
        val allSessions = getAllSessions().first()
        val lowerQuery = query.lowercase()

        return allSessions
            .filter { session ->
                session.title?.lowercase()?.contains(lowerQuery) == true ||
                session.messages.any { it.content.lowercase().contains(lowerQuery) }
            }
            .map { SessionListItem.from(it) }
    }

    /**
     * 获取指定提供商的会话
     *
     * @param provider 提供商 ID
     * @return 会话列表
     */
    suspend fun getSessionsByProvider(provider: String): List<Session> {
        val allSessions = getAllSessions().first()
        return allSessions.filter { it.provider == provider }
    }

    /**
     * 删除指定提供商的所有会话
     *
     * @param provider 提供商 ID
     */
    suspend fun deleteSessionsByProvider(provider: String) {
        val sessions = getSessionsByProvider(provider)
        sessions.forEach { session ->
            deleteSession(session.id)
        }
    }

    /**
     * 导出会话
     *
     * @param sessionId 会话 ID
     * @return 会话的 JSON 字符串
     */
    suspend fun exportSession(sessionId: String): String? {
        val session = getSession(sessionId) ?: return null
        return json.encodeToString(session)
    }

    /**
     * 导入会话
     *
     * @param jsonStr 会话的 JSON 字符串
     * @return 导入的会话
     */
    suspend fun importSession(jsonStr: String): Session {
        val session = json.decodeFromString<Session>(jsonStr)
        // 生成新 ID 避免冲突
        val newSession = session.copy(id = UUID.randomUUID().toString())
        saveSession(newSession)
        return newSession
    }

    private fun getKeyForSession(sessionId: String): String = "session_$sessionId"
}
