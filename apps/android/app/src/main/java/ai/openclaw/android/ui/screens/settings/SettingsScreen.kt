package ai.openclaw.android.ui.screens.settings

import ai.openclaw.android.domain.model.AIModel
import ai.openclaw.android.domain.model.AuthState
import ai.openclaw.android.domain.model.Provider
import ai.openclaw.android.ui.viewmodel.SettingsViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onLoginClick: (Provider) -> Unit
) {
    val currentProvider by viewModel.currentProvider.collectAsStateWithLifecycle()
    val currentModel by viewModel.currentModel.collectAsStateWithLifecycle()
    val temperature by viewModel.temperature.collectAsStateWithLifecycle()
    val providerAuthStates by viewModel.providerAuthStates.collectAsStateWithLifecycle()
    
    var showModelSelector by remember { mutableStateOf(false) }
    var showProviderSelector by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 账号管理
            item {
                SettingsSectionHeader(title = "账号管理")
            }
            
            items(Provider.values().toList()) { provider ->
                ProviderSettingItem(
                    provider = provider,
                    authState = providerAuthStates[provider.id] ?: AuthState.NotAuthenticated,
                    onLoginClick = { onLoginClick(provider) },
                    onLogoutClick = { viewModel.logout(provider) }
                )
            }
            
            // 模型设置
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(title = "模型设置")
            }
            
            item {
                SettingsItem(
                    title = "当前提供商",
                    subtitle = currentProvider?.displayName ?: "未选择",
                    icon = Icons.Default.Cloud,
                    onClick = { showProviderSelector = true }
                )
            }
            
            item {
                SettingsItem(
                    title = "当前模型",
                    subtitle = currentModel?.displayName ?: "未选择",
                    icon = Icons.Default.Psychology,
                    onClick = { showModelSelector = true }
                )
            }
            
            item {
                SettingsSliderItem(
                    title = "温度 (Temperature)",
                    subtitle = "控制回复的随机性，值越高越随机",
                    value = temperature,
                    valueRange = 0f..2f,
                    onValueChange = { viewModel.setTemperature(it) }
                )
            }
            
            // 功能设置
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(title = "功能设置")
            }
            
            item {
                SettingsSwitchItem(
                    title = "深度思考",
                    subtitle = "启用 R1 模式的深度推理",
                    checked = viewModel.enableThinking.collectAsStateWithLifecycle().value,
                    onCheckedChange = { viewModel.setEnableThinking(it) }
                )
            }
            
            item {
                SettingsSwitchItem(
                    title = "联网搜索",
                    subtitle = "启用联网搜索获取最新信息",
                    checked = viewModel.enableWebSearch.collectAsStateWithLifecycle().value,
                    onCheckedChange = { viewModel.setEnableWebSearch(it) }
                )
            }
            
            item {
                SettingsSwitchItem(
                    title = "流式输出",
                    subtitle = "实时显示 AI 回复",
                    checked = viewModel.enableStreaming.collectAsStateWithLifecycle().value,
                    onCheckedChange = { viewModel.setEnableStreaming(it) }
                )
            }
            
            // 关于
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(title = "关于")
            }
            
            item {
                SettingsItem(
                    title = "版本",
                    subtitle = "1.0.0",
                    icon = Icons.Default.Info,
                    onClick = { }
                )
            }
            
            item {
                SettingsItem(
                    title = "开源协议",
                    subtitle = "MIT License",
                    icon = Icons.Default.Description,
                    onClick = { }
                )
            }
        }
    }
    
    // 模型选择对话框
    if (showModelSelector) {
        ModelSelectorDialog(
            providerId = currentProvider?.id,
            currentModel = currentModel,
            onModelSelected = { model ->
                viewModel.setModel(model)
                showModelSelector = false
            },
            onDismiss = { showModelSelector = false }
        )
    }
    
    // 提供商选择对话框
    if (showProviderSelector) {
        ProviderSelectorDialog(
            currentProvider = currentProvider,
            providerAuthStates = providerAuthStates,
            onProviderSelected = { provider ->
                viewModel.setProvider(provider)
                showProviderSelector = false
            },
            onDismiss = { showProviderSelector = false }
        )
    }
}

/**
 * 设置分组标题
 */
@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

/**
 * 设置项
 */
@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 设置滑块项
 */
@Composable
private fun SettingsSliderItem(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("%.1f", value),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * 设置开关项
 */
@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

/**
 * 提供商设置项
 */
@Composable
private fun ProviderSettingItem(
    provider: Provider,
    authState: AuthState,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = provider.displayName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = when (authState) {
                        is AuthState.Authenticated -> "已登录"
                        is AuthState.Authenticating -> "登录中..."
                        is AuthState.Error -> "登录失败"
                        else -> "未登录"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (authState) {
                        is AuthState.Authenticated -> MaterialTheme.colorScheme.primary
                        is AuthState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            when (authState) {
                is AuthState.Authenticated -> {
                    TextButton(onClick = onLogoutClick) {
                        Text("登出")
                    }
                }
                is AuthState.Authenticating -> {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
                else -> {
                    Button(onClick = onLoginClick) {
                        Text("登录")
                    }
                }
            }
        }
    }
}

/**
 * 模型选择对话框
 */
@Composable
private fun ModelSelectorDialog(
    providerId: String?,
    currentModel: AIModel?,
    onModelSelected: (AIModel) -> Unit,
    onDismiss: () -> Unit
) {
    val models = providerId?.let { AIModel.getModelsByProvider(it) } ?: emptyList()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择模型") },
        text = {
            LazyColumn {
                items(models) { model ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModelSelected(model) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentModel == model,
                            onClick = { onModelSelected(model) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = model.displayName)
                            Text(
                                text = model.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 提供商选择对话框
 */
@Composable
private fun ProviderSelectorDialog(
    currentProvider: Provider?,
    providerAuthStates: Map<String, AuthState>,
    onProviderSelected: (Provider) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择提供商") },
        text = {
            LazyColumn {
                items(Provider.values().toList()) { provider ->
                    val isAuth = providerAuthStates[provider.id] is AuthState.Authenticated
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isAuth) { onProviderSelected(provider) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentProvider == provider,
                            onClick = { if (isAuth) onProviderSelected(provider) },
                            enabled = isAuth
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = provider.displayName,
                            color = if (isAuth) 
                                MaterialTheme.colorScheme.onSurface 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!isAuth) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(未登录)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
