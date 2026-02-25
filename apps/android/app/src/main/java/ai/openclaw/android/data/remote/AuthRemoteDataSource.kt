package ai.openclaw.android.data.remote

import ai.openclaw.android.domain.model.AuthConfig
import ai.openclaw.android.domain.model.AuthState
import ai.openclaw.android.domain.model.Provider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证远程数据源
 *
 * 处理 WebView 认证和凭证捕获
 */
@Singleton
class AuthRemoteDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    // WebView 认证事件
    private val _authEvent = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val authEvent: SharedFlow<AuthEvent> = _authEvent.asSharedFlow()

    // 认证结果
    private var authResult: CompletableDeferred<AuthConfig?>? = null

    // 捕获的凭证
    private val capturedCredentials = mutableMapOf<String, MutableCredentialData>()

    /**
     * 认证
     *
     * @param provider 提供商
     * @return 认证状态流
     */
    fun authenticate(provider: Provider): Flow<AuthState> = callbackFlow {
        // 重置状态
        authResult = CompletableDeferred()
        capturedCredentials[provider.id] = MutableCredentialData()

        // 发送 WebView 显示事件
        _authEvent.tryEmit(AuthEvent.ShowWebView(
            url = provider.loginUrl,
            provider = provider
        ))

        // 发送认证中状态
        send(AuthState.Authenticating(provider))

        // 等待认证结果
        val config = authResult?.await()

        if (config != null) {
            send(AuthState.Authenticated(config))
        } else {
            send(AuthState.Cancelled)
        }

        awaitClose {
            authResult = null
            capturedCredentials.remove(provider.id)
        }
    }

    /**
     * 刷新认证
     *
     * @param provider 提供商
     * @param currentConfig 当前配置
     * @return 新的认证配置，如果刷新失败则返回 null
     */
    suspend fun refresh(provider: Provider, currentConfig: AuthConfig): AuthConfig? {
        return try {
            // 尝试使用当前配置访问 API
            val request = Request.Builder()
                .url("${provider.apiBaseUrl}/user")
                .header("Cookie", currentConfig.cookie)
                .header("Authorization", "Bearer ${currentConfig.bearerToken}")
                .header("User-Agent", currentConfig.userAgent)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (response.isSuccessful) {
                // 认证仍然有效，更新过期时间
                currentConfig.withExpiration(AuthConfig.DEFAULT_EXPIRATION_MS)
            } else {
                // 需要重新认证
                null
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to refresh auth for ${provider.id}")
            null
        }
    }

    /**
     * 处理 WebView 页面加载完成
     *
     * @param url 当前 URL
     * @param cookies Cookie 字符串
     */
    fun onPageFinished(url: String, cookies: String) {
        Timber.d("WebView page finished: $url")

        // 检查是否登录成功
        if (isLoginSuccess(url)) {
            // 提取凭证
            val provider = detectProvider(url)
            val credentials = capturedCredentials[provider]

            if (credentials != null && credentials.hasRequiredData()) {
                val config = AuthConfig(
                    provider = provider,
                    cookie = credentials.cookie.ifEmpty { cookies },
                    bearerToken = credentials.bearerToken,
                    sessionKey = credentials.sessionKey,
                    userAgent = credentials.userAgent,
                    capturedAt = System.currentTimeMillis()
                ).withExpiration(AuthConfig.DEFAULT_EXPIRATION_MS)

                authResult?.complete(config)

                // 发送隐藏 WebView 事件
                _authEvent.tryEmit(AuthEvent.HideWebView)
            }
        }
    }

    /**
     * 处理网络请求拦截
     *
     * @param url 请求 URL
     * @param headers 请求头
     */
    fun onInterceptRequest(url: String, headers: Map<String, String>) {
        val provider = detectProvider(url) ?: return
        val credentials = capturedCredentials.getOrPut(provider) { MutableCredentialData() }

        // 提取 Cookie
        headers["Cookie"]?.let { cookie ->
            if (cookie.isNotEmpty()) {
                credentials.cookie = cookie
            }
        }

        // 提取 Authorization
        headers["Authorization"]?.let { auth ->
            if (auth.startsWith("Bearer ")) {
                credentials.bearerToken = auth.removePrefix("Bearer ")
            }
        }

        // 提取 User-Agent
        headers["User-Agent"]?.let { ua ->
            if (ua.isNotEmpty()) {
                credentials.userAgent = ua
            }
        }

        // 从 URL 提取 session_key（某些提供商）
        extractSessionKey(url)?.let { key ->
            credentials.sessionKey = key
        }
    }

    /**
     * 处理认证取消
     */
    fun onAuthCancelled() {
        authResult?.complete(null)
        _authEvent.tryEmit(AuthEvent.HideWebView)
    }

    /**
     * 处理 WebView 错误
     *
     * @param errorCode 错误码
     * @param description 错误描述
     */
    fun onWebViewError(errorCode: Int, description: String) {
        Timber.e("WebView error: $errorCode - $description")
        authResult?.complete(null)
        _authEvent.tryEmit(AuthEvent.HideWebView)
    }

    /**
     * 检查是否登录成功
     */
    private fun isLoginSuccess(url: String): Boolean {
        return when {
            url.contains("chat.deepseek.com") && 
                !url.contains("login") && 
                !url.contains("auth") -> true

            url.contains("claude.ai") && 
                !url.contains("login") && 
                !url.contains("auth") -> true

            url.contains("doubao.com") && 
                url.contains("chat") -> true

            else -> false
        }
    }

    /**
     * 检测提供商
     */
    private fun detectProvider(url: String): String? {
        return when {
            url.contains("deepseek.com") -> "deepseek"
            url.contains("claude.ai") -> "claude"
            url.contains("doubao.com") -> "doubao"
            else -> null
        }
    }

    /**
     * 从 URL 提取 session_key
     */
    private fun extractSessionKey(url: String): String? {
        // 从 URL 参数中提取 session_key
        val regex = Regex("[?&]session_key=([^&]+)")
        return regex.find(url)?.groupValues?.getOrNull(1)
    }
}

/**
 * 认证事件
 */
sealed class AuthEvent {
    data class ShowWebView(val url: String, val provider: Provider) : AuthEvent()
    object HideWebView : AuthEvent()
    data class Error(val code: Int, val message: String) : AuthEvent()
}

/**
 * 可变凭证数据
 */
private data class MutableCredentialData(
    var cookie: String = "",
    var bearerToken: String? = null,
    var sessionKey: String? = null,
    var userAgent: String = ""
) {
    fun hasRequiredData(): Boolean {
        return cookie.isNotEmpty()
    }
}
