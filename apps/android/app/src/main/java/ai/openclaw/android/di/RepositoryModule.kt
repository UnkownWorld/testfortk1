package ai.openclaw.android.di

import ai.openclaw.android.data.repository.AuthProviderImpl
import ai.openclaw.android.data.repository.SessionRepositoryImpl
import ai.openclaw.android.data.remote.chat.ChatProviderFactory
import ai.openclaw.android.domain.repository.IAuthProvider
import ai.openclaw.android.domain.repository.IChatProviderFactory
import ai.openclaw.android.domain.repository.ISessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 仓库模块
 *
 * 提供仓库接口的实现绑定
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthProvider(
        impl: AuthProviderImpl
    ): IAuthProvider

    @Binds
    @Singleton
    abstract fun bindChatProviderFactory(
        impl: ChatProviderFactory
    ): IChatProviderFactory

    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: SessionRepositoryImpl
    ): ISessionRepository
}
