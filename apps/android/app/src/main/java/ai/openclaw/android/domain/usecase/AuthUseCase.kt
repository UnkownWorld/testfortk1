package ai.openclaw.android.domain.usecase

import ai.openclaw.android.data.repository.AuthProviderImpl
import ai.openclaw.android.domain.model.AuthConfig
import ai.openclaw.android.domain.model.AuthState
import ai.openclaw.android.domain.model.Provider
import ai.openclaw.android.domain.repository.IAuthProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证用例
 *
 * 封装认证相关的业务逻辑，提供统一的认证操作接口
 */
@Singleton
class AuthUseCase @Inject constructor(
    private val authProvider: IAuthProvider
) {
    /**
     * 获取认证状态流
     */
    fun getAuthState(provider: Provider): Flow<AuthState> {
        return authProvider.getAuthState(provider)
    }

    /**
     * 检查是否已认证
     */
    suspend fun isAuthenticated(provider: Provider): Boolean {
        return authProvider.isAuthenticated(provider)
    }

    /**
     * 获取认证配置
     */
    suspend fun getAuthConfig(provider: Provider): AuthConfig? {
        return authProvider.getAuthConfig(provider)
    }

    /**
     * 保存认证配置
     *
     * 用于 WebView 认证完成后保存凭证
     *
     * @param config 认证配置
     */
    suspend fun saveAuthConfig(config: AuthConfig) {
        // 使用 AuthProviderImpl 的 saveAuthConfig 方法
        if (authProvider is AuthProviderImpl) {
            authProvider.saveAuthConfig(config)
        }
    }

    /**
     * 开始认证流程
     */
    fun authenticate(provider: Provider): Flow<AuthState> {
        return authProvider.authenticate(provider)
    }

    /**
     * 刷新认证
     */
    suspend fun refresh(provider: Provider): Result<Boolean> {
        return authProvider.refresh(provider)
    }

    /**
     * 登出
     */
    suspend fun logout(provider: Provider) {
        authProvider.logout(provider)
    }

    /**
     * 获取所有已认证的提供商
     */
    suspend fun getAuthenticatedProviders(): List<Provider> {
        return authProvider.getAuthenticatedProviders()
    }

    /**
     * 检查是否有任何已认证的提供商
     */
    suspend fun hasAnyAuthenticated(): Boolean {
        return authProvider.getAuthenticatedProviders().isNotEmpty()
    }

    /**
     * 清除所有认证信息
     */
    suspend fun clearAll() {
        authProvider.clearAll()
    }

    /**
     * 获取第一个已认证的提供商
     */
    suspend fun getFirstAuthenticatedProvider(): Provider? {
        return authProvider.getAuthenticatedProviders().firstOrNull()
    }

    /**
     * 确保已认证
     */
    suspend fun ensureAuthenticated(provider: Provider) {
        if (!authProvider.isAuthenticated(provider)) {
            throw ai.openclaw.android.domain.model.ChatException.NotAuthenticated(provider)
        }
    }
}
