package ai.openclaw.android.domain.model

/**
 * 聊天响应块
 *
 * 流式响应中的单个数据块，使用密封类确保类型安全
 * 每种响应类型都有独立的数据结构
 */
sealed class ChatChunk {

    abstract val id: String
    abstract val timestamp: Long

    /**
     * 文本内容块
     *
     * @property text 文本内容
     */
    data class Text(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        val text: String
    ) : ChatChunk()

    /**
     * 思考内容块（DeepSeek 推理模式）
     *
     * @property text 思考过程文本
     */
    data class Thinking(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        val text: String
    ) : ChatChunk()

    /**
     * 工具调用开始
     *
     * @property toolName 工具名称
     */
    data class ToolCallStart(
        override val id: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val toolName: String
    ) : ChatChunk()

    /**
     * 工具调用参数增量
     *
     * @property toolCallId 工具调用 ID
     * @property toolName 工具名称
     * @property arguments 参数 JSON 片段
     */
    data class ToolCallDelta(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        val toolCallId: String,
        val toolName: String,
        val arguments: String
    ) : ChatChunk()

    /**
     * 工具调用结果
     *
     * @property toolCallId 工具调用 ID
     * @property toolName 工具名称
     * @property result 执行结果
     */
    data class ToolCallResult(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        val toolCallId: String,
        val toolName: String,
        val result: String
    ) : ChatChunk()

    /**
     * 工具调用错误
     *
     * @property toolCallId 工具调用 ID
     * @property toolName 工具名称
     * @property error 错误信息
     */
    data class ToolCallError(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        val toolCallId: String,
        val toolName: String,
        val error: String
    ) : ChatChunk()

    /**
     * 完成块
     */
    data class Done(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatChunk()

    /**
     * 错误块
     *
     * @property code 错误代码
     * @property message 错误信息
     * @property recoverable 是否可恢复
     */
    data class Error(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        val code: String,
        val message: String,
        val recoverable: Boolean = false
    ) : ChatChunk()

    /**
     * 中止块
     */
    data class Aborted(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis()
    ) : ChatChunk()

    /**
     * 使用量信息块
     *
     * @property promptTokens 提示词 token 数
     * @property completionTokens 完成 token 数
     * @property totalTokens 总 token 数
     */
    data class Usage(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        val promptTokens: Int,
        val completionTokens: Int,
        val totalTokens: Int
    ) : ChatChunk()

    companion object {
        private fun generateId(): String = java.util.UUID.randomUUID().toString()

        fun text(text: String) = Text(text = text)
        fun thinking(text: String) = Thinking(text = text)
        fun toolCallStart(id: String, name: String) = ToolCallStart(id = id, toolName = name)
        fun toolCallDelta(toolCallId: String, name: String, args: String) = 
            ToolCallDelta(toolCallId = toolCallId, toolName = name, arguments = args)
        fun toolCallResult(toolCallId: String, name: String, result: String) = 
            ToolCallResult(toolCallId = toolCallId, toolName = name, result = result)
        fun toolCallError(toolCallId: String, name: String, error: String) = 
            ToolCallError(toolCallId = toolCallId, toolName = name, error = error)
        fun done() = Done()
        fun error(code: String, message: String, recoverable: Boolean = false) =
            Error(code = code, message = message, recoverable = recoverable)
        fun aborted() = Aborted()
        fun usage(prompt: Int, completion: Int) = 
            Usage(promptTokens = prompt, completionTokens = completion, totalTokens = prompt + completion)
    }
}
