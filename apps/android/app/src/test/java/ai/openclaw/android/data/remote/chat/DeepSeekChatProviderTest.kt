package ai.openclaw.android.data.remote.chat

import ai.openclaw.android.domain.model.*
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.mockwebserver.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class DeepSeekChatProviderTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var provider: DeepSeekChatProvider

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.SECONDS)
            .build()

        provider = DeepSeekChatProvider(okHttpClient)
    }

    @AfterEach
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `chat should return text chunks`() = runTest {
        // Given
        val config = AuthConfig(
            provider = "deepseek",
            cookie = "test-cookie",
            bearerToken = "test-token",
            userAgent = "test-ua"
        )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody("""
                    event: message
                    data: {"choices":[{"delta":{"content":"Hello"}}]}
                    
                    event: message
                    data: {"choices":[{"delta":{"content":" World"}}]}
                    
                    event: message
                    data: [DONE]
                    
                """.trimIndent())
        )

        val messages = listOf(ChatMessage.user("Hi"))

        // When
        provider.chat(messages, config).test {
            // Then
            val chunk1 = awaitItem()
            assertTrue(chunk1 is ChatChunk.Text)
            assertEquals("Hello", (chunk1 as ChatChunk.Text).text)

            val chunk2 = awaitItem()
            assertTrue(chunk2 is ChatChunk.Text)
            assertEquals(" World", (chunk2 as ChatChunk.Text).text)

            val chunk3 = awaitItem()
            assertTrue(chunk3 is ChatChunk.Done)

            awaitComplete()
        }
    }

    @Test
    fun `chat should return error on 401`() = runTest {
        // Given
        val config = AuthConfig(
            provider = "deepseek",
            cookie = "invalid-cookie",
            bearerToken = "invalid-token",
            userAgent = "test-ua"
        )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\":\"Unauthorized\"}")
        )

        val messages = listOf(ChatMessage.user("Hi"))

        // When
        provider.chat(messages, config).test {
            // Then
            val chunk = awaitItem()
            assertTrue(chunk is ChatChunk.Error)
            assertEquals("AUTH_EXPIRED", (chunk as ChatChunk.Error).code)

            awaitComplete()
        }
    }

    @Test
    fun `chat should return error on rate limit`() = runTest {
        // Given
        val config = AuthConfig(
            provider = "deepseek",
            cookie = "test-cookie",
            bearerToken = "test-token",
            userAgent = "test-ua"
        )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("{\"error\":\"Rate limited\"}")
        )

        val messages = listOf(ChatMessage.user("Hi"))

        // When
        provider.chat(messages, config).test {
            // Then
            val chunk = awaitItem()
            assertTrue(chunk is ChatChunk.Error)
            assertEquals("RATE_LIMITED", (chunk as ChatChunk.Error).code)
            assertTrue(chunk.recoverable)

            awaitComplete()
        }
    }

    @Test
    fun `chat should return error on server error`() = runTest {
        // Given
        val config = AuthConfig(
            provider = "deepseek",
            cookie = "test-cookie",
            bearerToken = "test-token",
            userAgent = "test-ua"
        )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\":\"Internal server error\"}")
        )

        val messages = listOf(ChatMessage.user("Hi"))

        // When
        provider.chat(messages, config).test {
            // Then
            val chunk = awaitItem()
            assertTrue(chunk is ChatChunk.Error)
            assertEquals("SERVER_ERROR", (chunk as ChatChunk.Error).code)
            assertTrue(chunk.recoverable)

            awaitComplete()
        }
    }

    @Test
    fun `abort should cancel ongoing request`() = runTest {
        // Given
        val config = AuthConfig(
            provider = "deepseek",
            cookie = "test-cookie",
            bearerToken = "test-token",
            userAgent = "test-ua"
        )

        // 模拟慢响应
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBodyDelay(5, TimeUnit.SECONDS)
                .setBody("event: message\ndata: test\n\n")
        )

        val messages = listOf(ChatMessage.user("Hi"))

        // When
        provider.chat(messages, config).test {
            // 等待一小段时间后中止
            Thread.sleep(100)
            provider.abort()

            // Then
            val chunk = awaitItem()
            assertTrue(chunk is ChatChunk.Aborted || chunk is ChatChunk.Error)
        }
    }

    @Test
    fun `validateConfig should return true for valid config`() = runTest {
        // Given
        val config = AuthConfig(
            provider = "deepseek",
            cookie = "valid-cookie",
            bearerToken = "valid-token",
            userAgent = "test-ua"
        )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"user\":\"test\"}")
        )

        // When
        val result = provider.validateConfig(config)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrDefault(false))
    }

    @Test
    fun `validateConfig should return false for invalid config`() = runTest {
        // Given
        val config = AuthConfig(
            provider = "deepseek",
            cookie = "invalid-cookie",
            bearerToken = "invalid-token",
            userAgent = "test-ua"
        )

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("{\"error\":\"Unauthorized\"}")
        )

        // When
        val result = provider.validateConfig(config)

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrDefault(true))
    }

    @Test
    fun `getModels should return available models`() = runTest {
        // Given
        val config = AuthConfig(
            provider = "deepseek",
            cookie = "test-cookie",
            userAgent = "test-ua"
        )

        // When
        val result = provider.getModels(config)

        // Then
        assertTrue(result.isSuccess)
        val models = result.getOrDefault(emptyList())
        assertTrue(models.contains("deepseek-chat"))
        assertTrue(models.contains("deepseek-reasoner"))
    }

    @Test
    fun `isBusy should return false initially`() {
        assertFalse(provider.isBusy())
    }

    @Test
    fun `provider should return correct provider type`() {
        assertEquals(Provider.DEEPSEEK, provider.provider)
    }
}
