package ai.openclaw.android.domain.repository

import ai.openclaw.android.domain.model.AuthConfig
import ai.openclaw.android.domain.model.AuthState
import ai.openclaw.android.domain.model.Provider
import kotlinx.coroutines.flow.Flow

/**
 * 认证提供者接口
 *
 * 定义认证相关的操作，包括状态查询、认证流程和凭证管理
 */
interface IAuthProvider {

    /**
     * 获取认证状态流
     *
     * @param provider 提供商
     * @return 认证状态流，可用于观察认证状态变化
     */
    fun getAuthState(provider: Provider): Flow<AuthState>

    /**
     * 检查是否已认证
     *
     * @param provider 提供商
     * @return 如果已认证且凭证有效则返回 true
     */
    suspend fun isAuthenticated(provider: Provider): Boolean

    /**
     * 获取认证配置
     *
     * @param provider 提供商
     * @return 认证配置，如果未认证或凭证无效则返回 null
     */
    suspend fun getAuthConfig(provider: Provider): AuthConfig?

    /**
     * 开始认证流程
     *
     * 启动 WebView 认证流程，通过 Flow 返回状态更新
     *
     * @param provider 提供商
     * @return 认证状态流，包含认证过程中的各种状态
     */
    fun authenticate(provider: Provider): Flow<AuthState>

    /**
     * 刷新认证
     *
     * 尝试使用当前凭证刷新认证状态
     *
     * @param provider 提供商
     * @return 刷新结果，成功返回 true
     */
    suspend fun refresh(provider: Provider): Result<Boolean>

    /**
     * 登出
     *
     * 清除指定提供商的认证信息
     *
     * @param provider 提供商
     */
    suspend fun logout(provider: Provider)

    /**
     * 获取所有已认证的提供商
     *
     * @return 已认证的提供商列表
     */
    suspend fun getAuthenticatedProviders(): List<Provider>

    /**
     * 清除所有认证信息
     */
    suspend fun clearAll()
}
