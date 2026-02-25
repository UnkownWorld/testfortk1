package ai.openclaw.android.ui.screens.settings

import ai.openclaw.android.data.repository.UpdateStatus
import ai.openclaw.android.domain.model.ProviderConfig
import ai.openclaw.android.domain.model.RemoteModelConfig
import ai.openclaw.android.ui.viewmodel.ModelSelectionViewModel
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
 * 模型选择页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionScreen(
    viewModel: ModelSelectionViewModel,
    onNavigateBack: () -> Unit
) {
    val providers by viewModel.providers.collectAsStateWithLifecycle()
    val selectedProvider by viewModel.selectedProvider.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val models by viewModel.models.collectAsStateWithLifecycle()
    val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()
    val isUpdating by viewModel.isUpdating.collectAsStateWithLifecycle()
    
    var showProviderDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("模型设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshConfig() },
                        enabled = !isUpdating
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 更新状态
            when (updateStatus) {
                is UpdateStatus.Success -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "配置已更新到 ${(updateStatus as UpdateStatus.Success).version}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                is UpdateStatus.Error -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = (updateStatus as UpdateStatus.Error).message,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
            
            // 提供商选择
            item {
                SettingsSectionHeader(title = "AI 提供商")
            }
            
            item {
                ModelSelectionCard(
                    title = "当前提供商",
                    value = selectedProvider?.displayName ?: "未选择",
                    icon = Icons.Default.Cloud,
                    onClick = { showProviderDialog = true }
                )
            }
            
            // 模型选择
            item {
                SettingsSectionHeader(title = "AI 模型")
            }
            
            item {
                ModelSelectionCard(
                    title = "当前模型",
                    value = selectedModel?.displayName ?: "未选择",
                    subtitle = selectedModel?.description,
                    icon = Icons.Default.Psychology,
                    onClick = { showModelDialog = true }
                )
            }
            
            // 模型特性
            selectedModel?.let { model ->
                item {
                    ModelFeaturesCard(model = model)
                }
            }
            
            // 配置版本信息
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(title = "配置信息")
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "配置版本: ${viewModel.configVersion ?: "未知"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "提供商数量: ${providers.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "模型数量: ${providers.sumOf { it.models.size }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    // 提供商选择对话框
    if (showProviderDialog) {
        ProviderSelectionDialog(
            providers = providers,
            selectedProvider = selectedProvider,
            onProviderSelected = { provider ->
                viewModel.selectProvider(provider)
                showProviderDialog = false
            },
            onDismiss = { showProviderDialog = false }
        )
    }
    
    // 模型选择对话框
    if (showModelDialog) {
        ModelSelectionDialog(
            models = models,
            selectedModel = selectedModel,
            onModelSelected = { model ->
                viewModel.selectModel(model)
                showModelDialog = false
            },
            onDismiss = { showModelDialog = false }
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
 * 模型选择卡片
 */
@Composable
private fun ModelSelectionCard(
    title: String,
    value: String,
    subtitle: String? = null,
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
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
 * 模型特性卡片
 */
@Composable
private fun ModelFeaturesCard(model: RemoteModelConfig) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "模型特性",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (model.supportsThinking()) {
                    FeatureChip(text = "深度思考", icon = Icons.Default.Psychology)
                }
                if (model.supportsWebSearch()) {
                    FeatureChip(text = "联网搜索", icon = Icons.Default.Search)
                }
                if (model.supportsVision()) {
                    FeatureChip(text = "视觉理解", icon = Icons.Default.Image)
                }
                if (model.features.any { it.name == "CODE_EXECUTION" }) {
                    FeatureChip(text = "代码执行", icon = Icons.Default.Code)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "最大 Token: ${model.maxTokens}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 特性标签
 */
@Composable
private fun FeatureChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    AssistChip(
        onClick = { },
        label = { Text(text) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    )
}

/**
 * 提供商选择对话框
 */
@Composable
private fun ProviderSelectionDialog(
    providers: List<ProviderConfig>,
    selectedProvider: ProviderConfig?,
    onProviderSelected: (ProviderConfig) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择提供商") },
        text = {
            LazyColumn {
                items(providers) { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onProviderSelected(provider) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedProvider?.id == provider.id,
                            onClick = { onProviderSelected(provider) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = provider.displayName)
                            Text(
                                text = "${provider.models.size} 个模型",
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
 * 模型选择对话框
 */
@Composable
private fun ModelSelectionDialog(
    models: List<RemoteModelConfig>,
    selectedModel: RemoteModelConfig?,
    onModelSelected: (RemoteModelConfig) -> Unit,
    onDismiss: () -> Unit
) {
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
                            selected = selectedModel?.id == model.id,
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

// 扩展函数
private fun Modifier.clickable(onClick: () -> Unit): Modifier {
    return androidx.compose.foundation.clickable(onClick = onClick)
}
