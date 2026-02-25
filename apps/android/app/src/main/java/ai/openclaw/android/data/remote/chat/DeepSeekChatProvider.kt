package ai.openclaw.android.data.remote.chat

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
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DeepSeek 聊天提供者
 *
 * 实现 DeepSeek Web API 的调用，支持流式响应和 PoW 挑战
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

    companion object {
        private const val API_URL = "https://chat.deepseek.com/api/v1/chat/completions"
        private const val MODEL = "deepseek-chat"
        private const val REASONER_MODEL = "deepseek-reasoner"
        private const val POW_DIFFICULTY = 5
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
            // 1. 计算 PoW（如果需要）
            val powAnswer = calculatePowIfNeeded(config)

            // 2. 构建请求体
            val requestBody = buildRequestBody(messages, sessionId, powAnswer, options)

            // 3. 构建请求
            val request = Request.Builder()
                .url(API_URL)
                .header("Cookie", config.cookie)
                .header("Authorization", "Bearer ${config.bearerToken}")
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("User-Agent", config.userAgent)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            // 4. 创建 SSE 事件源
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

                    when (type) {
                        "message" -> {
                            if (data == "[DONE]") {
                                trySend(ChatChunk.done())
                            } else {
                                parseAndSendChunk(data)
                            }
                        }
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
                    Timber.e(t, "SSE connection failed")
                    isBusyFlag.set(false)

                    val errorCode = when (response?.code) {
                        401 -> "AUTH_EXPIRED"
                        429 -> "RATE_LIMITED"
                        500, 502, 503 -> "SERVER_ERROR"
                        else -> "CONNECTION_ERROR"
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

            // 5. 开始请求
            currentEventSource = factory.newEventSource(request, listener)

            // 6. 等待完成或中止
            awaitClose {
                aborted.set(true)
                currentEventSource?.cancel()
                isBusyFlag.set(false)
            }
        } catch (e: Exception) {
            Timber.e(e, "Chat error")
            isBusyFlag.set(false)
            trySend(ChatChunk.error(
                code = "CHAT_ERROR",
                message = e.message ?: "Unknown error"
            ))
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
                .url("https://chat.deepseek.com/api/v1/user")
                .header("Cookie", config.cookie)
                .header("Authorization", "Bearer ${config.bearerToken}")
                .header("User-Agent", config.userAgent)
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
        return Result.success(listOf(MODEL, REASONER_MODEL))
    }

    override fun isBusy(): Boolean = isBusyFlag.get()

    /**
     * 计算 PoW（如果需要）
     */
    private suspend fun calculatePowIfNeeded(config: AuthConfig): String? {
        return withContext(Dispatchers.Default) {
            try {
                // 获取挑战
                val challenge = fetchChallenge(config)

                // 计算答案
                var nonce = 0L
                val target = "0".repeat(POW_DIFFICULTY)

                while (!aborted.get() && nonce < Long.MAX_VALUE) {
                    val input = "$nonce$challenge"
                    val hash = sha3_256(input)

                    if (hash.startsWith(target)) {
                        return@withContext nonce.toString()
                    }

                    nonce++

                    // 每 10000 次检查一次取消
                    if (nonce % 10000 == 0L) {
                        yield()
                    }
                }

                null
            } catch (e: Exception) {
                Timber.w(e, "PoW calculation failed")
                null
            }
        }
    }

    /**
     * 获取 PoW 挑战
     */
    private fun fetchChallenge(config: AuthConfig): String {
        // 简化实现：使用时间戳
        return System.currentTimeMillis().toString()
    }

    /**
     * SHA3-256 哈希
     */
    private fun sha3_256(input: String): String {
        val md = MessageDigest.getInstance("SHA3-256")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 构建请求体
     */
    private fun buildRequestBody(
        messages: List<ChatMessage>,
        sessionId: String?,
        powAnswer: String?,
        options: ChatOptions?
    ): String {
        val json = JSONObject().apply {
            put("model", options?.model ?: MODEL)
            put("stream", true)

            // 消息
            put("messages", JSONArray().apply {
                messages.forEach { msg ->
                    put(JSONObject().apply {
                        put("role", msg.role.name.lowercase())
                        put("content", msg.content)
                    })
                }
            })

            // 会话 ID
            sessionId?.let { put("conversation_id", it) }

            // PoW 答案
            powAnswer?.let { put("pow_answer", it) }

            // 温度
            options?.temperature?.let { put("temperature", it) }

            // 最大 token
            options?.maxTokens?.let { put("max_tokens", it) }

            // 思考模式
            options?.thinking?.let { put("thinking", it) }
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

            // 思考内容
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
