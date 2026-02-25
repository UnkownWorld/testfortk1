package ai.openclaw.android.data.remote.chat

import ai.openclaw.android.data.remote.pow.DeepSeekPowSolver
import ai.openclaw.android.domain.model.*
import ai.openclaw.android.domain.repository.ChatOptions
import ai.openclaw.android.domain.repository.IChatProvider
import ai.openclaw.android.domain.repository.ToolDefinition
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeepSeek 聊天提供者
 *
 * 实现 DeepSeek Web API 的调用，支持：
 * - 流式响应
 * - PoW 挑战计算
 * - 多轮对话
 * - 联网搜索
 * - R1 深度思考
 */
@Singleton
class DeepSeekChatProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) : IChatProvider {

    override val provider = Provider.DEEPSEEK

    // 中止标志
    private val aborted = AtomicBoolean(false)

    // 当前请求
    private var currentEventSource: EventSource? = null

    // 是否忙碌
    private val isBusyFlag = AtomicBoolean(false)

    // PoW 求解器
    private val powSolver = DeepSeekPowSolver()

    companion object {
        private const val API_URL = "https://chat.deepseek.com/api/v0/chat/completions"
        private const val CHAT_URL = "https://chat.deepseek.com/api/v0/chat"
        private const val MODEL_CHAT = "deepseek-chat"
        private const val MODEL_REASONER = "deepseek-reasoner"
        
        // App 版本（需要定期更新）
        private const val APP_VERSION = "20241129.1"
        private const val CLIENT_VERSION = "1.0.0-always"
    }

    override fun chat(
        messages: List<ChatMessage>,
        config: AuthConfig,
        sessionId: String?,
        options: ChatOptions?
    ): Flow<ChatChunk> = channelFlow {
        aborted.set(false)
        isBusyFlag.set(true)

        try {
            // 1. 获取 PoW 挑战（如果需要）
            val powResponse = fetchPowChallenge(config)
            
            // 2. 构建请求体
            val requestBody = buildRequestBody(messages, sessionId, options)

            // 3. 构建请求头
            val headersBuilder = Headers.Builder()
                .add("Accept", "*/*")
                .add("Accept-Language", "en-US,en;q=0.9")
                .add("Authorization", "Bearer ${config.bearerToken}")
                .add("Content-Type", "application/json")
                .add("Origin", "https://chat.deepseek.com")
                .add("Referer", "https://chat.deepseek.com/")
                .add("User-Agent", config.userAgent)
                .add("x-app-version", APP_VERSION)
                .add("x-client-locale", "en_US")
                .add("x-client-platform", "web")
                .add("x-client-version", CLIENT_VERSION)
            
            // 添加 PoW 响应
            powResponse?.let {
                headersBuilder.add("x-ds-pow-response", it)
            }

            // 4. 构建请求
            val request = Request.Builder()
                .url(CHAT_URL)
                .headers(headersBuilder.build())
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            // 5. 创建 SSE 事件源
            val factory = EventSources.createFactory(okHttpClient)

            val listener = object : EventSourceListener() {
                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    if (aborted.get()) {
                        eventSource.cancel()
                        return
                    }

                    if (data == "[DONE]") {
                        trySend(ChatChunk.done())
                    } else {
                        parseAndSendChunk(data)
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    if (!aborted.get()) {
                        trySend(ChatChunk.done())
                    }
                    isBusyFlag.set(false)
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    Timber.e(t, "DeepSeek SSE connection failed")
                    isBusyFlag.set(false)

                    val errorCode = when (response?.code) {
                        401 -> "AUTH_EXPIRED"
                        429 -> "RATE_LIMITED"
                        500, 502, 503 -> "SERVER_ERROR"
                        else -> "CONNECTION_ERROR"
                    }

                    // 检查是否需要 PoW
                    response?.body?.string()?.let { body ->
                        val powConfig = powSolver.extractChallenge(body)
                        if (powConfig != null) {
                            Timber.d("PoW challenge required")
                            // TODO: 重试带 PoW 的请求
                        }
                    }

                    val recoverable = response?.code == 429 ||
                        response?.code in 500..599

                    trySend(ChatChunk.error(
                        code = errorCode,
                        message = t?.message ?: "Connection failed",
                        recoverable = recoverable
                    ))
                }
            }

            // 6. 开始请求
            currentEventSource = factory.newEventSource(request, listener)

            // 7. 等待完成或中止
            awaitClose {
                aborted.set(true)
                currentEventSource?.cancel()
                isBusyFlag.set(false)
            }
        } catch (e: Exception) {
            Timber.e(e, "DeepSeek chat error")
            isBusyFlag.set(false)
            trySend(ChatChunk.error(
                code = "CHAT_ERROR",
                message = e.message ?: "Unknown error"
            ))
        }
    }

