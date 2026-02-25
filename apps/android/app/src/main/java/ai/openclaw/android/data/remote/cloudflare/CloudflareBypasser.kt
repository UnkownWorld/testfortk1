package ai.openclaw.android.data.remote.cloudflare

import android.webkit.CookieManager
import timber.log.Timber

/**
 * Cloudflare 绕过工具
 *
 * 提供检测和处理 Cloudflare 保护的功能
 */
object CloudflareBypasser {

    /**
     * Cloudflare 相关的 Cookie 名称
     */
    val CLOUDFLARE_COOKIES = listOf(
        "cf_clearance",
        "cf_bm",
        "__cf_bm"
    )

    /**
     * 检查是否遇到 Cloudflare 保护
     */
    fun isCloudflareChallenge(html: String): Boolean {
        return html.contains("Just a moment") ||
               html.contains("Checking your browser") ||
               html.contains("cf-browser-verification") ||
               html.contains("challenge-platform") ||
               html.contains("ray id:") ||
               html.contains("__cfduid")
    }

    /**
     * 检查是否有有效的 Cloudflare Cookie
     */
    fun hasValidCloudflareCookie(cookies: String): Boolean {
        return CLOUDFLARE_COOKIES.any { cookies.contains(it) }
    }

    /**
     * 从响应中提取 Cookie
     */
    fun extractCookies(cookieHeader: String?): Map<String, String> {
        if (cookieHeader.isNullOrBlank()) return emptyMap()
        
        return cookieHeader.split(";")
            .map { it.trim() }
            .filter { it.contains("=") }
            .associate {
                val parts = it.split("=", limit = 2)
                parts[0] to parts.getOrElse(1) { "" }
            }
    }

    /**
     * 获取 WebView 的 Cookie
     */
    fun getWebViewCookies(url: String): String {
        return try {
            CookieManager.getInstance().getCookie(url) ?: ""
        } catch (e: Exception) {
            Timber.e(e, "Failed to get WebView cookies")
            ""
        }
    }

    /**
     * 清除 WebView Cookie
     */
    fun clearWebViewCookies() {
        try {
            CookieManager.getInstance().apply {
                removeAllCookies(null)
                flush()
            }
            Timber.d("Cleared WebView cookies")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear WebView cookies")
        }
    }

    /**
     * 刷新 Cookie
     */
    fun flushCookies() {
        try {
            CookieManager.getInstance().flush()
        } catch (e: Exception) {
            Timber.e(e, "Failed to flush cookies")
        }
    }
}

/**
 * Cloudflare 异常
 */
class CloudflareException(
    val code: String,
    override val message: String
) : Exception(message) {
    
    companion object {
        const val CODE_TIMEOUT = "CF_TIMEOUT"
        const val CODE_BLOCKED = "CF_BLOCKED"
        const val CODE_CHALLENGE = "CF_CHALLENGE"
        const val CODE_RATE_LIMITED = "CF_RATE_LIMITED"
    }
}
