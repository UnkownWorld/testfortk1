package ai.openclaw.android.domain.usecase

import ai.openclaw.android.domain.model.Session
import ai.openclaw.android.domain.model.SessionListItem
import ai.openclaw.android.domain.repository.ISessionRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 会话用例
 *
 * 封装会话相关的业务逻辑，提供统一的会话操作接口
 *
 * 使用示例：
 * ```kotlin
 * // 获取所有会话
 * sessionUseCase.getAllSessions()
 *     .collect { sessions -> ... }
 *
 * // 创建新会话
 * val newSession = sessionUseCase.createSession("deepseek")
 *
 * // 切换会话
 * sessionUseCase.switchSession(sessionId)
 *
 * // 删除会话
 * sessionUseCase.deleteSession(sessionId)
 * ```
 */
@Singleton
class SessionUseCase @Inject constructor(
    private val sessionRepository: ISessionRepository
) {
    /**
     * 获取所有会话
     *
     * @return 会话列表流
     */
    fun getAllSessions(): Flow<List<Session>> {
        return sessionRepository.getAllSessions()
    }

    /**
     * 获取会话列表项（用于 UI 显示）
     *
     * @return 会话列表项流
     */
    fun getSessionListItems(): Flow<List<SessionListItem>> {
        return sessionRepository.getSessionListItems()
    }

    /**
     * 获取当前活跃会话
     *
     * @return 活跃会话流
     */
    fun getActiveSession(): Flow<Session?> {
        return sessionRepository.getActiveSession()
    }

    /**
     * 获取会话
     *
     * @param sessionId 会话 ID
     * @return 会话，如果不存在则返回 null
     */
    suspend fun getSession(sessionId: String): Session? {
        return sessionRepository.getSession(sessionId)
    }

    /**
     * 创建新会话
     *
     * @param provider 提供商 ID
     * @return 新创建的会话
     */
    suspend fun createSession(provider: String): Session {
        return sessionRepository.createSession(provider)
    }

    /**
     * 创建新会话并设为活跃
     *
     * @param provider 提供商 ID
     * @return 新创建的会话
     */
    suspend fun createAndActivateSession(provider: String): Session {
        return sessionRepository.createAndActivateSession(provider)
    }

    /**
     * 切换会话
     *
     * @param sessionId 会话 ID
     */
    suspend fun switchSession(sessionId: String) {
        sessionRepository.setActiveSession(sessionId)
    }

    /**
     * 删除会话
     *
     * @param sessionId 会话 ID
     */
    suspend fun deleteSession(sessionId: String) {
        sessionRepository.deleteSession(sessionId)
    }

    /**
     * 清除活跃会话
     */
    suspend fun clearActiveSession() {
        sessionRepository.clearActiveSession()
    }

    /**
     * 保存会话
     *
     * @param session 会话
     */
    suspend fun saveSession(session: Session) {
        sessionRepository.saveSession(session)
    }

    /**
     * 获取会话数量
     *
     * @return 会话数量
     */
    suspend fun getSessionCount(): Int {
        return sessionRepository.getSessionCount()
    }

    /**
     * 清除所有会话
     */
    suspend fun clearAllSessions() {
        sessionRepository.clearAllSessions()
    }

    /**
     * 搜索会话
     *
     * @param query 搜索关键词
     * @return 匹配的会话列表
     */
    suspend fun searchSessions(query: String): List<SessionListItem> {
        if (query.isBlank()) return emptyList()
        return sessionRepository.searchSessions(query.trim())
    }

    /**
     * 获取指定提供商的会话
     *
     * @param provider 提供商 ID
     * @return 会话列表
     */
    suspend fun getSessionsByProvider(provider: String): List<Session> {
        return sessionRepository.getSessionsByProvider(provider)
    }

    /**
     * 删除指定提供商的所有会话
     *
     * @param provider 提供商 ID
     */
    suspend fun deleteSessionsByProvider(provider: String) {
        sessionRepository.deleteSessionsByProvider(provider)
    }

    /**
     * 导出会话
     *
     * @param sessionId 会话 ID
     * @return 会话的 JSON 字符串
     */
    suspend fun exportSession(sessionId: String): String? {
        return sessionRepository.exportSession(sessionId)
    }

    /**
     * 导入会话
     *
     * @param json 会话的 JSON 字符串
     * @return 导入的会话
     */
    suspend fun importSession(json: String): Session? {
        return try {
            sessionRepository.importSession(json)
        } catch (e: Exception) {
            Timber.e(e, "Failed to import session")
            null
        }
    }

    /**
     * 获取或创建活跃会话
     *
     * 如果没有活跃会话，则创建一个新的
     *
     * @param provider 提供商 ID（用于创建新会话）
     * @return 活跃会话
     */
    suspend fun getOrCreateActiveSession(provider: String): Session {
        val activeSession = sessionRepository.getActiveSession().first()
        return activeSession ?: sessionRepository.createAndActivateSession(provider)
    }

    /**
     * 确保会话存在
     *
     * @param sessionId 会话 ID
     * @return 会话
     * @throws SessionException.NotFound 如果会话不存在
     */
    suspend fun ensureSessionExists(sessionId: String): Session {
        return sessionRepository.getSession(sessionId)
            ?: throw ai.openclaw.android.domain.model.SessionException.NotFound(sessionId)
    }

    /**
     * 重命名会话
     *
     * @param sessionId 会话 ID
     * @param newTitle 新标题
     */
    suspend fun renameSession(sessionId: String, newTitle: String) {
        val session = sessionRepository.getSession(sessionId) ?: return
        sessionRepository.saveSession(session.withTitle(newTitle))
    }

    /**
     * 清空会话消息
     *
     * @param sessionId 会话 ID
     */
    suspend fun clearSessionMessages(sessionId: String) {
        val session = sessionRepository.getSession(sessionId) ?: return
        sessionRepository.saveSession(session.clearMessages())
    }

    /**
     * 复制会话
     *
     * @param sessionId 要复制的会话 ID
     * @return 新会话
     */
    suspend fun duplicateSession(sessionId: String): Session? {
        val original = sessionRepository.getSession(sessionId) ?: return null
        val newSession = Session.create(original.provider).copy(
            title = "${original.title ?: "对话"} (副本)",
            messages = original.messages,
            metadata = original.metadata
        )
        sessionRepository.saveSession(newSession)
        return newSession
    }

    /**
     * 批量删除会话
     *
     * @param sessionIds 会话 ID 列表
     */
    suspend fun deleteSessions(sessionIds: List<String>) {
        sessionIds.forEach { sessionId ->
            sessionRepository.deleteSession(sessionId)
        }
    }

    /**
     * 获取最近的会话
     *
     * @param limit 数量限制
     * @return 最近的会话列表
     */
    suspend fun getRecentSessions(limit: Int = 10): List<SessionListItem> {
        return sessionRepository.getSessionListItems().first().take(limit)
    }
}

/**
 * Flow 的 first() 扩展函数
 */
private suspend fun <T> Flow<T>.first(): T {
    var result: T? = null
    collect { value ->
        result = value
        return@collect
    }
    return result ?: throw NoSuchElementException("Flow is empty")
}
