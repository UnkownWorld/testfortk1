package ai.openclaw.android.domain.usecase

import ai.openclaw.android.domain.model.*
import ai.openclaw.android.domain.repository.IAuthProvider
import ai.openclaw.android.domain.repository.IChatProviderFactory
import ai.openclaw.android.domain.repository.ISessionRepository
import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class ChatUseCaseTest {

    private lateinit var authProvider: IAuthProvider
    private lateinit var chatProviderFactory: IChatProviderFactory
    private lateinit var sessionRepository: ISessionRepository
    private lateinit var chatUseCase: ChatUseCase

    @BeforeEach
    fun setup() {
        authProvider = mockk()
        chatProviderFactory = mockk()
        sessionRepository = mockk()
        chatUseCase = ChatUseCase(authProvider, chatProviderFactory, sessionRepository)
    }

    @Test
    fun `sendMessage should throw NoActiveSession when no active session`() = runTest {
        // Given
        coEvery { sessionRepository.getActiveSession() } returns flowOf(null)

        // When & Then
        assertThrows<ChatException.NoActiveSession> {
            chatUseCase.sendMessage("Hello").collect {}
        }
    }

    @Test
    fun `sendMessage should throw NotAuthenticated when not authenticated`() = runTest {
        // Given
        val session = Session.create("deepseek")
        coEvery { sessionRepository.getActiveSession() } returns flowOf(session)
        coEvery { authProvider.getAuthConfig(Provider.DEEPSEEK) } returns null

        // When & Then
        assertThrows<ChatException.NotAuthenticated> {
            chatUseCase.sendMessage("Hello").collect {}
        }
    }

    @Test
    fun `sendMessage should throw AuthExpired when config is expired`() = runTest {
        // Given
        val session = Session.create("deepseek")
        val expiredConfig = AuthConfig(
            provider = "deepseek",
            cookie = "expired-cookie",
            userAgent = "test-ua",
            expiresAt = System.currentTimeMillis() - 1000 // 已过期
        )
        coEvery { sessionRepository.getActiveSession() } returns flowOf(session)
        coEvery { authProvider.getAuthConfig(Provider.DEEPSEEK) } returns expiredConfig

        // When & Then
        assertThrows<ChatException.AuthExpired> {
            chatUseCase.sendMessage("Hello").collect {}
        }
    }

    @Test
    fun `sendMessage should return text chunks on success`() = runTest {
        // Given
        val session = Session.create("deepseek")
        val authConfig = AuthConfig(
            provider = "deepseek",
            cookie = "test-cookie",
            bearerToken = "test-token",
            userAgent = "test-ua"
        )
        val chatProvider = mockk<IChatProvider>()

        coEvery { sessionRepository.getActiveSession() } returns flowOf(session)
        coEvery { authProvider.getAuthConfig(Provider.DEEPSEEK) } returns authConfig
        coEvery { sessionRepository.saveSession(any()) } just Runs
        every { chatProviderFactory.getProvider(Provider.DEEPSEEK) } returns chatProvider
        every { chatProvider.chat(any(), any(), any(), any()) } returns flowOf(
            ChatChunk.text("Hello"),
            ChatChunk.text(" World"),
            ChatChunk.done()
        )

        // When
        val result = chatUseCase.sendMessage("Hi").toList()

        // Then
        assertEquals(3, result.size)
        assertTrue(result[0] is ChatChunk.Text)
        assertTrue(result[1] is ChatChunk.Text)
        assertTrue(result[2] is ChatChunk.Done)

        // Verify session was saved
        coVerify { sessionRepository.saveSession(any()) }
    }

    @Test
    fun `sendMessage should handle rate limited error`() = runTest {
        // Given
        val session = Session.create("deepseek")
        val authConfig = AuthConfig(
            provider = "deepseek",
            cookie = "test-cookie",
            bearerToken = "test-token",
            userAgent = "test-ua"
        )
        val chatProvider = mockk<IChatProvider>()

        coEvery { sessionRepository.getActiveSession() } returns flowOf(session)
        coEvery { authProvider.getAuthConfig(Provider.DEEPSEEK) } returns authConfig
        coEvery { sessionRepository.saveSession(any()) } just Runs
        every { chatProviderFactory.getProvider(Provider.DEEPSEEK) } returns chatProvider
        every { chatProvider.chat(any(), any(), any(), any()) } returns flowOf(
            ChatChunk.error("RATE_LIMITED", "Too many requests", recoverable = true)
        )

        // When
        chatUseCase.sendMessage("Hi").test {
            // Then
            val chunk = awaitItem()
            assertTrue(chunk is ChatChunk.Error)
            assertEquals("RATE_LIMITED", (chunk as ChatChunk.Error).code)
            assertTrue(chunk.recoverable)
            
            awaitComplete()
        }
    }

    @Test
    fun `abort should call abort on all providers`() {
        // Given
        val chatProvider = mockk<IChatProvider>(relaxed = true)
        every { chatProviderFactory.hasProvider(any()) } returns true
        every { chatProviderFactory.getProvider(any()) } returns chatProvider

        // When
        chatUseCase.abort()

        // Then
        verify(exactly = Provider.values().size) { chatProvider.abort() }
    }

    @Test
    fun `sendMessage with session should use provided session`() = runTest {
        // Given
        val session = Session.create("claude")
        val authConfig = AuthConfig(
            provider = "claude",
            cookie = "test-cookie",
            userAgent = "test-ua"
        )
        val chatProvider = mockk<IChatProvider>()

        coEvery { authProvider.getAuthConfig(Provider.CLAUDE) } returns authConfig
        coEvery { sessionRepository.saveSession(any()) } just Runs
        every { chatProviderFactory.getProvider(Provider.CLAUDE) } returns chatProvider
        every { chatProvider.chat(any(), any(), any(), any()) } returns flowOf(
            ChatChunk.text("Response"),
            ChatChunk.done()
        )

        // When
        val result = chatUseCase.sendMessage("Hi", session).toList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result[0] is ChatChunk.Text)
        assertTrue(result[1] is ChatChunk.Done)
    }
}
