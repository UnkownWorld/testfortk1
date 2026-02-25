package ai.openclaw.android.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 用例模块
 *
 * UseCase 通过 @Inject 构造函数自动提供
 * 此模块用于未来扩展
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    // UseCase 类通过 @Inject 注解自动提供
    // 如需自定义提供方式，可在此添加 @Provides 方法
}
