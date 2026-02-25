package ai.openclaw.android.domain.model

/**
 * 提供商配置
 *
 * 动态配置，支持远程更新
 */
data class ProviderConfig(
    val id: String,
    val displayName: String,
    val loginUrl: String,
    val apiBaseUrl: String,
    val enabled: Boolean = true,
    val models: List<RemoteModelConfig> = emptyList()
)

/**
 * 远程模型配置
 *
 * 动态配置，支持远程更新
 */
data class RemoteModelConfig(
    val id: String,
    val displayName: String,
    val features: Set<ModelFeature>,
    val maxTokens: Int,
    val description: String,
    val enabled: Boolean = true
) {
    /**
     * 检查是否支持某功能
     */
    fun supports(feature: ModelFeature): Boolean = features.contains(feature)

    /**
     * 是否支持深度思考
     */
    fun supportsThinking(): Boolean = supports(ModelFeature.THINKING)

    /**
     * 是否支持联网搜索
     */
    fun supportsWebSearch(): Boolean = supports(ModelFeature.WEB_SEARCH)

    /**
     * 是否支持视觉
     */
    fun supportsVision(): Boolean = supports(ModelFeature.VISION)
}

/**
 * 模型功能特性
 */
enum class ModelFeature {
    /** 流式响应 */
    STREAMING,
    /** 多轮对话 */
    MULTI_TURN,
    /** 深度思考/推理 */
    THINKING,
    /** 联网搜索 */
    WEB_SEARCH,
    /** 视觉理解/图像识别 */
    VISION,
    /** 工具调用 */
    TOOL_CALLING,
    /** 代码执行 */
    CODE_EXECUTION,
    /** 文件处理 */
    FILE_PROCESSING,
    /** 音频处理 */
    AUDIO_PROCESSING,
    /** 视频处理 */
    VIDEO_PROCESSING
}

/**
 * 用户模型偏好设置
 */
data class UserModelPreferences(
    val providerId: String,
    val modelId: String,
    val temperature: Float = 0.7f,
    val maxTokens: Int? = null,
    val enableThinking: Boolean = false,
    val enableWebSearch: Boolean = false,
    val systemPrompt: String? = null
) {
    /**
     * 获取实际使用的模型 ID
     */
    fun getActualModelId(): String {
        // 根据功能自动选择模型变体
        return modelId
    }

    companion object {
        /**
         * 创建默认偏好
         */
        fun default(providerId: String, modelId: String): UserModelPreferences {
            return UserModelPreferences(
                providerId = providerId,
                modelId = modelId
            )
        }
    }
}
