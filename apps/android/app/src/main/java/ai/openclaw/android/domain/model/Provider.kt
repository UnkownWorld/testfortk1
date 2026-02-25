package ai.openclaw.android.domain.model

/**
 * AI 提供商枚举
 *
 * 定义支持的 AI 提供商及其配置
 * 更新时间：2025年2月
 *
 * @property id 提供商唯一标识
 * @property displayName 显示名称
 * @property loginUrl 登录页面 URL
 * @property apiBaseUrl API 基础 URL
 */
enum class Provider(
    val id: String,
    val displayName: String,
    val loginUrl: String,
    val apiBaseUrl: String
) {
    DEEPSEEK(
        id = "deepseek",
        displayName = "DeepSeek",
        loginUrl = "https://chat.deepseek.com",
        apiBaseUrl = "https://chat.deepseek.com/api/v0"
    ),
    CLAUDE(
        id = "claude",
        displayName = "Claude",
        loginUrl = "https://claude.ai",
        apiBaseUrl = "https://claude.ai/api"
    ),
    DOUBAO(
        id = "doubao",
        displayName = "豆包",
        loginUrl = "https://www.doubao.com",
        apiBaseUrl = "https://www.doubao.com/api"
    ),
    KIMI(
        id = "kimi",
        displayName = "Kimi",
        loginUrl = "https://kimi.moonshot.cn",
        apiBaseUrl = "https://kimi.moonshot.cn/api"
    ),
    ZHIPU(
        id = "zhipu",
        displayName = "智谱清言",
        loginUrl = "https://chatglm.cn",
        apiBaseUrl = "https://chatglm.cn/api"
    );

    companion object {
        /**
         * 根据 ID 获取提供商
         * @param id 提供商 ID
         * @return 提供商枚举值，如果不存在则返回 null
         */
        fun fromId(id: String): Provider? = values().find { it.id == id }
    }
}
