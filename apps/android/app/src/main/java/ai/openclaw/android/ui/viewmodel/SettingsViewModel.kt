package ai.openclaw.android.ui.viewmodel

import ai.openclaw.android.domain.model.AIModel
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
 * 设置 ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authUseCase: AuthUseCase
) : ViewModel() {
    
    // 当前提供商
    private val _currentProvider = MutableStateFlow<Provider?>(null)
    val currentProvider: StateFlow<Provider?> = _currentProvider.asStateFlow()
    
    // 当前模型
    private val _currentModel = MutableStateFlow<AIModel?>(null)
    val currentModel: StateFlow<AIModel?> = _currentModel.asStateFlow()
    
    // 温度
    private val _temperature = MutableStateFlow(0.7f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()
    
    // 启用深度思考
    private val _enableThinking = MutableStateFlow(false)
    val enableThinking: StateFlow<Boolean> = _enableThinking.asStateFlow()
    
    // 启用联网搜索
    private val _enableWebSearch = MutableStateFlow(false)
    val enableWebSearch: StateFlow<Boolean> = _enableWebSearch.asStateFlow()
    
    // 启用流式输出
    private val _enableStreaming = MutableStateFlow(true)
    val enableStreaming: StateFlow<Boolean> = _enableStreaming.asStateFlow()
    
    // 提供商认证状态
    private val _providerAuthStates = MutableStateFlow<Map<String, AuthState>>(emptyMap())
    val providerAuthStates: StateFlow<Map<String, AuthState>> = _providerAuthStates.asStateFlow()
    
    init {
        loadAuthStates()
    }
    
    /**
     * 加载认证状态
     */
    private fun loadAuthStates() {
        viewModelScope.launch {
            Provider.values().forEach { provider ->
                val isAuth = authUseCase.isAuthenticated(provider)
                val state = if (isAuth) {
                    AuthState.Authenticated(
                        authUseCase.getAuthConfig(provider) 
                            ?: ai.openclaw.android.domain.model.AuthConfig.empty(provider.id)
                    )
                } else {
                    AuthState.NotAuthenticated
                }
                
                _providerAuthStates.update { map ->
                    map + (provider.id to state)
                }
            }
            
            // 设置默认提供商
            val authProviders = Provider.values().filter { 
                _providerAuthStates.value[it.id] is AuthState.Authenticated 
            }
            
            if (authProviders.isNotEmpty() && _currentProvider.value == null) {
                setProvider(authProviders.first())
            }
        }
    }
    
    /**
     * 设置提供商
     */
    fun setProvider(provider: Provider) {
        _currentProvider.value = provider
        _currentModel.value = AIModel.getDefaultModel(provider.id)
        Timber.d("Set provider: ${provider.id}")
    }
    
    /**
     * 设置模型
     */
    fun setModel(model: AIModel) {
        _currentModel.value = model
        Timber.d("Set model: ${model.id}")
    }
    
    /**
     * 设置温度
     */
    fun setTemperature(value: Float) {
        _temperature.value = value
    }
    
    /**
     * 设置是否启用深度思考
     */
    fun setEnableThinking(enabled: Boolean) {
        _enableThinking.value = enabled
    }
    
    /**
     * 设置是否启用联网搜索
     */
    fun setEnableWebSearch(enabled: Boolean) {
        _enableWebSearch.value = enabled
    }
    
    /**
     * 设置是否启用流式输出
     */
    fun setEnableStreaming(enabled: Boolean) {
        _enableStreaming.value = enabled
    }
    
    /**
     * 登出
     */
    fun logout(provider: Provider) {
        viewModelScope.launch {
            authUseCase.logout(provider)
            _providerAuthStates.update { map ->
                map + (provider.id to AuthState.NotAuthenticated)
            }
            
            // 如果登出的是当前提供商，切换到其他已认证的提供商
            if (_currentProvider.value == provider) {
                val authProviders = Provider.values().filter { 
                    _providerAuthStates.value[it.id] is AuthState.Authenticated 
                }
                if (authProviders.isNotEmpty()) {
                    setProvider(authProviders.first())
                } else {
                    _currentProvider.value = null
                    _currentModel.value = null
                }
            }
        }
    }
    
    /**
     * 获取模型配置
     */
    fun getModelConfig(): ai.openclaw.android.domain.model.ModelConfig? {
        val model = _currentModel.value ?: return null
        return ai.openclaw.android.domain.model.ModelConfig(
            model = model,
            temperature = _temperature.value,
            enableThinking = _enableThinking.value,
            enableWebSearch = _enableWebSearch.value
        )
    }
}
