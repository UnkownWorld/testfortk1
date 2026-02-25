package ai.openclaw.android.data.repository

import ai.openclaw.android.data.local.ConfigLocalDataSource
import ai.openclaw.android.data.remote.config.ModelsConfigResponse
import ai.openclaw.android.data.remote.config.RemoteConfigDataSource
import ai.openclaw.android.domain.model.ModelFeature
import ai.openclaw.android.domain.model.ProviderConfig
import ai.openclaw.android.domain.model.RemoteModelConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 模型配置仓库
 *
 * 管理模型配置的获取、缓存和更新
 */
@Singleton
class ModelConfigRepository @Inject constructor(
    private val remoteDataSource: RemoteConfigDataSource,
    private val localDataSource: ConfigLocalDataSource
) {
    // 当前配置
    private val _config = MutableStateFlow<ModelsConfigResponse?>(null)
    val config: StateFlow<ModelsConfigResponse?> = _config.asStateFlow()

    // 是否正在更新
    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating.asStateFlow()

    // 更新状态
    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    /**
     * 初始化配置
     *
     * 优先使用本地缓存，后台检查更新
     */
    suspend fun initialize() {
        // 尝试加载本地缓存
        val localConfig = localDataSource.getConfig()
        if (localConfig != null) {
            _config.value = localConfig
            Timber.d("Loaded local config: version=${localConfig.version}")
        }

        // 检查是否需要更新
        if (localDataSource.needsUpdate()) {
            refreshConfig()
        }
    }

    /**
     * 刷新配置
     *
     * 从远程获取最新配置
     */
    suspend fun refreshConfig(): Boolean {
        if (_isUpdating.value) return false

        _isUpdating.value = true
        _updateStatus.value = UpdateStatus.Checking

        try {
            val remoteConfig = remoteDataSource.fetchConfig()
            
            if (remoteConfig != null) {
                // 保存到本地
                localDataSource.saveConfig(remoteConfig)
                _config.value = remoteConfig
                _updateStatus.value = UpdateStatus.Success(remoteConfig.version)
                Timber.d("Refreshed config: version=${remoteConfig.version}")
                return true
            } else {
                _updateStatus.value = UpdateStatus.Error("Failed to fetch remote config")
                return false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh config")
            _updateStatus.value = UpdateStatus.Error(e.message ?: "Unknown error")
            return false
        } finally {
            _isUpdating.value = false
        }
    }

    /**
     * 获取提供商列表
     */
    fun getProviders(): List<ProviderConfig> {
        return _config.value?.providers?.map { it.toProviderConfig() } ?: emptyList()
    }

    /**
     * 获取指定提供商的模型列表
     */
    fun getModels(providerId: String): List<RemoteModelConfig> {
        return _config.value?.providers
            ?.find { it.id == providerId }
            ?.models
            ?.map { it.toModelConfig() }
            ?: emptyList()
    }

    /**
     * 获取默认提供商
     */
    fun getDefaultProvider(): String? {
        return _config.value?.defaultProvider
    }

    /**
     * 获取默认模型
     */
    fun getDefaultModel(providerId: String): String? {
        return _config.value?.defaultModels?.get(providerId)
    }

    /**
     * 获取提供商配置
     */
    fun getProviderConfig(providerId: String): ProviderConfig? {
        return _config.value?.providers
            ?.find { it.id == providerId }
            ?.toProviderConfig()
    }

    /**
     * 获取模型配置
     */
    fun getModelConfig(providerId: String, modelId: String): RemoteModelConfig? {
        return _config.value?.providers
            ?.find { it.id == providerId }
            ?.models
            ?.find { it.id == modelId }
            ?.toModelConfig()
    }

    /**
     * 检查是否有更新
     */
    suspend fun checkForUpdate(): Boolean {
        val currentVersion = localDataSource.getConfigVersion() ?: return true
        return remoteDataSource.checkForUpdate(currentVersion)
    }

    /**
     * 获取配置版本
     */
    fun getConfigVersion(): String? {
        return _config.value?.version
    }

    /**
     * 获取上次更新时间
     */
    suspend fun getLastUpdateTime(): Long? {
        return localDataSource.getLastUpdateTime()
    }
}

/**
 * 更新状态
 */
sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class Success(val version: String) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

// 扩展函数：转换响应为领域模型

private fun ai.openclaw.android.data.remote.config.ProviderConfigResponse.toProviderConfig(): ProviderConfig {
    return ProviderConfig(
        id = id,
        displayName = displayName,
        loginUrl = loginUrl,
        apiBaseUrl = apiBaseUrl,
        enabled = enabled,
        models = models.map { it.toModelConfig() }
    )
}

private fun ai.openclaw.android.data.remote.config.ModelConfigResponse.toModelConfig(): RemoteModelConfig {
    return RemoteModelConfig(
        id = id,
        displayName = displayName,
        features = features.mapNotNull { 
            try { ModelFeature.valueOf(it) } catch (e: Exception) { null }
        }.toSet(),
        maxTokens = maxTokens,
        description = description,
        enabled = enabled
    )
}
