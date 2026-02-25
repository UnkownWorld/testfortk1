package ai.openclaw.android.di

import ai.openclaw.android.domain.usecase.AuthUseCase
import ai.openclaw.android.domain.usecase.ChatUseCase
import ai.openclaw.android.domain.usecase.SessionUseCase
import ai.openclaw.android.ui.viewmodel.AuthViewModel
import ai.openclaw.android.ui.viewmodel.ChatViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * ViewModel 模块
 */
@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {

    @Provides
    @ViewModelScoped
    fun provideAuthViewModel(
        authUseCase: AuthUseCase
    ): AuthViewModel {
        return AuthViewModel(authUseCase)
    }

    @Provides
    @ViewModelScoped
    fun provideChatViewModel(
        chatUseCase: ChatUseCase,
        sessionUseCase: SessionUseCase,
        authUseCase: AuthUseCase
    ): ChatViewModel {
        return ChatViewModel(chatUseCase, sessionUseCase, authUseCase)
    }
}
