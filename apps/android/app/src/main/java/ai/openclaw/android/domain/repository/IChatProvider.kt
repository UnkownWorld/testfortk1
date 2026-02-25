package ai.openclaw.android.domain.repository

import ai.openclaw.android.domain.model.AuthConfig
import ai.openclaw.android.domain.model.ChatChunk
import ai.openclaw.android.domain.model.ChatMessage
import ai.openclaw.android.domain.model.Provider
import kotlinx.coroutines.flow.Flow

/**
 * 聊天提供者接口
 *
 * 定义聊天相关的操作，包括发送消息、中止请求和配置验证
 */
interface IChatProvider {

    /**
     * 提供商类型
     */
    val provider: Provider

    /**
     * 发送聊天消息
     *
     * 使用流式响应返回聊天结果
     *
     * @param messages 消息历史
     * @param config 认证配置
     * @param sessionId 会话 ID（可选，用于保持上下文）
     * @param options 额外选项（可选）
     * @return 响应块流，包含文本、工具调用、错误等
     */
    fun chat(
        messages: List<ChatMessage>,
        config: AuthConfig,
        sessionId: String? = null,
        options: ChatOptions? = null
    ): Flow<ChatChunk>

    /**
     * 中止当前请求
     *
     * 取消正在进行的聊天请求
     */
    fun abort()

    /**
     * 检查认证配置是否有效
     *
     * 通过发送测试请求验证凭证
     *
     * @param config 认证配置
     * @return 验证结果
     */
    suspend fun validateConfig(config: AuthConfig): Result<Boolean>

    /**
     * 获取支持的模型列表
     *
     * @param config 认证配置
     * @return 模型列表
     */
    suspend fun getModels(config: AuthConfig): Result<List<String>>

    /**
     * 检查是否有正在进行的请求
     *
     * @return 如果有正在进行的请求则返回 true
     */
    fun isBusy(): Boolean
}

/**
 * 聊天选项
 *
 * 包含聊天请求的额外配置
 *
 * @property model 模型名称
 * @property temperature 温度参数
 * @property maxTokens 最大 token 数
 * @property thinking 是否启用思考模式
 * @property stream 是否使用流式响应
 * @property tools 工具列表
 */
data class ChatOptions(
    val model: String? = null,
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val thinking: Boolean? = null,
    val stream: Boolean = true,
    val tools: List<ToolDefinition>? = null
)

/**
 * 工具定义
 *
 * @property name 工具名称
 * @property description 工具描述
 * @property parameters 参数 Schema
 */
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
)

/**
 * 聊天提供者工厂接口
 *
 * 用于创建和管理聊天提供者实例
 */
interface IChatProviderFactory {

    /**
     * 获取聊天提供者
     *
     * @param provider 提供商
     * @return 聊天提供者实例
     * @throws IllegalArgumentException 如果提供商未注册
     */
    fun getProvider(provider: Provider): IChatProvider

    /**
     * 注册聊天提供者
     *
     * @param provider 提供者实例
     */
    fun registerProvider(provider: IChatProvider)

    /**
     * 检查提供商是否已注册
     *
     * @param provider 提供商
     * @return 如果已注册则返回 true
     */
    fun hasProvider(provider: Provider): Boolean

    /**
     * 获取所有已注册的提供商
     *
     * @return 提供商列表
     */
    fun getRegisteredProviders(): List<Provider>
}
