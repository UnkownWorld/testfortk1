package ai.openclaw.android.ui.screens.auth

import ai.openclaw.android.domain.model.AuthState
import ai.openclaw.android.domain.model.Provider
import ai.openclaw.android.ui.viewmodel.AuthViewModel
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import timber.log.Timber

/**
 * 认证 WebView 页面
 *
 * 显示登录页面并自动捕获认证凭证
 *
 * @param provider 要认证的提供商
 * @param viewModel 认证 ViewModel
 * @param onAuthSuccess 认证成功回调
 * @param onAuthCancelled 取消认证回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthWebViewScreen(
    provider: Provider,
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onAuthCancelled: () -> Unit
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    
    // WebView 状态
    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(provider.loginUrl) }
    var pageTitle by remember { mutableStateOf("") }
    
    // 捕获的凭证
    val capturedCredentials = remember { mutableStateMapOf<String, String>() }
    
    // 监听认证状态
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                Timber.d("Authentication successful for ${provider.id}")
                onAuthSuccess()
            }
            is AuthState.Cancelled -> {
                Timber.d("Authentication cancelled for ${provider.id}")
                onAuthCancelled()
            }
            is AuthState.Error -> {
                Timber.e("Authentication error: ${(authState as AuthState.Error).message}")
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (pageTitle.isNotEmpty()) pageTitle 
                        else "登录 ${provider.displayName}"
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {
                        webView?.goBack() ?: onAuthCancelled()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 取消按钮
                    IconButton(onClick = {
                        viewModel.cancelAuthentication()
                        onAuthCancelled()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "取消")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // WebView
            AndroidView(
                factory = { context ->
                    Timber.d("Creating WebView for ${provider.id}")
                    
                    // 配置 Cookie Manager
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(null, true)
                    }
                    
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        }
                        
                        webViewClient = AuthWebViewClient(
                            provider = provider,
                            onUrlChanged = { url ->
                                currentUrl = url
                                Timber.d("URL changed: $url")
                            },
                            onTitleChanged = { title ->
                                pageTitle = title
                            },
                            onLoadStarted = {
                                isLoading = true
                            },
                            onLoadFinished = { url ->
                                isLoading = false
                                // 检查是否登录成功
                                checkLoginSuccess(
                                    url = url,
                                    provider = provider,
                                    capturedCredentials = capturedCredentials,
                                    onSuccess = { cookie, token ->
                                        Timber.d("Login successful, capturing credentials")
                                        viewModel.completeAuthentication(
                                            provider = provider,
                                            cookie = cookie,
                                            bearerToken = token
                                        )
                                    }
                                )
                            },
                            onInterceptRequest = { url, headers ->
                                // 捕获请求头中的认证信息
                                captureCredentials(
                                    url = url,
                                    headers = headers,
                                    provider = provider,
                                    capturedCredentials = capturedCredentials
                                )
                            }
                        )
                        
                        // 加载登录页面
                        loadUrl(provider.loginUrl)
                    }.also {
                        webView = it
                    }
                },
                update = { view ->
                    // 更新 WebView 配置（如果需要）
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // 加载指示器
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // 错误提示
            if (authState is AuthState.Error) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text((authState as AuthState.Error).message)
                }
            }
        }
    }
}

/**
 * 认证 WebView 客户端
 *
 * 处理页面加载和凭证捕获
 */
class AuthWebViewClient(
    private val provider: Provider,
    private val onUrlChanged: (String) -> Unit,
    private val onTitleChanged: (String) -> Unit,
    private val onLoadStarted: () -> Unit,
    private val onLoadFinished: (String) -> Unit,
    private val onInterceptRequest: (String, Map<String, String>) -> Unit
) : WebViewClient() {
    
    override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
        Timber.d("shouldOverrideUrlLoading: $url")
        onUrlChanged(url)
        return false
    }
    
    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Timber.d("onPageStarted: $url")
        onLoadStarted()
        url?.let { onUrlChanged(it) }
    }
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        Timber.d("onPageFinished: $url")
        url?.let { onLoadFinished(it) }
        view?.title?.let { onTitleChanged(it) }
    }
    
    override fun shouldInterceptRequest(
        view: WebView?,
        request: android.webkit.WebResourceRequest?
    ): android.webkit.WebResourceResponse? {
        request?.let { req ->
            val url = req.url.toString()
            val headers = req.requestHeaders ?: mapOf()
            
            // 只拦截 API 请求
            if (shouldIntercept(url)) {
                Timber.d("Intercepting request: $url")
                onInterceptRequest(url, headers)
            }
        }
        return super.shouldInterceptRequest(view, request)
    }
    
    private fun shouldIntercept(url: String): Boolean {
        return when (provider) {
            Provider.DEEPSEEK -> {
                url.contains("deepseek.com/api/") ||
                url.contains("chat.deepseek.com")
            }
            Provider.CLAUDE -> {
                url.contains("claude.ai/api/") ||
                url.contains("claude.ai")
            }
            Provider.DOUBAO -> {
                url.contains("doubao.com/api/") ||
                url.contains("doubao.com")
            }
        }
    }
}

/**
 * 检查是否登录成功
 */
private fun checkLoginSuccess(
    url: String,
    provider: Provider,
    capturedCredentials: Map<String, String>,
    onSuccess: (cookie: String, bearerToken: String?) -> Unit
) {
    val isSuccess = when (provider) {
        Provider.DEEPSEEK -> {
            url.contains("chat.deepseek.com") && 
                !url.contains("login") && 
                !url.contains("auth")
        }
        Provider.CLAUDE -> {
            url.contains("claude.ai") && 
                !url.contains("login") && 
                !url.contains("auth")
        }
        Provider.DOUBAO -> {
            url.contains("doubao.com") && 
                url.contains("chat")
        }
    }
    
    if (isSuccess) {
        // 获取 Cookie
        val cookie = CookieManager.getInstance().getCookie(url) ?: ""
        
        // 获取捕获的 Token
        val bearerToken = capturedCredentials["bearerToken"]
        
        if (cookie.isNotEmpty()) {
            Timber.d("Login success detected, cookie length: ${cookie.length}")
            onSuccess(cookie, bearerToken)
        }
    }
}

/**
 * 捕获凭证
 */
private fun captureCredentials(
    url: String,
    headers: Map<String, String>,
    provider: Provider,
    capturedCredentials: MutableMap<String, String>
) {
    // 提取 Authorization
    headers["Authorization"]?.let { auth ->
        if (auth.startsWith("Bearer ")) {
            val token = auth.removePrefix("Bearer ")
            capturedCredentials["bearerToken"] = token
            Timber.d("Captured bearer token: ${token.take(20)}...")
        }
    }
    
    // 提取 Cookie
    headers["Cookie"]?.let { cookie ->
        if (cookie.isNotEmpty()) {
            capturedCredentials["cookie"] = cookie
            Timber.d("Captured cookie: ${cookie.take(50)}...")
        }
    }
    
    // 提取 User-Agent
    headers["User-Agent"]?.let { ua ->
        capturedCredentials["userAgent"] = ua
    }
}
