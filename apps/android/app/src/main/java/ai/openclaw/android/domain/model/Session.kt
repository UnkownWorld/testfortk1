package ai.openclaw.android.domain.model

import kotlinx.serialization.Serializable

/**
 * 会话
 *
 * 表示一个聊天会话，包含消息历史和元数据
 *
 * @property id 会话唯一标识
 * @property provider 提供商 ID
 * @property title 会话标题
 * @property messages 消息列表
 * @property createdAt 创建时间
 * @property updatedAt 更新时间
 * @property metadata 额外元数据
 */
@Serializable
data class Session(
    val id: String,
    val provider: String,
    val title: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * 添加消息到会话
     * @param message 要添加的消息
     * @return 包含新消息的会话副本
     */
    fun addMessage(message: ChatMessage): Session {
        return copy(
            messages = messages + message,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 更新最后一条助手消息
     * @param content 新内容
     * @return 更新后的会话
     */
    fun updateLastAssistantMessage(content: String): Session {
        val lastAssistantIndex = messages.indexOfLast { it.role == ChatMessage.Role.ASSISTANT }
        if (lastAssistantIndex == -1) return this

        val updatedMessages = messages.toMutableList()
        updatedMessages[lastAssistantIndex] = updatedMessages[lastAssistantIndex].copy(
            content = content
        )

        return copy(
            messages = updatedMessages,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 追加内容到最后一条助手消息
     * @param content 要追加的内容
     * @return 更新后的会话
     */
    fun appendToLastAssistantMessage(content: String): Session {
        val lastAssistantIndex = messages.indexOfLast { it.role == ChatMessage.Role.ASSISTANT }
        if (lastAssistantIndex == -1) {
            // 如果没有助手消息，创建一条新的
            return addMessage(ChatMessage.assistant(content))
        }

        val updatedMessages = messages.toMutableList()
        val lastMessage = updatedMessages[lastAssistantIndex]
        updatedMessages[lastAssistantIndex] = lastMessage.copy(
            content = lastMessage.content + content
        )

        return copy(
            messages = updatedMessages,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 获取消息上下文（用于 API 调用）
     * @param maxCount 最大消息数量
     * @return 截取的消息列表
     */
    fun getContextMessages(maxCount: Int = 20): List<ChatMessage> {
        return messages.takeLast(maxCount)
    }

    /**
     * 获取消息总 token 数估算
     * @return 估算的 token 数
     */
    fun estimateTokenCount(): Int {
        // 简单估算：平均每 4 个字符约 1 个 token
        return messages.sumOf { it.content.length / 4 }
    }

    /**
     * 清空消息历史
     * @return 清空后的会话
     */
    fun clearMessages(): Session {
        return copy(
            messages = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 设置标题
     * @param title 新标题
     * @return 更新后的会话
     */
    fun withTitle(title: String): Session {
        return copy(
            title = title,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 自动生成标题（基于第一条用户消息）
     * @return 带有自动生成标题的会话
     */
    fun withAutoTitle(): Session {
        if (title != null) return this

        val firstUserMessage = messages.firstOrNull { it.role == ChatMessage.Role.USER }
        val autoTitle = firstUserMessage?.content?.take(50)?.let {
            if (firstUserMessage.content.length > 50) "$it..." else it
        } ?: "新对话"

        return copy(title = autoTitle)
    }

    companion object {
        /**
         * 创建新会话
         * @param provider 提供商 ID
         * @return 新会话实例
         */
        fun create(provider: String): Session {
            return Session(
                id = java.util.UUID.randomUUID().toString(),
                provider = provider
            )
        }

        /**
         * 创建带初始消息的会话
         * @param provider 提供商 ID
         * @param initialMessage 初始消息
         * @return 新会话实例
         */
        fun createWithMessage(provider: String, initialMessage: ChatMessage): Session {
            return create(provider).addMessage(initialMessage)
        }
    }
}

/**
 * 会话列表项（用于 UI 显示）
 *
 * @property id 会话 ID
 * @property title 标题
 * @property provider 提供商
 * @property lastMessage 最后一条消息预览
 * @property updatedAt 更新时间
 */
data class SessionListItem(
    val id: String,
    val title: String,
    val provider: String,
    val lastMessage: String?,
    val updatedAt: Long
) {
    companion object {
        /**
         * 从 Session 创建列表项
         */
        fun from(session: Session): SessionListItem {
            val lastMessage = session.messages.lastOrNull()?.content?.take(100)
            return SessionListItem(
                id = session.id,
                title = session.title ?: "新对话",
                provider = session.provider,
                lastMessage = lastMessage,
                updatedAt = session.updatedAt
            )
        }
    }
}
