package ai.openclaw.android.ui.viewmodel

import ai.openclaw.android.domain.model.*
import ai.openclaw.android.domain.repository.ChatOptions
import ai.openclaw.android.domain.usecase.AuthUseCase
import ai.openclaw.android.domain.usecase.ChatUseCase
import ai.openclaw.android.domain.usecase.SessionUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 聊天 ViewModel
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatUseCase: ChatUseCase,
    private val sessionUseCase: SessionUseCase,
    private val authUseCase: AuthUseCase
) : ViewModel() {
    
    // 当前会话
    private val _currentSession = MutableStateFlow<Session?>(null)
    val currentSession: StateFlow<Session?> = _currentSession.asStateFlow()
    
    // 消息列表
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    // 输入文本
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 错误信息
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // 当前提供商
    private val _currentProvider = MutableStateFlow<Provider?>(null)
    val currentProvider: StateFlow<Provider?> = _currentProvider.asStateFlow()
    
    // 当前模型
    private val _currentModel = MutableStateFlow<AIModel?>(null)
    val currentModel: StateFlow<AIModel?> = _currentModel.asStateFlow()
    
    // 已认证的提供商列表
    private val _authenticatedProviders = MutableStateFlow<List<Provider>>(emptyList())
    val authenticatedProviders: StateFlow<List<Provider>> = _authenticatedProviders.asStateFlow()
    
    // 模型配置
    private val _enableThinking = MutableStateFlow(false)
    val enableThinking: StateFlow<Boolean> = _enableThinking.asStateFlow()
    
    private val _enableWebSearch = MutableStateFlow(false)
    val enableWebSearch: StateFlow<Boolean> = _enableWebSearch.asStateFlow()
    
    private val _temperature = MutableStateFlow(0.7f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()
    
    init {
        loadAuthenticatedProviders()
        loadOrCreateSession()
    }
    
    /**
     * 加载已认证的提供商
     */
    private fun loadAuthenticatedProviders() {
        viewModelScope.launch {
            val providers = authUseCase.getAuthenticatedProviders()
            _authenticatedProviders.value = providers
            
            if (providers.isNotEmpty() && _currentProvider.value == null) {
                _currentProvider.value = providers.first()
                _currentModel.value = AIModel.getDefaultModel(providers.first().id)
            }
            
            Timber.d("Loaded ${providers.size} authenticated providers")
        }
    }
    
    /**
     * 加载或创建会话
     */
    private fun loadOrCreateSession() {
        viewModelScope.launch {
            val activeSession = sessionUseCase.getActiveSession().first()
            
            if (activeSession != null) {
                _currentSession.value = activeSession
                _messages.value = activeSession.messages
                _currentProvider.value = Provider.fromId(activeSession.provider)
                _currentModel.value = AIModel.getDefaultModel(activeSession.provider)
                Timber.d("Loaded active session: ${activeSession.id}")
            } else {
                val provider = _currentProvider.value ?: 
                    _authenticatedProviders.value.firstOrNull()
                
                if (provider != null) {
                    createNewSession(provider)
                }
            }
        }
    }
    
    /**
     * 加载指定会话
     */
    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val session = sessionUseCase.getSession(sessionId)
                if (session != null) {
                    _currentSession.value = session
                    _messages.value = session.messages
                    _currentProvider.value = Provider.fromId(session.provider)
                    _currentModel.value = AIModel.getDefaultModel(session.provider)
                    sessionUseCase.switchSession(sessionId)
                    Timber.d("Loaded session: $sessionId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load session: $sessionId")
                _error.value = "加载会话失败"
            }
        }
    }
    
    /**
     * 创建新会话
     */
    fun createNewSession(provider: Provider? = null) {
        viewModelScope.launch {
            val targetProvider = provider ?: _currentProvider.value ?: return@launch
            
            try {
                val session = sessionUseCase.createAndActivateSession(targetProvider.id)
                _currentSession.value = session
                _messages.value = emptyList()
                _currentProvider.value = targetProvider
                _currentModel.value = AIModel.getDefaultModel(targetProvider.id)
                
                Timber.d("Created new session: ${session.id}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to create new session")
                _error.value = "创建会话失败: ${e.message}"
            }
        }
    }
    
    /**
     * 发送消息
     */
    fun sendMessage(text: String) {
        val session = _currentSession.value
        val provider = _currentProvider.value
        val model = _currentModel.value
        
        if (session == null) {
            _error.value = "没有活跃会话"
            return
        }
        
        if (provider == null) {
            _error.value = "没有选择提供商"
            return
        }
        
        if (!authUseCase.isAuthenticated(provider)) {
            _error.value = "请先登录 ${provider.displayName}"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _inputText.value = ""
            
            // 添加用户消息
            val userMessage = ChatMessage.user(text)
            _messages.value = _messages.value + userMessage
            
            // 构建助手消息
            val assistantContent = StringBuilder()
            
            // 构建聊天选项
            val options = ChatOptions(
                model = model?.id ?: AIModel.getDefaultModel(provider.id).id,
                temperature = _temperature.value.toDouble(),
                thinking = _enableThinking.value
            )
            
            try {
                chatUseCase.sendMessage(text, session, options).collect { chunk ->
                    when (chunk) {
                        is ChatChunk.Text -> {
                            assistantContent.append(chunk.text)
                            updateAssistantMessage(assistantContent.toString())
                        }
                        is ChatChunk.Thinking -> {
                            // 显示思考过程
                            updateThinkingMessage(chunk.text)
                        }
                        is ChatChunk.Done -> {
                            _isLoading.value = false
                            // 最终保存消息
                            finalizeAssistantMessage(assistantContent.toString())
                        }
                        is ChatChunk.Error -> {
                            _isLoading.value = false
                            _error.value = chunk.message
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to send message")
                _error.value = "发送失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 更新助手消息（流式）
     */
    private fun updateAssistantMessage(content: String) {
        val currentMessages = _messages.value.toMutableList()
        val lastAssistantIndex = currentMessages.indexOfLast { 
            it.role == ChatMessage.Role.ASSISTANT 
        }
        
        if (lastAssistantIndex >= 0 && 
            currentMessages[lastAssistantIndex].id.startsWith("streaming_")) {
            currentMessages[lastAssistantIndex] = currentMessages[lastAssistantIndex].copy(
                content = content
            )
        } else {
            currentMessages.add(ChatMessage.assistant(content).copy(
                id = "streaming_${System.currentTimeMillis()}"
            ))
        }
        
        _messages.value = currentMessages
    }
    
    /**
     * 更新思考消息
     */
    private fun updateThinkingMessage(content: String) {
        val currentMessages = _messages.value.toMutableList()
        val lastThinkingIndex = currentMessages.indexOfLast { 
            it.id.startsWith("thinking_") 
        }
        
        if (lastThinkingIndex >= 0) {
            val existing = currentMessages[lastThinkingIndex]
            currentMessages[lastThinkingIndex] = existing.copy(
                content = existing.content + content
            )
        } else {
            currentMessages.add(ChatMessage.assistant("💭 思考中...\n$content").copy(
                id = "thinking_${System.currentTimeMillis()}"
            ))
        }
        
        _messages.value = currentMessages
    }
    
    /**
     * 最终化助手消息
     */
    private fun finalizeAssistantMessage(content: String) {
        val currentMessages = _messages.value.toMutableList()
        
        // 移除思考消息
        currentMessages.removeAll { it.id.startsWith("thinking_") }
        
        // 更新或添加助手消息
        val lastAssistantIndex = currentMessages.indexOfLast { 
            it.role == ChatMessage.Role.ASSISTANT 
        }
        
        if (lastAssistantIndex >= 0 && 
            currentMessages[lastAssistantIndex].id.startsWith("streaming_")) {
            currentMessages[lastAssistantIndex] = ChatMessage.assistant(content)
        } else if (content.isNotEmpty()) {
            currentMessages.add(ChatMessage.assistant(content))
        }
        
        _messages.value = currentMessages
    }
    
    /**
     * 更新输入文本
     */
    fun updateInputText(text: String) {
        _inputText.value = text
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * 切换提供商
     */
    fun switchProvider(provider: Provider) {
        if (!authUseCase.isAuthenticated(provider)) {
            _error.value = "请先登录 ${provider.displayName}"
            return
        }
        
        _currentProvider.value = provider
        _currentModel.value = AIModel.getDefaultModel(provider.id)
        createNewSession(provider)
    }
    
    /**
     * 切换模型
     */
    fun switchModel(model: AIModel) {
        _currentModel.value = model
    }
    
    /**
     * 设置温度
     */
    fun setTemperature(value: Float) {
        _temperature.value = value
    }
    
    /**
     * 设置是否启用深度思考
     */
    fun setEnableThinking(enabled: Boolean) {
        _enableThinking.value = enabled
    }
    
    /**
     * 设置是否启用联网搜索
     */
    fun setEnableWebSearch(enabled: Boolean) {
        _enableWebSearch.value = enabled
    }
    
    /**
     * 中止当前对话
     */
    fun abort() {
        chatUseCase.abort()
        _isLoading.value = false
    }
    
    /**
     * 删除当前会话
     */
    fun deleteCurrentSession() {
        viewModelScope.launch {
            _currentSession.value?.let { session ->
                sessionUseCase.deleteSession(session.id)
                createNewSession()
            }
        }
    }
    
    /**
     * 检查是否需要登录
     */
    fun needsLogin(): Boolean {
        return _authenticatedProviders.value.isEmpty()
    }
}