    /**
     * 获取 PoW 挑战
     */
    private suspend fun fetchPowChallenge(config: AuthConfig): String? {
        return withContext(Dispatchers.IO) {
            try {
                // 尝试获取 PoW 挑战
                val request = Request.Builder()
                    .url("https://chat.deepseek.com/api/v0/chat/settings")
                    .header("Authorization", "Bearer ${config.bearerToken}")
                    .header("User-Agent", config.userAgent)
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val body = response.body?.string()
                
                // 解析 PoW 配置
                body?.let {
                    val powConfig = powSolver.extractChallenge(it)
                    powConfig?.let { cfg ->
                        powSolver.solve(cfg)
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch PoW challenge")
                null
            }
        }
    }

    override fun abort() {
        aborted.set(true)
        currentEventSource?.cancel()
        isBusyFlag.set(false)
    }

    override suspend fun validateConfig(config: AuthConfig): Result<Boolean> {
        return try {
            val request = Request.Builder()
                .url("https://chat.deepseek.com/api/v0/users/me")
                .header("Authorization", "Bearer ${config.bearerToken}")
                .header("User-Agent", config.userAgent)
                .header("x-app-version", APP_VERSION)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Timber.e(e, "Config validation failed")
            Result.failure(e)
        }
    }

    override suspend fun getModels(config: AuthConfig): Result<List<String>> {
        return Result.success(listOf(
            MODEL_CHAT,
            MODEL_REASONER,
            "deepseek-search",      // 联网搜索
            "deepseek-think",       // 深度思考
            "deepseek-r1",          // R1 模型
            "deepseek-r1-search"    // R1 + 联网搜索
        ))
    }

    override fun isBusy(): Boolean = isBusyFlag.get()

    /**
     * 构建请求体
     */
    private fun buildRequestBody(
        messages: List<ChatMessage>,
        sessionId: String?,
        options: ChatOptions?
    ): String {
        val json = JSONObject().apply {
            // 模型选择
            val modelName = when (options?.model) {
                "deepseek-reasoner", "deepseek-r1", "deepseek-think" -> MODEL_REASONER
                else -> MODEL_CHAT
            }
            put("model", modelName)
            
            // 消息
            put("messages", JSONArray().apply {
                messages.forEach { msg ->
                    put(JSONObject().apply {
                        put("role", msg.role.name.lowercase())
                        put("content", msg.content)
                    })
                }
            })

            // 会话 ID（用于多轮对话）
            sessionId?.let { put("conversation_id", it) }

            // 流式响应
            put("stream", true)

            // 选项
            options?.let { opt ->
                opt.temperature?.let { put("temperature", it) }
                opt.maxTokens?.let { put("max_tokens", it) }
            }

            // 联网搜索
            if (options?.model?.contains("search") == true) {
                put("search_enabled", true)
            }

            // 深度思考
            if (options?.model?.contains("think") == true || 
                options?.model?.contains("r1") == true) {
                put("thinking_enabled", true)
            }
        }

        return json.toString()
    }

    /**
     * 解析并发送响应块
     */
    private fun parseAndSendChunk(data: String) {
        if (data.isEmpty()) return

        try {
            val json = JSONObject(data)
            
            // 检查错误
            if (json.has("error")) {
                val error = json.getJSONObject("error")
                trySend(ChatChunk.error(
                    code = error.optString("code", "UNKNOWN"),
                    message = error.optString("message", "Unknown error")
                ))
                return
            }

            // 解析选择
            val choices = json.optJSONArray("choices")
            if (choices == null || choices.length() == 0) return

            val choice = choices.getJSONObject(0)
            val delta = choice.optJSONObject("delta") ?: return

            // 文本内容
            val content = delta.optString("content", "")
            if (content.isNotEmpty()) {
                trySend(ChatChunk.text(content))
                return
            }

            // 思考内容（R1 模型）
            val thinking = delta.optString("reasoning_content", "")
            if (thinking.isNotEmpty()) {
                trySend(ChatChunk.thinking(thinking))
                return
            }

            // 工具调用
            val toolCalls = delta.optJSONArray("tool_calls")
            if (toolCalls != null && toolCalls.length() > 0) {
                for (i in 0 until toolCalls.length()) {
                    val tc = toolCalls.getJSONObject(i)
                    val id = tc.optString("id", "")
                    val func = tc.optJSONObject("function")
                    val name = func?.optString("name", "") ?: ""
                    val args = func?.optString("arguments", "") ?: ""

                    trySend(ChatChunk.toolCallDelta(
                        toolCallId = id,
                        toolName = name,
                        arguments = args
                    ))
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse chunk: $data")
        }
    }
}
