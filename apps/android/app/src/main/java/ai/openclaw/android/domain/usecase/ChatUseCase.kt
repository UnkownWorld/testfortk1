package ai.openclaw.android.domain.usecase

import ai.openclaw.android.domain.model.*
import ai.openclaw.android.domain.repository.IAuthProvider
import ai.openclaw.android.domain.repository.IChatProviderFactory
import ai.openclaw.android.domain.repository.ISessionRepository
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天用例
 *
 * 封装聊天相关的业务逻辑，协调认证、会话和聊天提供者
 *
 * 使用示例：
 * ```kotlin
 * // 发送消息
 * chatUseCase.sendMessage("你好")
 *     .collect { chunk ->
 *         when (chunk) {
 *             is ChatChunk.Text -> print(chunk.text)
 *             is ChatChunk.Done -> println("完成")
 *             is ChatChunk.Error -> println("错误: ${chunk.message}")
 *             else -> {}
 *         }
 *     }
 *
 * // 中止对话
 * chatUseCase.abort()
 * ```
 */
@Singleton
class ChatUseCase @Inject constructor(
    private val authProvider: IAuthProvider,
    private val chatProviderFactory: IChatProviderFactory,
    private val sessionRepository: ISessionRepository
) {
    /**
     * 发送消息
     *
     * @param message 消息内容
     * @param session 会话（可选，不传则使用当前活跃会话）
     * @param options 聊天选项（可选）
     * @return 响应块流
     */
    fun sendMessage(
        message: String,
        session: Session? = null,
        options: ChatOptions? = null
    ): Flow<ChatChunk> = flow {
        // 1. 获取或创建会话
        val currentSession = session ?: sessionRepository.getActiveSession().first()
            ?: throw ChatException.NoActiveSession()

        // 2. 获取提供商
        val provider = Provider.fromId(currentSession.provider)
            ?: throw ChatException.InvalidProvider(currentSession.provider)

        // 3. 获取认证配置
        val authConfig = authProvider.getAuthConfig(provider)
            ?: throw ChatException.NotAuthenticated(provider)

        // 检查认证是否有效
        if (!authConfig.isValid()) {
            throw ChatException.AuthExpired(provider)
        }

        // 4. 创建用户消息
        val userMessage = ChatMessage.user(message)
        var updatedSession = currentSession.addMessage(userMessage)
        sessionRepository.saveSession(updatedSession)

        // 5. 获取聊天提供者
        val chatProvider = chatProviderFactory.getProvider(provider)

        // 6. 发送消息并处理响应
        val assistantContent = StringBuilder()
        var hasError = false

        chatProvider.chat(
            messages = updatedSession.getContextMessages(),
            config = authConfig,
            sessionId = updatedSession.id,
            options = options
        ).catch { e ->
            Timber.e(e, "Chat error for ${provider.id}")
            
            when (e) {
                is ChatException.AuthExpired -> {
                    // 尝试刷新认证
                    val refreshed = authProvider.refresh(provider).getOrDefault(false)
                    if (refreshed) {
                        // 获取新配置并重试
                        val newConfig = authProvider.getAuthConfig(provider)
                        if (newConfig != null) {
                            emitAll(
                                chatProvider.chat(
                                    messages = updatedSession.getContextMessages(),
                                    config = newConfig,
                                    sessionId = updatedSession.id,
                                    options = options
                                )
                            )
                        } else {
                            emit(ChatChunk.error(
                                code = "AUTH_REFRESH_FAILED",
                                message = "认证刷新失败，请重新登录",
                                recoverable = true
                            ))
                        }
                    } else {
                        emit(ChatChunk.error(
                            code = "AUTH_EXPIRED",
                            message = "认证已过期，请重新登录",
                            recoverable = true
                        ))
                    }
                }
                is ChatException.RateLimited -> {
                    emit(ChatChunk.error(
                        code = "RATE_LIMITED",
                        message = "请求过于频繁，请稍后重试",
                        recoverable = true
                    ))
                }
                is ChatException.NetworkError -> {
                    emit(ChatChunk.error(
                        code = "NETWORK_ERROR",
                        message = "网络连接失败，请检查网络",
                        recoverable = true
                    ))
                }
                is ChatException.Timeout -> {
                    emit(ChatChunk.error(
                        code = "TIMEOUT",
                        message = "请求超时，请重试",
                        recoverable = true
                    ))
                }
                else -> {
                    emit(ChatChunk.error(
                        code = "CHAT_ERROR",
                        message = e.message ?: "Unknown error",
                        recoverable = false
                    ))
                }
            }
            hasError = true
        }.collect { chunk ->
            // 累积助手响应
            when (chunk) {
                is ChatChunk.Text -> {
                    assistantContent.append(chunk.text)
                    // 实时更新会话
                    updatedSession = updatedSession.appendToLastAssistantMessage(chunk.text)
                }
                is ChatChunk.Done -> {
                    // 保存最终会话
                    if (assistantContent.isNotEmpty() && !hasError) {
                        val finalSession = if (updatedSession.messages.lastOrNull()?.role == ChatMessage.Role.ASSISTANT) {
                            updatedSession
                        } else {
                            updatedSession.addMessage(ChatMessage.assistant(assistantContent.toString()))
                        }
                        sessionRepository.saveSession(finalSession.withAutoTitle())
                    }
                }
                is ChatChunk.Error -> {
                    hasError = true
                }
                else -> {}
            }

            emit(chunk)
        }
    }

    /**
     * 发送消息（带附件）
     *
     * @param message 消息内容
     * @param attachments 附件列表
     * @param session 会话（可选）
     * @return 响应块流
     */
    fun sendMessageWithAttachments(
        message: String,
        attachments: List<ChatMessage.Attachment>,
        session: Session? = null
    ): Flow<ChatChunk> {
        // 目前简化处理，将附件信息附加到消息中
        val messageWithAttachments = buildString {
            append(message)
            attachments.forEach { attachment ->
                append("\n[附件: ${attachment.type.name}]")
            }
        }
        
        return sendMessage(messageWithAttachments, session)
    }

    /**
     * 中止当前对话
     */
    fun abort() {
        Provider.values().forEach { provider ->
            try {
                if (chatProviderFactory.hasProvider(provider)) {
                    chatProviderFactory.getProvider(provider).abort()
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to abort provider: ${provider.id}")
            }
        }
    }

    /**
     * 重新生成最后一条回复
     *
     * @param session 会话（可选）
     * @return 响应块流
     */
    fun regenerate(session: Session? = null): Flow<ChatChunk> = flow {
        val currentSession = session ?: sessionRepository.getActiveSession().first()
            ?: throw ChatException.NoActiveSession()

        // 移除最后一条助手消息
        val messages = currentSession.messages.toMutableList()
        val lastAssistantIndex = messages.indexOfLast { it.role == ChatMessage.Role.ASSISTANT }
        
        if (lastAssistantIndex >= 0) {
            messages.removeAt(lastAssistantIndex)
            val updatedSession = currentSession.copy(messages = messages)
            sessionRepository.saveSession(updatedSession)

            // 获取最后一条用户消息
            val lastUserMessage = messages.lastOrNull { it.role == ChatMessage.Role.USER }
            if (lastUserMessage != null) {
                emitAll(sendMessage(lastUserMessage.content, updatedSession))
            } else {
                emit(ChatChunk.error("NO_MESSAGE", "没有可重新生成的消息"))
            }
        } else {
            emit(ChatChunk.error("NO_MESSAGE", "没有可重新生成的消息"))
        }
    }

    /**
     * 继续对话（在指定消息后继续）
     *
     * @param message 新消息
     * @param afterMessageId 在此消息后继续
     * @return 响应块流
     */
    fun continueFrom(
        message: String,
        afterMessageId: String
    ): Flow<ChatChunk> = flow {
        val activeSession = sessionRepository.getActiveSession().first()
            ?: throw ChatException.NoActiveSession()

        // 找到指定消息的位置
        val messageIndex = activeSession.messages.indexOfFirst { it.id == afterMessageId }
        if (messageIndex < 0) {
            emit(ChatChunk.error("MESSAGE_NOT_FOUND", "指定的消息不存在"))
            return@flow
        }

        // 截取消息历史
        val truncatedMessages = activeSession.messages.take(messageIndex + 1)
        val truncatedSession = activeSession.copy(messages = truncatedMessages)
        sessionRepository.saveSession(truncatedSession)

        // 发送新消息
        emitAll(sendMessage(message, truncatedSession))
    }

    /**
     * 检查是否可以重新生成
     *
     * @param session 会话（可选）
     * @return 如果可以重新生成则返回 true
     */
    suspend fun canRegenerate(session: Session? = null): Boolean {
        val currentSession = session ?: sessionRepository.getActiveSession().first()
            ?: return false

        return currentSession.messages.any { it.role == ChatMessage.Role.ASSISTANT }
    }

    /**
     * 获取建议的下一步操作
     *
     * @param session 会话（可选）
     * @return 建议的操作列表
     */
    suspend fun getSuggestedActions(session: Session? = null): List<String> {
        val currentSession = session ?: sessionRepository.getActiveSession().first()
            ?: return emptyList()

        // 基于会话历史生成建议
        // 这里可以集成 AI 来生成更智能的建议
        return listOf(
            "继续对话",
            "重新生成",
            "新对话"
        )
    }
}
