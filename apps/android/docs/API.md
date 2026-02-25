# OpenClaw Android API 文档

## 目录

- [认证 API](#认证-api)
- [聊天 API](#聊天-api)
- [会话 API](#会话-api)
- [模型 API](#模型-api)
- [数据模型](#数据模型)

---

## 认证 API

### IAuthProvider

认证提供者接口，管理用户认证状态。

#### 方法

##### getAuthState

```kotlin
fun getAuthState(provider: Provider): Flow<AuthState>
```

获取指定提供商的认证状态流。

**参数：**
- `provider`: AI 提供商

**返回：**
- `Flow<AuthState>`: 认证状态流

---

##### isAuthenticated

```kotlin
suspend fun isAuthenticated(provider: Provider): Boolean
```

检查是否已认证。

**参数：**
- `provider`: AI 提供商

**返回：**
- `Boolean`: 是否已认证

---

##### getAuthConfig

```kotlin
suspend fun getAuthConfig(provider: Provider): AuthConfig?
```

获取认证配置。

**参数：**
- `provider`: AI 提供商

**返回：**
- `AuthConfig?`: 认证配置，未认证返回 null

---

##### authenticate

```kotlin
fun authenticate(provider: Provider): Flow<AuthState>
```

开始认证流程。

**参数：**
- `provider`: AI 提供商

**返回：**
- `Flow<AuthState>`: 认证状态流

**示例：**

```kotlin
authProvider.authenticate(Provider.DEEPSEEK)
    .collect { state ->
        when (state) {
            is AuthState.Authenticating -> // 认证中
            is AuthState.Authenticated -> // 认证成功
            is AuthState.Error -> // 认证失败
            is AuthState.Cancelled -> // 已取消
            else -> {}
        }
    }
```

---

##### logout

```kotlin
suspend fun logout(provider: Provider)
```

登出指定提供商。

**参数：**
- `provider`: AI 提供商

---

## 聊天 API

### IChatProvider

聊天提供者接口，处理与 AI 的对话。

#### 方法

##### chat

```kotlin
fun chat(
    messages: List<ChatMessage>,
    config: AuthConfig,
    sessionId: String? = null,
    options: ChatOptions? = null
): Flow<ChatChunk>
```

发送聊天消息。

**参数：**
- `messages`: 消息历史
- `config`: 认证配置
- `sessionId`: 会话 ID（可选）
- `options`: 聊天选项（可选）

**返回：**
- `Flow<ChatChunk>`: 响应块流

**示例：**

```kotlin
val messages = listOf(
    ChatMessage.user("你好")
)

chatProvider.chat(messages, authConfig)
    .collect { chunk ->
        when (chunk) {
            is ChatChunk.Text -> print(chunk.text)
            is ChatChunk.Done -> println("完成")
            is ChatChunk.Error -> println("错误: ${chunk.message}")
            else -> {}
        }
    }
```

---

##### abort

```kotlin
fun abort()
```

中止当前请求。

---

##### validateConfig

```kotlin
suspend fun validateConfig(config: AuthConfig): Result<Boolean>
```

验证认证配置是否有效。

**参数：**
- `config`: 认证配置

**返回：**
- `Result<Boolean>`: 验证结果

---

### ChatOptions

聊天选项配置。

```kotlin
data class ChatOptions(
    val model: String? = null,           // 模型 ID
    val temperature: Double? = null,     // 温度 (0-2)
    val maxTokens: Int? = null,          // 最大 token 数
    val thinking: Boolean? = null,       // 启用深度思考
    val stream: Boolean = true,          // 流式响应
    val tools: List<ToolDefinition>? = null  // 工具列表
)
```

---

## 会话 API

### ISessionRepository

会话仓库接口，管理聊天会话。

#### 方法

##### getAllSessions

```kotlin
fun getAllSessions(): Flow<List<Session>>
```

获取所有会话。

**返回：**
- `Flow<List<Session>>`: 会话列表流

---

##### getSession

```kotlin
suspend fun getSession(sessionId: String): Session?
```

获取指定会话。

**参数：**
- `sessionId`: 会话 ID

**返回：**
- `Session?`: 会话，不存在返回 null

---

##### saveSession

```kotlin
suspend fun saveSession(session: Session)
```

保存会话。

**参数：**
- `session`: 会话

---

##### deleteSession

```kotlin
suspend fun deleteSession(sessionId: String)
```

删除会话。

**参数：**
- `sessionId`: 会话 ID

---

##### createSession

```kotlin
suspend fun createSession(provider: String): Session
```

创建新会话。

**参数：**
- `provider`: 提供商 ID

**返回：**
- `Session`: 新会话

---

## 数据模型

### AuthConfig

认证配置。

```kotlin
data class AuthConfig(
    val provider: String,          // 提供商 ID
    val cookie: String,            // Cookie
    val bearerToken: String? = null,  // Bearer Token
    val userAgent: String,         // User Agent
    val capturedAt: Long,          // 捕获时间
    val expiresAt: Long? = null    // 过期时间
)
```

#### 方法

```kotlin
fun isExpired(): Boolean    // 检查是否已过期
fun isValid(): Boolean      // 检查是否有效
```

---

### AuthState

认证状态密封类。

```kotlin
sealed class AuthState {
    object NotAuthenticated : AuthState()
    data class Authenticating(val provider: Provider) : AuthState()
    data class Authenticated(val config: AuthConfig) : AuthState()
    data class Error(val code: String, val message: String) : AuthState()
    object Cancelled : AuthState()
}
```

---

### ChatMessage

聊天消息。

```kotlin
data class ChatMessage(
    val id: String,
    val role: Role,
    val content: String,
    val timestamp: Long,
    val attachments: List<Attachment>
) {
    enum class Role {
        USER, ASSISTANT, SYSTEM, TOOL
    }
}
```

#### 静态方法

```kotlin
fun user(content: String): ChatMessage
fun assistant(content: String): ChatMessage
fun system(content: String): ChatMessage
```

---

### ChatChunk

聊天响应块密封类。

```kotlin
sealed class ChatChunk {
    data class Text(val text: String) : ChatChunk()
    data class Thinking(val text: String) : ChatChunk()
    data class Done : ChatChunk()
    data class Error(val code: String, val message: String) : ChatChunk()
    data class Aborted : ChatChunk()
}
```

---

### Session

会话。

```kotlin
data class Session(
    val id: String,
    val provider: String,
    val title: String?,
    val messages: List<ChatMessage>,
    val createdAt: Long,
    val updatedAt: Long
)
```

#### 方法

```kotlin
fun addMessage(message: ChatMessage): Session
fun getContextMessages(maxCount: Int = 20): List<ChatMessage>
fun clearMessages(): Session
```

---

### Provider

AI 提供商枚举。

```kotlin
enum class Provider(
    val id: String,
    val displayName: String,
    val loginUrl: String,
    val apiBaseUrl: String
) {
    DEEPSEEK("deepseek", "DeepSeek", ...),
    CLAUDE("claude", "Claude", ...),
    DOUBAO("doubao", "豆包", ...)
}
```

---

## 完整示例

### 发送聊天消息

```kotlin
fun sendMessage(text: String) {
    viewModelScope.launch {
        chatUseCase.sendMessage(text).collect { chunk ->
            when (chunk) {
                is ChatChunk.Text -> updateUI(chunk.text)
                is ChatChunk.Done -> finishMessage()
                is ChatChunk.Error -> showError(chunk.message)
                else -> {}
            }
        }
    }
}
```

### 认证流程

```kotlin
fun login(provider: Provider) {
    viewModelScope.launch {
        authUseCase.authenticate(provider).collect { state ->
            _authState.value = state
        }
    }
}
```
