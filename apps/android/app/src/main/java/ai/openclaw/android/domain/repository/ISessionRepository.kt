package ai.openclaw.android.domain.repository

import ai.openclaw.android.domain.model.Session
import ai.openclaw.android.domain.model.SessionListItem
import kotlinx.coroutines.flow.Flow

/**
 * 会话仓库接口
 *
 * 定义会话存储相关的操作，包括 CRUD 和活跃会话管理
 */
interface ISessionRepository {

    /**
     * 获取所有会话列表
     *
     * @return 会话列表流，按更新时间降序排列
     */
    fun getAllSessions(): Flow<List<Session>>

    /**
     * 获取会话列表项（用于 UI 显示）
     *
     * @return 会话列表项流
     */
    fun getSessionListItems(): Flow<List<SessionListItem>>

    /**
     * 获取单个会话
     *
     * @param sessionId 会话 ID
     * @return 会话，如果不存在则返回 null
     */
    suspend fun getSession(sessionId: String): Session?

    /**
     * 获取当前活跃会话
     *
     * @return 活跃会话流，如果没有活跃会话则返回 null
     */
    fun getActiveSession(): Flow<Session?>

    /**
     * 获取活跃会话 ID
     *
     * @return 活跃会话 ID，如果没有则返回 null
     */
    suspend fun getActiveSessionId(): String?

    /**
     * 保存会话
     *
     * 如果会话已存在则更新，否则创建新会话
     *
     * @param session 会话
     */
    suspend fun saveSession(session: Session)

    /**
     * 删除会话
     *
     * @param sessionId 会话 ID
     */
    suspend fun deleteSession(sessionId: String)

    /**
     * 设置活跃会话
     *
     * @param sessionId 会话 ID
     */
    suspend fun setActiveSession(sessionId: String)

    /**
     * 清除活跃会话
     */
    suspend fun clearActiveSession()

    /**
     * 创建新会话
     *
     * @param provider 提供商 ID
     * @return 新创建的会话
     */
    suspend fun createSession(provider: String): Session

    /**
     * 创建新会话并设为活跃
     *
     * @param provider 提供商 ID
     * @return 新创建的会话
     */
    suspend fun createAndActivateSession(provider: String): Session

    /**
     * 获取会话数量
     *
     * @return 会话数量
     */
    suspend fun getSessionCount(): Int

    /**
     * 清除所有会话
     */
    suspend fun clearAllSessions()

    /**
     * 搜索会话
     *
     * @param query 搜索关键词
     * @return 匹配的会话列表
     */
    suspend fun searchSessions(query: String): List<SessionListItem>

    /**
     * 获取指定提供商的会话
     *
     * @param provider 提供商 ID
     * @return 会话列表
     */
    suspend fun getSessionsByProvider(provider: String): List<Session>

    /**
     * 删除指定提供商的所有会话
     *
     * @param provider 提供商 ID
     */
    suspend fun deleteSessionsByProvider(provider: String)

    /**
     * 导出会话
     *
     * @param sessionId 会话 ID
     * @return 会话的 JSON 字符串
     */
    suspend fun exportSession(sessionId: String): String?

    /**
     * 导入会话
     *
     * @param json 会话的 JSON 字符串
     * @return 导入的会话
     */
    suspend fun importSession(json: String): Session
}
