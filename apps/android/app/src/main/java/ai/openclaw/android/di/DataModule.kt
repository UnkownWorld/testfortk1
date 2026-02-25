package ai.openclaw.android.di

import ai.openclaw.android.data.local.AuthLocalDataSource
import ai.openclaw.android.data.local.SessionLocalDataSource
import ai.openclaw.android.data.remote.AuthRemoteDataSource
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * 数据模块
 *
 * 提供数据源相关的依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAuthLocalDataSource(
        @ApplicationContext context: Context,
        json: Json
    ): AuthLocalDataSource {
        return AuthLocalDataSource(context, json)
    }

    @Provides
    @Singleton
    fun provideSessionLocalDataSource(
        @ApplicationContext context: Context,
        json: Json
    ): SessionLocalDataSource {
        return SessionLocalDataSource(context, json)
    }

    @Provides
    @Singleton
    fun provideAuthRemoteDataSource(
        okHttpClient: OkHttpClient
    ): AuthRemoteDataSource {
        return AuthRemoteDataSource(okHttpClient)
    }
}
