package ai.openclaw.android.data.repository

import ai.openclaw.android.data.local.AuthLocalDataSource
import ai.openclaw.android.data.remote.AuthRemoteDataSource
import ai.openclaw.android.domain.model.AuthConfig
import ai.openclaw.android.domain.model.AuthState
import ai.openclaw.android.domain.model.Provider
import ai.openclaw.android.domain.repository.IAuthProvider
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证提供者实现
 *
 * 协调本地数据源和远程数据源，提供统一的认证接口
 */
@Singleton
class AuthProviderImpl @Inject constructor(
    private val localDataSource: AuthLocalDataSource
) : IAuthProvider {

    // 认证状态缓存
    private val authStates = mutableMapOf<String, MutableStateFlow<AuthState>>()

    init {
        // 初始化时创建所有提供商的状态流
        Provider.values().forEach { provider ->
            authStates[provider.id] = MutableStateFlow(AuthState.NotAuthenticated)
        }
    }

    /**
     * 初始化认证状态
     *
     * 从本地存储加载已保存的认证配置
     */
    suspend fun initialize() {
        Provider.values().forEach { provider ->
            try {
                val config = localDataSource.getAuthConfig(provider.id)
                if (config != null && config.isValid()) {
                    authStates[provider.id]?.value = AuthState.Authenticated(config)
                    Timber.d("Loaded auth config for ${provider.id}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize auth state for ${provider.id}")
            }
        }
    }

    /**
     * 保存认证配置
     *
     * 直接保存配置到本地存储（用于 WebView 认证完成后）
     *
     * @param config 认证配置
     */
    suspend fun saveAuthConfig(config: AuthConfig) {
        try {
            localDataSource.saveAuthConfig(config)
            authStates[config.provider]?.value = AuthState.Authenticated(config)
            Timber.d("Saved auth config for ${config.provider}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save auth config for ${config.provider}")
            throw e
        }
    }

    override fun getAuthState(provider: Provider): Flow<AuthState> {
        return authStates[provider.id] ?: flowOf(AuthState.NotAuthenticated)
    }

    override suspend fun isAuthenticated(provider: Provider): Boolean {
        val config = getAuthConfig(provider)
        return config != null && config.isValid()
    }

    override suspend fun getAuthConfig(provider: Provider): AuthConfig? {
        return try {
            val config = localDataSource.getAuthConfig(provider.id)
            if (config != null && config.isValid()) {
                config
            } else {
                // 配置无效，清除状态
                if (config != null) {
                    authStates[provider.id]?.value = AuthState.NotAuthenticated
                }
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get auth config for ${provider.id}")
            null
        }
    }

    override fun authenticate(provider: Provider): Flow<AuthState> = flow {
        // 这个方法现在由 WebView 认证流程处理
        // 这里只发送一个状态表示需要通过 WebView 认证
        emit(AuthState.Authenticating(provider))
    }

    override suspend fun refresh(provider: Provider): Result<Boolean> {
        return try {
            val currentConfig = getAuthConfig(provider)
            if (currentConfig != null) {
                // 更新过期时间
                val newConfig = currentConfig.withExpiration(AuthConfig.DEFAULT_EXPIRATION_MS)
                localDataSource.saveAuthConfig(newConfig)
                authStates[provider.id]?.value = AuthState.Authenticated(newConfig)
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh auth for ${provider.id}")
            Result.failure(e)
        }
    }

    override suspend fun logout(provider: Provider) {
        try {
            localDataSource.deleteAuthConfig(provider.id)
            authStates[provider.id]?.value = AuthState.NotAuthenticated
            Timber.d("Logged out from ${provider.id}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to logout from ${provider.id}")
        }
    }

    override suspend fun getAuthenticatedProviders(): List<Provider> {
        return Provider.values().filter { isAuthenticated(it) }
    }

    override suspend fun clearAll() {
        try {
            localDataSource.clearAll()
            Provider.values().forEach { provider ->
                authStates[provider.id]?.value = AuthState.NotAuthenticated
            }
            Timber.d("Cleared all auth configs")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear all auth configs")
        }
    }
}
