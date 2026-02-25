package ai.openclaw.android.ui.screens.auth

import ai.openclaw.android.domain.model.AuthState
import ai.openclaw.android.domain.model.Provider
import ai.openclaw.android.ui.viewmodel.AuthViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 认证选择页面
 *
 * 显示所有可用的提供商及其认证状态
 *
 * @param viewModel 认证 ViewModel
 * @param onProviderSelected 选择提供商后的回调（跳转到 WebView）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthSelectionScreen(
    viewModel: AuthViewModel,
    onProviderSelected: (Provider) -> Unit
) {
    val providerAuthStates by viewModel.providerAuthStates.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择登录平台") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(Provider.values().toList()) { provider ->
                ProviderCard(
                    provider = provider,
                    authState = providerAuthStates[provider.id] ?: AuthState.NotAuthenticated,
                    onLogin = { onProviderSelected(provider) },
                    onLogout = { viewModel.logout(provider) },
                    onRefresh = { viewModel.refreshAuthentication(provider) }
                )
            }
            
            // 说明信息
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "使用说明",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = """
                                1. 点击"登录"按钮进入登录页面
                                2. 使用您的账号登录
                                3. 登录成功后自动返回
                                4. 凭证将保存在本地，7天内有效
                                
                                注意：您的账号信息仅保存在本地设备，不会上传到任何服务器。
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * 提供商卡片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderCard(
    provider: Provider,
    authState: AuthState,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (authState is AuthState.Authenticated) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 提供商信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getStatusText(authState),
                    style = MaterialTheme.typography.bodyMedium,
                    color = getStatusColor(authState),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 操作按钮
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (authState) {
                    is AuthState.NotAuthenticated -> {
                        // 登录按钮
                        FilledIconButton(onClick = onLogin) {
                            Icon(Icons.Default.Login, contentDescription = "登录")
                        }
                    }
                    is AuthState.Authenticating -> {
                        // 加载中
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                    }
                    is AuthState.Authenticated -> {
                        // 已认证：显示刷新和登出按钮
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, contentDescription = "登出")
                        }
                        // 已认证图标
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "已认证",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    is AuthState.Error -> {
                        // 错误状态：显示重试按钮
                        FilledIconButton(onClick = onLogin) {
                            Icon(Icons.Default.Refresh, contentDescription = "重试")
                        }
                    }
                    is AuthState.Cancelled -> {
                        // 已取消：显示登录按钮
                        FilledIconButton(onClick = onLogin) {
                            Icon(Icons.Default.Login, contentDescription = "登录")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 获取状态文本
 */
@Composable
private fun getStatusText(authState: AuthState): String {
    return when (authState) {
        is AuthState.NotAuthenticated -> "未登录"
        is AuthState.Authenticating -> "登录中..."
        is AuthState.Authenticated -> {
            val config = authState.config
            val remaining = config.remainingTime()
            if (remaining != null) {
                val days = remaining / (24 * 60 * 60 * 1000)
                val hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
                if (days > 0) {
                    "已登录 · 剩余 ${days}天 ${hours}小时"
                } else {
                    "已登录 · 剩余 ${hours}小时"
                }
            } else {
                "已登录"
            }
        }
        is AuthState.Error -> "错误: ${authState.message}"
        is AuthState.Cancelled -> "已取消"
    }
}

/**
 * 获取状态颜色
 */
@Composable
private fun getStatusColor(authState: AuthState): androidx.compose.ui.graphics.Color {
    return when (authState) {
        is AuthState.NotAuthenticated -> MaterialTheme.colorScheme.onSurfaceVariant
        is AuthState.Authenticating -> MaterialTheme.colorScheme.primary
        is AuthState.Authenticated -> MaterialTheme.colorScheme.primary
        is AuthState.Error -> MaterialTheme.colorScheme.error
        is AuthState.Cancelled -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
