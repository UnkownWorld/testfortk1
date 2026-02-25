package ai.openclaw.android.di

import ai.openclaw.android.ui.viewmodel.AuthViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import ai.openclaw.android.domain.usecase.AuthUseCase

/**
 * ViewModel 模块
 *
 * 提供 ViewModel 实例
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
}
