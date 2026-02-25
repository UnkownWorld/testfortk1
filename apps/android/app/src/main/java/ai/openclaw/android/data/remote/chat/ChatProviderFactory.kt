package ai.openclaw.android.data.remote.chat

import ai.openclaw.android.domain.model.Provider
import ai.openclaw.android.domain.repository.IChatProvider
import ai.openclaw.android.domain.repository.IChatProviderFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天提供者工厂
 *
 * 管理和创建聊天提供者实例
 */
@Singleton
class ChatProviderFactory @Inject constructor(
    private val deepSeekChatProvider: DeepSeekChatProvider,
    private val claudeChatProvider: ClaudeChatProvider,
    private val doubaoChatProvider: DoubaoChatProvider
) : IChatProviderFactory {

    private val providers = mutableMapOf<Provider, IChatProvider>()

    init {
        // 注册内置提供者
        registerProvider(deepSeekChatProvider)
        registerProvider(claudeChatProvider)
        registerProvider(doubaoChatProvider)
    }

    override fun getProvider(provider: Provider): IChatProvider {
        return providers[provider]
            ?: throw IllegalArgumentException("Provider not registered: ${provider.id}")
    }

    override fun registerProvider(provider: IChatProvider) {
        providers[provider.provider] = provider
    }

    override fun hasProvider(provider: Provider): Boolean {
        return providers.containsKey(provider)
    }

    override fun getRegisteredProviders(): List<Provider> {
        return providers.keys.toList()
    }
}
