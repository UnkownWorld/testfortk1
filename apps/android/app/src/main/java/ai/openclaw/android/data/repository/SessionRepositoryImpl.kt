package ai.openclaw.android.data.repository

import ai.openclaw.android.data.local.SessionLocalDataSource
import ai.openclaw.android.domain.model.Session
import ai.openclaw.android.domain.model.SessionListItem
import ai.openclaw.android.domain.repository.ISessionRepository
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 会话仓库实现
 *
 * 协调本地数据源，提供统一的会话存储接口
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val localDataSource: SessionLocalDataSource
) : ISessionRepository {

    override fun getAllSessions(): Flow<List<Session>> {
        return localDataSource.getAllSessions()
    }

    override fun getSessionListItems(): Flow<List<SessionListItem>> {
        return localDataSource.getSessionListItems()
    }

    override suspend fun getSession(sessionId: String): Session? {
        return try {
            localDataSource.getSession(sessionId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get session: $sessionId")
            null
        }
    }

    override fun getActiveSession(): Flow<Session?> {
        return localDataSource.getActiveSessionId().map { sessionId ->
            sessionId?.let { localDataSource.getSession(it) }
        }
    }

    override suspend fun getActiveSessionId(): String? {
        return localDataSource.getActiveSessionId()
    }

    override suspend fun saveSession(session: Session) {
        try {
            localDataSource.saveSession(session)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save session: ${session.id}")
            throw ai.openclaw.android.domain.model.SessionException.SaveFailed(
                session.id, e
            )
        }
    }

    override suspend fun deleteSession(sessionId: String) {
        try {
            localDataSource.deleteSession(sessionId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete session: $sessionId")
        }
    }

    override suspend fun setActiveSession(sessionId: String) {
        try {
            // 验证会话存在
            val session = localDataSource.getSession(sessionId)
            if (session != null) {
                localDataSource.setActiveSession(sessionId)
            } else {
                Timber.w("Attempted to set non-existent session as active: $sessionId")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set active session: $sessionId")
        }
    }

    override suspend fun clearActiveSession() {
        try {
            localDataSource.clearActiveSession()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear active session")
        }
    }

    override suspend fun createSession(provider: String): Session {
        return Session.create(provider)
    }

    override suspend fun createAndActivateSession(provider: String): Session {
        val session = Session.create(provider)
        saveSession(session)
        setActiveSession(session.id)
        return session
    }

    override suspend fun getSessionCount(): Int {
        return localDataSource.getSessionCount()
    }

    override suspend fun clearAllSessions() {
        try {
            localDataSource.clearAllSessions()
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear all sessions")
        }
    }

    override suspend fun searchSessions(query: String): List<SessionListItem> {
        return try {
            localDataSource.searchSessions(query)
        } catch (e: Exception) {
            Timber.e(e, "Failed to search sessions")
            emptyList()
        }
    }

    override suspend fun getSessionsByProvider(provider: String): List<Session> {
        return try {
            localDataSource.getSessionsByProvider(provider)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get sessions by provider: $provider")
            emptyList()
        }
    }

    override suspend fun deleteSessionsByProvider(provider: String) {
        try {
            localDataSource.deleteSessionsByProvider(provider)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete sessions by provider: $provider")
        }
    }

    override suspend fun exportSession(sessionId: String): String? {
        return try {
            localDataSource.exportSession(sessionId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to export session: $sessionId")
            null
        }
    }

    override suspend fun importSession(json: String): Session {
        return try {
            localDataSource.importSession(json)
        } catch (e: Exception) {
            Timber.e(e, "Failed to import session")
            throw ai.openclaw.android.domain.model.SessionException.SaveFailed(
                "import", e
            )
        }
    }
}

/**
 * Flow 的 map 扩展函数
 */
private fun <T, R> Flow<T>.map(transform: suspend (T) -> R): Flow<R> {
    return kotlinx.coroutines.flow.map { transform(it) }
}
