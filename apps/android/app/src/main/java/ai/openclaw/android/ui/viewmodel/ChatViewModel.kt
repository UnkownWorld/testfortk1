package ai.openclaw.android.ui.viewmodel

import ai.openclaw.android.domain.model.*
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
 *
 * 管理聊天状态和操作
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
    
    // 已认证的提供商列表
    private val _authenticatedProviders = MutableStateFlow<List<Provider>>(emptyList())
    val authenticatedProviders: StateFlow<List<Provider>> = _authenticatedProviders.asStateFlow()
    
    init {
        // 加载已认证的提供商
        loadAuthenticatedProviders()
        
        // 加载或创建会话
        loadOrCreateSession()
    }
    
    /**
     * 加载已认证的提供商
     */
    private fun loadAuthenticatedProviders() {
        viewModelScope.launch {
            val providers = authUseCase.getAuthenticatedProviders()
            _authenticatedProviders.value = providers
            
            // 设置当前提供商
            if (providers.isNotEmpty() && _currentProvider.value == null) {
                _currentProvider.value = providers.first()
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
                Timber.d("Loaded active session: ${activeSession.id}")
            } else {
                // 创建新会话
                val provider = _currentProvider.value ?: 
                    _authenticatedProviders.value.firstOrNull()
                
                if (provider != null) {
                    createNewSession(provider)
                }
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
            
            try {
                chatUseCase.sendMessage(text, session).collect { chunk ->
                    when (chunk) {
                        is ChatChunk.Text -> {
                            assistantContent.append(chunk.text)
                            // 更新消息列表
                            updateAssistantMessage(assistantContent.toString())
                        }
                        is ChatChunk.Thinking -> {
                            // TODO: 显示思考过程
                        }
                        is ChatChunk.Done -> {
                            _isLoading.value = false
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
     * 更新助手消息
     */
    private fun updateAssistantMessage(content: String) {
        val currentMessages = _messages.value.toMutableList()
        val lastAssistantIndex = currentMessages.indexOfLast { 
            it.role == ChatMessage.Role.ASSISTANT 
        }
        
        if (lastAssistantIndex >= 0 && 
            currentMessages[lastAssistantIndex].id.startsWith("streaming_")) {
            // 更新现有消息
            currentMessages[lastAssistantIndex] = currentMessages[lastAssistantIndex].copy(
                content = content
            )
        } else {
            // 添加新消息
            currentMessages.add(ChatMessage.assistant(content).copy(
                id = "streaming_${System.currentTimeMillis()}"
            ))
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
        
        // 创建新会话
        createNewSession(provider)
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
