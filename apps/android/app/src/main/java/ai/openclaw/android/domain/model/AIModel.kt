package ai.openclaw.android.domain.model

/**
 * AI 模型定义
 *
 * 定义各个提供商支持的模型及其特性
 */
enum class AIModel(
    val id: String,
    val displayName: String,
    val provider: String,
    val features: Set<ModelFeature>,
    val maxTokens: Int = 4096,
    val description: String = ""
) {
    // DeepSeek 模型
    DEEPSEEK_CHAT(
        id = "deepseek-chat",
        displayName = "DeepSeek Chat",
        provider = "deepseek",
        features = setOf(ModelFeature.STREAMING, ModelFeature.MULTI_TURN),
        maxTokens = 64000,
        description = "通用对话模型，适合日常对话"
    ),
    DEEPSEEK_REASONER(
        id = "deepseek-reasoner",
        displayName = "DeepSeek R1",
        provider = "deepseek",
        features = setOf(ModelFeature.STREAMING, ModelFeature.THINKING, ModelFeature.MULTI_TURN),
        maxTokens = 64000,
        description = "深度思考模型，适合复杂推理"
    ),
    DEEPSEEK_SEARCH(
        id = "deepseek-search",
        displayName = "DeepSeek 搜索",
        provider = "deepseek",
        features = setOf(ModelFeature.STREAMING, ModelFeature.WEB_SEARCH, ModelFeature.MULTI_TURN),
        maxTokens = 64000,
        description = "联网搜索模型，可获取最新信息"
    ),
    DEEPSEEK_R1_SEARCH(
        id = "deepseek-r1-search",
        displayName = "DeepSeek R1 搜索",
        provider = "deepseek",
        features = setOf(ModelFeature.STREAMING, ModelFeature.THINKING, ModelFeature.WEB_SEARCH, ModelFeature.MULTI_TURN),
        maxTokens = 64000,
        description = "深度思考 + 联网搜索"
    ),

    // Claude 模型
    CLAUDE_3_5_SONNET(
        id = "claude-3-5-sonnet-20241022",
        displayName = "Claude 3.5 Sonnet",
        provider = "claude",
        features = setOf(ModelFeature.STREAMING, ModelFeature.MULTI_TURN, ModelFeature.VISION),
        maxTokens = 200000,
        description = "最新 Claude 模型，性能强大"
    ),
    CLAUDE_3_OPUS(
        id = "claude-3-opus-20240229",
        displayName = "Claude 3 Opus",
        provider = "claude",
        features = setOf(ModelFeature.STREAMING, ModelFeature.MULTI_TURN, ModelFeature.VISION),
        maxTokens = 200000,
        description = "Claude 最强模型"
    ),
    CLAUDE_3_HAIKU(
        id = "claude-3-haiku-20240307",
        displayName = "Claude 3 Haiku",
        provider = "claude",
        features = setOf(ModelFeature.STREAMING, ModelFeature.MULTI_TURN),
        maxTokens = 200000,
        description = "快速响应，适合简单任务"
    ),

    // 豆包模型
    DOUBAO_PRO(
        id = "doubao-pro-32k",
        displayName = "豆包 Pro",
        provider = "doubao",
        features = setOf(ModelFeature.STREAMING, ModelFeature.MULTI_TURN),
        maxTokens = 32000,
        description = "豆包主力模型"
    );

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

    companion object {
        /**
         * 根据提供商获取模型列表
         */
        fun getModelsByProvider(providerId: String): List<AIModel> {
            return values().filter { it.provider == providerId }
        }

        /**
         * 根据 ID 获取模型
         */
        fun fromId(id: String): AIModel? {
            return values().find { it.id == id }
        }

        /**
         * 获取默认模型
         */
        fun getDefaultModel(providerId: String): AIModel {
            return when (providerId) {
                "deepseek" -> DEEPSEEK_CHAT
                "claude" -> CLAUDE_3_5_SONNET
                "doubao" -> DOUBAO_PRO
                else -> DEEPSEEK_CHAT
            }
        }
    }
}

/**
 * 模型功能特性
 */
enum class ModelFeature {
    /** 流式响应 */
    STREAMING,
    /** 多轮对话 */
    MULTI_TURN,
    /** 深度思考 */
    THINKING,
    /** 联网搜索 */
    WEB_SEARCH,
    /** 视觉理解 */
    VISION,
    /** 工具调用 */
    TOOL_CALLING,
    /** 代码执行 */
    CODE_EXECUTION
}

/**
 * 模型配置
 */
data class ModelConfig(
    val model: AIModel,
    val temperature: Float = 0.7f,
    val maxTokens: Int = model.maxTokens,
    val enableThinking: Boolean = false,
    val enableWebSearch: Boolean = false,
    val systemPrompt: String? = null
) {
    /**
     * 获取实际使用的模型 ID
     */
    fun getActualModelId(): String {
        return when {
            enableThinking && enableWebSearch && model.provider == "deepseek" -> "deepseek-r1-search"
            enableThinking && model.provider == "deepseek" -> "deepseek-reasoner"
            enableWebSearch && model.provider == "deepseek" -> "deepseek-search"
            else -> model.id
        }
    }

    companion object {
        /**
         * 创建默认配置
         */
        fun default(providerId: String): ModelConfig {
            val model = AIModel.getDefaultModel(providerId)
            return ModelConfig(model = model)
        }
    }
}
