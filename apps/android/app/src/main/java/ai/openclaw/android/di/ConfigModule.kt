package ai.openclaw.android.di

import ai.openclaw.android.data.local.ConfigLocalDataSource
import ai.openclaw.android.data.remote.config.RemoteConfigDataSource
import ai.openclaw.android.data.repository.ModelConfigRepository
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * 配置模块
 */
@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    fun provideRemoteConfigDataSource(): RemoteConfigDataSource {
        return RemoteConfigDataSource()
    }

    @Provides
    @Singleton
    fun provideConfigLocalDataSource(
        @ApplicationContext context: Context,
        json: Json
    ): ConfigLocalDataSource {
        return ConfigLocalDataSource(context, json)
    }

    @Provides
    @Singleton
    fun provideModelConfigRepository(
        remoteDataSource: RemoteConfigDataSource,
        localDataSource: ConfigLocalDataSource
    ): ModelConfigRepository {
        return ModelConfigRepository(remoteDataSource, localDataSource)
    }
}
