package ai.openclaw.android.ui.viewmodel

import ai.openclaw.android.data.repository.ModelConfigRepository
import ai.openclaw.android.data.repository.UpdateStatus
import ai.openclaw.android.domain.model.ProviderConfig
import ai.openclaw.android.domain.model.RemoteModelConfig
import ai.openclaw.android.domain.model.UserModelPreferences
import ai.openclaw.android.domain.usecase.AuthUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 模型选择 ViewModel
 *
 * 管理动态模型配置
 */
@HiltViewModel
class ModelSelectionViewModel @Inject constructor(
    private val modelConfigRepository: ModelConfigRepository,
    private val authUseCase: AuthUseCase
) : ViewModel() {

    // 提供商列表
    private val _providers = MutableStateFlow<List<ProviderConfig>>(emptyList())
    val providers: StateFlow<List<ProviderConfig>> = _providers.asStateFlow()

    // 当前选中的提供商
    private val _selectedProvider = MutableStateFlow<ProviderConfig?>(null)
    val selectedProvider: StateFlow<ProviderConfig?> = _selectedProvider.asStateFlow()

    // 当前提供商的模型列表
    val models: StateFlow<List<RemoteModelConfig>> = _selectedProvider
        .map { provider -> provider?.models ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 当前选中的模型
    private val _selectedModel = MutableStateFlow<RemoteModelConfig?>(null)
    val selectedModel: StateFlow<RemoteModelConfig?> = _selectedModel.asStateFlow()

    // 用户偏好
    private val _preferences = MutableStateFlow<UserModelPreferences?>(null)
    val preferences: StateFlow<UserModelPreferences?> = _preferences.asStateFlow()

    // 更新状态
    val updateStatus: StateFlow<UpdateStatus> = modelConfigRepository.updateStatus

    // 是否正在更新
    val isUpdating: StateFlow<Boolean> = modelConfigRepository.isUpdating

    // 配置版本
    val configVersion: String? = modelConfigRepository.configVersion

    init {
        loadConfig()
    }

    /**
     * 加载配置
     */
    private fun loadConfig() {
        viewModelScope.launch {
            // 初始化配置
            modelConfigRepository.initialize()

            // 加载提供商列表
            val providers = modelConfigRepository.getProviders()
            _providers.value = providers

            // 设置默认提供商
            val defaultProviderId = modelConfigRepository.getDefaultProvider()
            val defaultProvider = providers.find { it.id == defaultProviderId }
                ?: providers.firstOrNull()

            if (defaultProvider != null) {
                selectProvider(defaultProvider)
            }

            Timber.d("Loaded ${providers.size} providers")
        }
    }

    /**
     * 刷新配置
     */
    fun refreshConfig() {
        viewModelScope.launch {
            modelConfigRepository.refreshConfig()
            
            // 重新加载提供商列表
            val providers = modelConfigRepository.getProviders()
            _providers.value = providers
        }
    }

    /**
     * 选择提供商
     */
    fun selectProvider(provider: ProviderConfig) {
        _selectedProvider.value = provider

        // 设置默认模型
        val defaultModelId = modelConfigRepository.getDefaultModel(provider.id)
        val defaultModel = provider.models.find { it.id == defaultModelId }
            ?: provider.models.firstOrNull()

        if (defaultModel != null) {
            selectModel(defaultModel)
        }

        Timber.d("Selected provider: ${provider.id}")
    }

    /**
     * 选择模型
     */
    fun selectModel(model: RemoteModelConfig) {
        _selectedModel.value = model

        // 更新偏好
        _preferences.value = UserModelPreferences(
            providerId = _selectedProvider.value?.id ?: "",
            modelId = model.id
        )

        Timber.d("Selected model: ${model.id}")
    }

    /**
     * 设置温度
     */
    fun setTemperature(value: Float) {
        _preferences.value = _preferences.value?.copy(temperature = value)
    }

    /**
     * 设置是否启用深度思考
     */
    fun setEnableThinking(enabled: Boolean) {
        _preferences.value = _preferences.value?.copy(enableThinking = enabled)
    }

    /**
     * 设置是否启用联网搜索
     */
    fun setEnableWebSearch(enabled: Boolean) {
        _preferences.value = _preferences.value?.copy(enableWebSearch = enabled)
    }

    /**
     * 设置系统提示
     */
    fun setSystemPrompt(prompt: String?) {
        _preferences.value = _preferences.value?.copy(systemPrompt = prompt)
    }

    /**
     * 获取已认证的提供商
     */
    fun getAuthenticatedProviders(): List<ProviderConfig> {
        return _providers.value.filter { provider ->
            run {
                val isAuth = authUseCase.isAuthenticated(
                    ai.openclaw.android.domain.model.Provider.fromId(provider.id) 
                        ?: return@run false
                )
                isAuth
            }
        }
    }

    /**
     * 获取当前配置
     */
    fun getCurrentPreferences(): UserModelPreferences? {
        return _preferences.value
    }

    /**
     * 检查模型是否支持当前功能设置
     */
    fun checkModelSupport(): ModelSupportStatus {
        val model = _selectedModel.value ?: return ModelSupportStatus.Unsupported("No model selected")
        val prefs = _preferences.value ?: return ModelSupportStatus.Supported

        val warnings = mutableListOf<String>()

        if (prefs.enableThinking && !model.supportsThinking()) {
            warnings.add("当前模型不支持深度思考")
        }

        if (prefs.enableWebSearch && !model.supportsWebSearch()) {
            warnings.add("当前模型不支持联网搜索")
        }

        return if (warnings.isEmpty()) {
            ModelSupportStatus.Supported
        } else {
            ModelSupportStatus.Warning(warnings)
        }
    }
}

/**
 * 模型支持状态
 */
sealed class ModelSupportStatus {
    object Supported : ModelSupportStatus()
    data class Warning(val messages: List<String>) : ModelSupportStatus()
    data class Unsupported(val message: String) : ModelSupportStatus()
}
