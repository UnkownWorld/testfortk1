package ai.openclaw.android.domain.model

import kotlinx.serialization.Serializable

/**
 * 认证配置
 *
 * 存储提供商的认证信息，包括 Cookie、Token 等
 *
 * @property provider 提供商 ID
 * @property cookie Cookie 字符串
 * @property bearerToken Bearer Token
 * @property sessionKey 会话密钥
 * @property userAgent User Agent
 * @property deviceId 设备 ID
 * @property capturedAt 捕获时间
 * @property expiresAt 过期时间（null 表示不过期）
 */
@Serializable
data class AuthConfig(
    val provider: String,
    val cookie: String,
    val bearerToken: String? = null,
    val sessionKey: String? = null,
    val userAgent: String,
    val deviceId: String? = null,
    val capturedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
) {
    /**
     * 检查是否已过期
     * @return 如果有过期时间且已过期则返回 true
     */
    fun isExpired(): Boolean {
        return expiresAt?.let { System.currentTimeMillis() > it } ?: false
    }

    /**
     * 检查是否即将过期（1小时内）
     * @return 如果有过期时间且在1小时内过期则返回 true
     */
    fun isExpiringSoon(): Boolean {
        return expiresAt?.let {
            System.currentTimeMillis() > (it - 3600_000)
        } ?: false
    }

    /**
     * 检查是否有效
     * @return Cookie 非空且未过期则返回 true
     */
    fun isValid(): Boolean {
        return cookie.isNotEmpty() && !isExpired()
    }

    /**
     * 获取剩余有效时间（毫秒）
     * @return 剩余时间，如果不过期则返回 null
     */
    fun remainingTime(): Long? {
        return expiresAt?.let { maxOf(0L, it - System.currentTimeMillis()) }
    }

    /**
     * 创建带有新过期时间的配置
     * @param expiresInMillis 过期时间（毫秒后）
     * @return 新的配置实例
     */
    fun withExpiration(expiresInMillis: Long): AuthConfig {
        return copy(expiresAt = System.currentTimeMillis() + expiresInMillis)
    }

    companion object {
        /**
         * 默认过期时间：7天
         */
        const val DEFAULT_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L

        /**
         * 创建空的认证配置
         */
        fun empty(provider: String): AuthConfig {
            return AuthConfig(
                provider = provider,
                cookie = "",
                userAgent = ""
            )
        }
    }
}

/**
 * 认证状态
 *
 * 表示认证流程中的各种状态
 */
sealed class AuthState {
    /**
     * 未认证状态
     */
    object NotAuthenticated : AuthState()

    /**
     * 认证中状态
     * @property provider 正在认证的提供商
     */
    data class Authenticating(val provider: Provider) : AuthState()

    /**
     * 已认证状态
     * @property config 认证配置
     */
    data class Authenticated(val config: AuthConfig) : AuthState()

    /**
     * 认证错误状态
     * @property code 错误代码
     * @property message 错误信息
     */
    data class Error(val code: String, val message: String) : AuthState()

    /**
     * 已取消状态
     */
    object Cancelled : AuthState()

    /**
     * 检查是否已认证
     */
    fun isAuthenticated(): Boolean = this is Authenticated

    /**
     * 检查是否可以重试
     */
    fun canRetry(): Boolean = this is Error || this is Cancelled
}
