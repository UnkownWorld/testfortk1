package ai.openclaw.android.domain.usecase

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
 *
 * 使用示例：
 * ```kotlin
 * // 观察认证状态
 * authUseCase.getAuthState(Provider.DEEPSEEK)
 *     .collect { state -> ... }
 *
 * // 开始认证
 * authUseCase.authenticate(Provider.DEEPSEEK)
 *     .collect { state -> ... }
 *
 * // 检查认证状态
 * if (authUseCase.isAuthenticated(Provider.DEEPSEEK)) {
 *     // 已认证
 * }
 * ```
 */
@Singleton
class AuthUseCase @Inject constructor(
    private val authProvider: IAuthProvider
) {
    /**
     * 获取认证状态流
     *
     * @param provider 提供商
     * @return 认证状态流
     */
    fun getAuthState(provider: Provider): Flow<AuthState> {
        return authProvider.getAuthState(provider)
    }

    /**
     * 检查是否已认证
     *
     * @param provider 提供商
     * @return 如果已认证且凭证有效则返回 true
     */
    suspend fun isAuthenticated(provider: Provider): Boolean {
        return authProvider.isAuthenticated(provider)
    }

    /**
     * 获取认证配置
     *
     * @param provider 提供商
     * @return 认证配置，如果未认证则返回 null
     */
    suspend fun getAuthConfig(provider: Provider): AuthConfig? {
        return authProvider.getAuthConfig(provider)
    }

    /**
     * 开始认证流程
     *
     * @param provider 提供商
     * @return 认证状态流
     */
    fun authenticate(provider: Provider): Flow<AuthState> {
        return authProvider.authenticate(provider)
    }

    /**
     * 刷新认证
     *
     * @param provider 提供商
     * @return 刷新结果
     */
    suspend fun refresh(provider: Provider): Result<Boolean> {
        return authProvider.refresh(provider)
    }

    /**
     * 登出
     *
     * @param provider 提供商
     */
    suspend fun logout(provider: Provider) {
        authProvider.logout(provider)
    }

    /**
     * 获取所有已认证的提供商
     *
     * @return 已认证的提供商列表
     */
    suspend fun getAuthenticatedProviders(): List<Provider> {
        return authProvider.getAuthenticatedProviders()
    }

    /**
     * 检查是否有任何已认证的提供商
     *
     * @return 如果至少有一个已认证的提供商则返回 true
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
     *
     * @return 第一个已认证的提供商，如果没有则返回 null
     */
    suspend fun getFirstAuthenticatedProvider(): Provider? {
        return authProvider.getAuthenticatedProviders().firstOrNull()
    }

    /**
     * 确保已认证
     *
     * 如果未认证则抛出异常
     *
     * @param provider 提供商
     * @throws ChatException.NotAuthenticated 如果未认证
     */
    suspend fun ensureAuthenticated(provider: Provider) {
        if (!authProvider.isAuthenticated(provider)) {
            throw ai.openclaw.android.domain.model.ChatException.NotAuthenticated(provider)
        }
    }
}
