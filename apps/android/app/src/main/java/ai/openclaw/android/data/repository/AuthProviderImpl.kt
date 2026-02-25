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
    private val localDataSource: AuthLocalDataSource,
    private val remoteDataSource: AuthRemoteDataSource
) : IAuthProvider {

    // 认证状态缓存
    private val authStates = mutableMapOf<String, MutableStateFlow<AuthState>>()

    init {
        // 初始化时加载已保存的认证状态
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
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize auth state for ${provider.id}")
            }
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

    override fun authenticate(provider: Provider): Flow<AuthState> = channelFlow {
        // 获取状态流
        val stateFlow = authStates[provider.id] 
            ?: MutableStateFlow(AuthState.NotAuthenticated)

        // 更新状态为认证中
        stateFlow.value = AuthState.Authenticating(provider)
        send(AuthState.Authenticating(provider))

        try {
            // 使用远程数据源进行认证
            remoteDataSource.authenticate(provider)
                .catch { e ->
                    Timber.e(e, "Authentication failed for ${provider.id}")
                    val errorState = AuthState.Error(
                        code = "AUTH_ERROR",
                        message = e.message ?: "Authentication failed"
                    )
                    stateFlow.value = errorState
                    send(errorState)
                }
                .collect { state ->
                    when (state) {
                        is AuthState.Authenticated -> {
                            // 保存认证配置
                            localDataSource.saveAuthConfig(state.config)
                            stateFlow.value = state
                            send(state)
                        }
                        is AuthState.Cancelled -> {
                            stateFlow.value = state
                            send(state)
                        }
                        is AuthState.Error -> {
                            stateFlow.value = state
                            send(state)
                        }
                        else -> {
                            send(state)
                        }
                    }
                }
        } catch (e: Exception) {
            Timber.e(e, "Authentication flow failed")
            val errorState = AuthState.Error(
                code = "AUTH_FLOW_ERROR",
                message = e.message ?: "Authentication flow failed"
            )
            stateFlow.value = errorState
            send(errorState)
        }
    }

    override suspend fun refresh(provider: Provider): Result<Boolean> {
        return try {
            val currentConfig = getAuthConfig(provider)
                ?: return Result.failure(Exception("Not authenticated"))

            val newConfig = remoteDataSource.refresh(provider, currentConfig)

            if (newConfig != null) {
                localDataSource.saveAuthConfig(newConfig)
                authStates[provider.id]?.value = AuthState.Authenticated(newConfig)
                Result.success(true)
            } else {
                // 刷新失败，清除认证
                logout(provider)
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
