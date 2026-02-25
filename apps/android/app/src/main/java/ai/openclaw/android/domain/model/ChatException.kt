package ai.openclaw.android.domain.model

/**
 * 聊天异常
 *
 * 定义聊天过程中可能发生的各种异常
 * 使用密封类确保异常类型的完整性
 */
sealed class ChatException : Exception() {

    /**
     * 无活跃会话异常
     */
    class NoActiveSession : ChatException() {
        override val message: String = "No active session"
    }

    /**
     * 无效提供商异常
     * @property providerId 无效的提供商 ID
     */
    class InvalidProvider(val providerId: String) : ChatException() {
        override val message: String = "Invalid provider: $providerId"
    }

    /**
     * 未认证异常
     * @property provider 未认证的提供商
     */
    class NotAuthenticated(val provider: Provider) : ChatException() {
        override val message: String = "Not authenticated for ${provider.displayName}"
    }

    /**
     * 认证过期异常
     * @property provider 过期的提供商
     */
    class AuthExpired(val provider: Provider) : ChatException() {
        override val message: String = "Authentication expired for ${provider.displayName}"
    }

    /**
     * 网络错误异常
     * @property originalError 原始错误
     */
    class NetworkError(val originalError: Throwable?) : ChatException() {
        override val message: String = "Network error: ${originalError?.message ?: "Unknown"}"
    }

    /**
     * 请求超时异常
     */
    class Timeout : ChatException() {
        override val message: String = "Request timeout"
    }

    /**
     * 限流异常
     * @property retryAfter 重试等待时间（秒）
     */
    class RateLimited(val retryAfter: Long? = null) : ChatException() {
        override val message: String = if (retryAfter != null) {
            "Rate limited, retry after $retryAfter seconds"
        } else {
            "Rate limited"
        }
    }

    /**
     * 服务器错误异常
     * @property statusCode HTTP 状态码
     * @property errorBody 错误响应体
     */
    class ServerError(val statusCode: Int, val errorBody: String? = null) : ChatException() {
        override val message: String = "Server error: $statusCode"
    }

    /**
     * 响应解析异常
     * @property rawResponse 原始响应
     */
    class ParseError(val rawResponse: String? = null) : ChatException() {
        override val message: String = "Failed to parse response"
    }

    /**
     * 请求中止异常
     */
    class Aborted : ChatException() {
        override val message: String = "Request aborted"
    }

    /**
     * 内容过滤异常
     * @property reason 过滤原因
     */
    class ContentFiltered(val reason: String? = null) : ChatException() {
        override val message: String = "Content filtered: ${reason ?: "Unknown reason"}"
    }

    /**
     * 模型不可用异常
     * @property model 模型名称
     */
    class ModelUnavailable(val model: String) : ChatException() {
        override val message: String = "Model unavailable: $model"
    }

    /**
     * 上下文过长异常
     * @property tokenCount 当前 token 数
     * @property maxTokens 最大 token 数
     */
    class ContextTooLong(val tokenCount: Int, val maxTokens: Int) : ChatException() {
        override val message: String = "Context too long: $tokenCount > $maxTokens"
    }

    /**
     * 工具执行异常
     * @property toolName 工具名称
     * @property toolError 工具错误
     */
    class ToolExecutionError(val toolName: String, val toolError: String) : ChatException() {
        override val message: String = "Tool '$toolName' execution failed: $toolError"
    }

    /**
     * PoW 计算异常
     * @property reason 失败原因
     */
    class PowCalculationError(val reason: String) : ChatException() {
        override val message: String = "PoW calculation failed: $reason"
    }

    /**
     * 检查是否可重试
     * @return 如果错误是临时性的则返回 true
     */
    fun isRetryable(): Boolean {
        return this is NetworkError || 
               this is Timeout || 
               this is RateLimited || 
               (this is ServerError && statusCode >= 500)
    }

    /**
     * 获取建议的重试等待时间（毫秒）
     * @return 建议等待时间，如果不建议重试则返回 null
     */
    fun getRetryDelay(): Long? {
        return when (this) {
            is RateLimited -> retryAfter?.times(1000) ?: 60000L
            is NetworkError -> 5000L
            is Timeout -> 10000L
            is ServerError -> if (statusCode >= 500) 30000L else null
            else -> null
        }
    }
}

/**
 * 认证异常
 */
sealed class AuthException : Exception() {

    /**
     * WebView 加载失败
     */
    class WebViewLoadFailed(val url: String, val errorCode: Int) : AuthException() {
        override val message: String = "WebView load failed: $url (code: $errorCode)"
    }

    /**
     * 凭证捕获失败
     */
    class CredentialCaptureFailed(val provider: String) : AuthException() {
        override val message: String = "Failed to capture credentials for $provider"
    }

    /**
     * 认证被用户取消
     */
    object UserCancelled : AuthException() {
        override val message: String = "Authentication cancelled by user"
    }

    /**
     * 认证超时
     */
    object Timeout : AuthException() {
        override val message: String = "Authentication timeout"
    }

    /**
     * 无效的凭证
     */
    object InvalidCredentials : AuthException() {
        override val message: String = "Invalid credentials"
    }
}

/**
 * 会话异常
 */
sealed class SessionException : Exception() {

    /**
     * 会话不存在
     */
    class NotFound(val sessionId: String) : SessionException() {
        override val message: String = "Session not found: $sessionId"
    }

    /**
     * 会话已满
     */
    object StorageFull : SessionException() {
        override val message: String = "Session storage is full"
    }

    /**
     * 会话加载失败
     */
    class LoadFailed(val sessionId: String, val cause: Throwable?) : SessionException() {
        override val message: String = "Failed to load session: $sessionId"
    }

    /**
     * 会话保存失败
     */
    class SaveFailed(val sessionId: String, val cause: Throwable?) : SessionException() {
        override val message: String = "Failed to save session: $sessionId"
    }
}
