package ai.openclaw.android.domain.model

import kotlinx.serialization.Serializable

/**
 * 聊天消息
 *
 * 表示对话中的一条消息，包含角色、内容和附件等信息
 *
 * @property id 消息唯一标识
 * @property role 消息角色（用户/助手/系统/工具）
 * @property content 消息文本内容
 * @property timestamp 消息时间戳
 * @property attachments 附件列表
 */
@Serializable
data class ChatMessage(
    val id: String,
    val role: Role,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachments: List<Attachment> = emptyList()
) {
    /**
     * 消息角色枚举
     */
    @Serializable
    enum class Role {
        USER,
        ASSISTANT,
        SYSTEM,
        TOOL
    }

    /**
     * 消息附件
     *
     * @property id 附件唯一标识
     * @property type 附件类型
     * @property url 附件 URL
     * @property mimeType MIME 类型
     */
    @Serializable
    data class Attachment(
        val id: String,
        val type: Type,
        val url: String,
        val mimeType: String? = null
    ) {
        @Serializable
        enum class Type {
            IMAGE,
            FILE,
            AUDIO
        }
    }

    companion object {
        private fun generateId(): String = java.util.UUID.randomUUID().toString()

        /**
         * 创建用户消息
         * @param content 消息内容
         * @param attachments 附件列表
         * @return 用户消息实例
         */
        fun user(content: String, attachments: List<Attachment> = emptyList()): ChatMessage {
            return ChatMessage(
                id = generateId(),
                role = Role.USER,
                content = content,
                attachments = attachments
            )
        }

        /**
         * 创建助手消息
         * @param content 消息内容
         * @return 助手消息实例
         */
        fun assistant(content: String): ChatMessage {
            return ChatMessage(
                id = generateId(),
                role = Role.ASSISTANT,
                content = content
            )
        }

        /**
         * 创建系统消息
         * @param content 消息内容
         * @return 系统消息实例
         */
        fun system(content: String): ChatMessage {
            return ChatMessage(
                id = generateId(),
                role = Role.SYSTEM,
                content = content
            )
        }

        /**
         * 创建工具消息
         * @param content 消息内容
         * @return 工具消息实例
         */
        fun tool(content: String): ChatMessage {
            return ChatMessage(
                id = generateId(),
                role = Role.TOOL,
                content = content
            )
        }
    }
}
