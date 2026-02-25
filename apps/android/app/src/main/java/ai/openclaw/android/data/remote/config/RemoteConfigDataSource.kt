package ai.openclaw.android.data.remote.config

import ai.openclaw.android.domain.model.ModelConfig
import ai.openclaw.android.domain.model.ProviderConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 远程配置数据源
 *
 * 从远程服务器获取最新的模型配置
 */
@Singleton
class RemoteConfigDataSource @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // 配置 URL（可以替换为实际的 CDN 地址）
    // 支持多个备用地址
    private val configUrls = listOf(
        "https://raw.githubusercontent.com/UnkownWorld/testfortk1/main/config/models.json",
        "https://cdn.jsdelivr.net/gh/UnkownWorld/testfortk1/config/models.json"
    )

    /**
     * 获取远程配置
     *
     * @return 配置数据，失败返回 null
     */
    suspend fun fetchConfig(): ModelsConfigResponse? {
        for (url in configUrls) {
            try {
                Timber.d("Fetching config from: $url")
                
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrEmpty()) {
                        val config = json.decodeFromString<ModelsConfigResponse>(body)
                        Timber.d("Successfully fetched config: version=${config.version}")
                        return config
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch config from: $url")
            }
        }

        Timber.e("Failed to fetch config from all URLs")
        return null
    }

    /**
     * 检查是否有更新
     *
     * @param currentVersion 当前版本
     * @return 是否有更新
     */
    suspend fun checkForUpdate(currentVersion: String): Boolean {
        val config = fetchConfig() ?: return false
        return config.version != currentVersion
    }
}

/**
 * 模型配置响应
 */
@Serializable
data class ModelsConfigResponse(
    val version: String,
    @SerialName("updated_at")
    val updatedAt: String,
    val providers: List<ProviderConfigResponse>,
    @SerialName("default_provider")
    val defaultProvider: String,
    @SerialName("default_models")
    val defaultModels: Map<String, String>
)

/**
 * 提供商配置响应
 */
@Serializable
data class ProviderConfigResponse(
    val id: String,
    val displayName: String,
    val loginUrl: String,
    val apiBaseUrl: String,
    val enabled: Boolean = true,
    val models: List<ModelConfigResponse>
)

/**
 * 模型配置响应
 */
@Serializable
data class ModelConfigResponse(
    val id: String,
    val displayName: String,
    val features: List<String>,
    val maxTokens: Int,
    val description: String,
    val enabled: Boolean = true
)
