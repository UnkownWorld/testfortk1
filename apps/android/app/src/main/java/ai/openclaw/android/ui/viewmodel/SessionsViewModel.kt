package ai.openclaw.android.ui.viewmodel

import ai.openclaw.android.domain.model.SessionListItem
import ai.openclaw.android.domain.usecase.SessionUseCase
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 会话历史 ViewModel
 */
@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val sessionUseCase: SessionUseCase
) : ViewModel() {
    
    // 会话列表
    private val _sessions = MutableStateFlow<List<SessionListItem>>(emptyList())
    val sessions: StateFlow<List<SessionListItem>> = _sessions.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 搜索关键词
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    init {
        loadSessions()
    }
    
    /**
     * 加载会话列表
     */
    private fun loadSessions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                sessionUseCase.getSessionListItems()
                    .collect { sessions ->
                        _sessions.value = sessions
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load sessions")
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 搜索会话
     */
    fun search(query: String) {
        _searchQuery.value = query
        
        viewModelScope.launch {
            if (query.isBlank()) {
                loadSessions()
            } else {
                val results = sessionUseCase.searchSessions(query)
                _sessions.value = results
            }
        }
    }
    
    /**
     * 删除会话
     */
    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                sessionUseCase.deleteSession(sessionId)
                Timber.d("Deleted session: $sessionId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete session: $sessionId")
            }
        }
    }
    
    /**
     * 清空所有会话
     */
    fun clearAllSessions() {
        viewModelScope.launch {
            try {
                sessionUseCase.clearAllSessions()
                _sessions.value = emptyList()
                Timber.d("Cleared all sessions")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear all sessions")
            }
        }
    }
}
