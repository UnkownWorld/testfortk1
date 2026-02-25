package ai.openclaw.android.data.remote.chat

import ai.openclaw.android.domain.model.*
import ai.openclaw.android.domain.repository.ChatOptions
import ai.openclaw.android.domain.repository.IChatProvider
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
 * 豆包聊天提供者
 *
 * 实现豆包（Doubao）API 的调用
 */
@Singleton
class DoubaoChatProvider @Inject constructor(
    private val okHttpClient: OkHttpClient
) : IChatProvider {

    override val provider = Provider.DOUBAO

    // 中止标志
    private val aborted = AtomicBoolean(false)

    // 当前请求
    private var currentEventSource: EventSource? = null

    // 是否忙碌
    private val isBusyFlag = AtomicBoolean(false)

    companion object {
        private const val API_URL = "https://www.doubao.com/api/chat"
        private const val MODEL = "doubao-pro-32k"
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
            // 构建请求体
            val requestBody = buildRequestBody(messages, sessionId, options)

            // 构建请求
            val request = Request.Builder()
                .url(API_URL)
                .header("Cookie", config.cookie)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("User-Agent", config.userAgent)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            // 创建 SSE 事件源
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
                    Timber.e(t, "Doubao SSE connection failed")
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

            // 开始请求
            currentEventSource = factory.newEventSource(request, listener)

            // 等待完成或中止
            awaitClose {
                aborted.set(true)
                currentEventSource?.cancel()
                isBusyFlag.set(false)
            }
        } catch (e: Exception) {
            Timber.e(e, "Doubao chat error")
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
                .url("https://www.doubao.com/api/user")
                .header("Cookie", config.cookie)
                .header("User-Agent", config.userAgent)
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Timber.e(e, "Doubao config validation failed")
            Result.failure(e)
        }
    }

    override suspend fun getModels(config: AuthConfig): Result<List<String>> {
        return Result.success(listOf(MODEL))
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

            // 选项
            options?.let { opt ->
                opt.temperature?.let { put("temperature", it) }
                opt.maxTokens?.let { put("max_tokens", it) }
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
            val choices = json.optJSONArray("choices")
            if (choices == null || choices.length() == 0) return

            val choice = choices.getJSONObject(0)
            val delta = choice.optJSONObject("delta") ?: return

            val content = delta.optString("content", "")
            if (content.isNotEmpty()) {
                trySend(ChatChunk.text(content))
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse Doubao chunk: $data")
        }
    }
}
