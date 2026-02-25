package ai.openclaw.android.ui.viewmodel

import ai.openclaw.android.domain.model.AuthConfig
import ai.openclaw.android.domain.model.AuthState
import ai.openclaw.android.domain.model.Provider
import ai.openclaw.android.domain.usecase.AuthUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 认证 ViewModel
 *
 * 管理认证状态和操作
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authUseCase: AuthUseCase
) : ViewModel() {
    
    // 当前认证状态
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // 当前正在认证的提供商
    private val _currentProvider = MutableStateFlow<Provider?>(null)
    val currentProvider: StateFlow<Provider?> = _currentProvider.asStateFlow()
    
    // 所有提供商的认证状态
    private val _providerAuthStates = MutableStateFlow<Map<String, AuthState>>(emptyMap())
    val providerAuthStates: StateFlow<Map<String, AuthState>> = _providerAuthStates.asStateFlow()
    
    init {
        // 初始化时加载所有提供商的认证状态
        loadAllAuthStates()
    }
    
    /**
     * 加载所有提供商的认证状态
     */
    private fun loadAllAuthStates() {
        viewModelScope.launch {
            Provider.values().forEach { provider ->
                val isAuth = authUseCase.isAuthenticated(provider)
                val state = if (isAuth) {
                    AuthState.Authenticated(
                        authUseCase.getAuthConfig(provider) ?: AuthConfig.empty(provider.id)
                    )
                } else {
                    AuthState.NotAuthenticated
                }
                
                _providerAuthStates.update { map ->
                    map + (provider.id to state)
                }
            }
        }
    }
    
    /**
     * 开始认证流程
     */
    fun startAuthentication(provider: Provider) {
        viewModelScope.launch {
            _currentProvider.value = provider
            _authState.value = AuthState.Authenticating(provider)
            
            _providerAuthStates.update { map ->
                map + (provider.id to AuthState.Authenticating(provider))
            }
            
            Timber.d("Starting authentication for ${provider.id}")
        }
    }
    
    /**
     * 完成认证
     *
     * 在 WebView 捕获到凭证后调用
     */
    fun completeAuthentication(
        provider: Provider,
        cookie: String,
        bearerToken: String? = null,
        userAgent: String = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"
    ) {
        viewModelScope.launch {
            try {
                Timber.d("Completing authentication for ${provider.id}")
                Timber.d("Cookie length: ${cookie.length}")
                Timber.d("Bearer token: ${bearerToken?.take(20)}...")
                
                // 创建认证配置
                val config = AuthConfig(
                    provider = provider.id,
                    cookie = cookie,
                    bearerToken = bearerToken,
                    userAgent = userAgent,
                    capturedAt = System.currentTimeMillis()
                ).withExpiration(AuthConfig.DEFAULT_EXPIRATION_MS)
                
                // 保存认证配置
                authUseCase.saveAuthConfig(config)
                
                // 更新状态
                _authState.value = AuthState.Authenticated(config)
                _providerAuthStates.update { map ->
                    map + (provider.id to AuthState.Authenticated(config))
                }
                
                Timber.d("Authentication completed successfully for ${provider.id}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to complete authentication")
                _authState.value = AuthState.Error(
                    code = "SAVE_ERROR",
                    message = e.message ?: "Failed to save credentials"
                )
                _providerAuthStates.update { map ->
                    map + (provider.id to AuthState.Error("SAVE_ERROR", e.message ?: "Unknown error"))
                }
            }
        }
    }
    
    /**
     * 取消认证
     */
    fun cancelAuthentication() {
        viewModelScope.launch {
            val provider = _currentProvider.value
            Timber.d("Cancelling authentication for ${provider?.id}")
            
            _authState.value = AuthState.Cancelled
            _currentProvider.value = null
            
            provider?.let {
                _providerAuthStates.update { map ->
                    map + (it.id to AuthState.NotAuthenticated)
                }
            }
        }
    }
    
    /**
     * 登出
     */
    fun logout(provider: Provider) {
        viewModelScope.launch {
            try {
                authUseCase.logout(provider)
                
                _providerAuthStates.update { map ->
                    map + (provider.id to AuthState.NotAuthenticated)
                }
                
                Timber.d("Logged out from ${provider.id}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to logout from ${provider.id}")
            }
        }
    }
    
    /**
     * 刷新认证
     */
    fun refreshAuthentication(provider: Provider) {
        viewModelScope.launch {
            _providerAuthStates.update { map ->
                map + (provider.id to AuthState.Authenticating(provider))
            }
            
            val result = authUseCase.refresh(provider)
            
            result.fold(
                onSuccess = { success ->
                    if (success) {
                        val config = authUseCase.getAuthConfig(provider)
                        _providerAuthStates.update { map ->
                            map + (provider.id to AuthState.Authenticated(
                                config ?: AuthConfig.empty(provider.id)
                            ))
                        }
                    } else {
                        _providerAuthStates.update { map ->
                            map + (provider.id to AuthState.NotAuthenticated)
                        }
                    }
                },
                onFailure = { error ->
                    _providerAuthStates.update { map ->
                        map + (provider.id to AuthState.Error(
                            code = "REFRESH_ERROR",
                            message = error.message ?: "Refresh failed"
                        ))
                    }
                }
            )
        }
    }
    
    /**
     * 检查是否已认证
     */
    fun isAuthenticated(provider: Provider): Boolean {
        return _providerAuthStates.value[provider.id] is AuthState.Authenticated
    }
    
    /**
     * 获取已认证的提供商列表
     */
    fun getAuthenticatedProviders(): List<Provider> {
        return Provider.values().filter { isAuthenticated(it) }
    }
    
    /**
     * 重置认证状态
     */
    fun resetState() {
        _authState.value = AuthState.NotAuthenticated
        _currentProvider.value = null
    }
}
