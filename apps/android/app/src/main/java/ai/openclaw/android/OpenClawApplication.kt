package ai.openclaw.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * 应用类
 *
 * 初始化 Hilt 和其他全局组件
 */
@HiltAndroidApp
class OpenClawApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化 Timber 日志
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.d("OpenClaw Application started")
    }
}
