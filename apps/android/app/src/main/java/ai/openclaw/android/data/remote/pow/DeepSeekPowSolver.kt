package ai.openclaw.android.data.remote.pow

import timber.log.Timber
import java.security.MessageDigest
import java.util.Base64
import org.json.JSONObject

/**
 * DeepSeek PoW 挑战求解器
 *
 * 实现 DeepSeek 的 Proof of Work 挑战计算
 * 
 * PoW 挑战格式：
 * - algorithm: 算法名称（如 "SHA3-256"）
 * - challenge: 挑战字符串
 * - salt: 盐值
 * - difficulty: 难度（前导零数量）
 * - expire_at: 过期时间戳
 * - signature: 签名
 * - target_path: 目标路径
 */
class DeepSeekPowSolver {

    /**
     * PoW 配置
     */
    data class PowConfig(
        val algorithm: String,
        val challenge: String,
        val salt: String,
        val difficulty: Int,
        val expireAt: Long,
        val signature: String,
        val targetPath: String
    )

    /**
     * 求解 PoW 挑战
     *
     * @param config PoW 配置
     * @param maxIterations 最大迭代次数
     * @return PoW 响应字符串（Base64 编码）
     */
    fun solve(config: PowConfig, maxIterations: Long = 10_000_000L): String? {
        return try {
            Timber.d("Solving PoW challenge: algorithm=${config.algorithm}, difficulty=${config.difficulty}")
            
            val prefix = "${config.salt}_${config.expireAt}_"
            val target = "0".repeat(config.difficulty)
            
            var nonce = 0L
            var answer: Long? = null
            
            while (nonce < maxIterations && answer == null) {
                val input = "$prefix$nonce${config.challenge}"
                val hash = calculateHash(config.algorithm, input)
                
                if (hash.startsWith(target)) {
                    answer = nonce
                    Timber.d("PoW solved! nonce=$nonce, hash=${hash.take(20)}...")
                }
                
                nonce++
                
                // 每 10000 次检查一次
                if (nonce % 10000 == 0L) {
                    Thread.yield()
                }
            }
            
            if (answer != null) {
                buildResponse(config, answer)
            } else {
                Timber.w("PoW not solved within $maxIterations iterations")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to solve PoW challenge")
            null
        }
    }

    /**
     * 从 JSON 解析 PoW 配置
     */
    fun parseConfig(json: String): PowConfig? {
        return try {
            val obj = JSONObject(json)
            PowConfig(
                algorithm = obj.getString("algorithm"),
                challenge = obj.getString("challenge"),
                salt = obj.getString("salt"),
                difficulty = obj.getInt("difficulty"),
                expireAt = obj.getLong("expire_at"),
                signature = obj.getString("signature"),
                targetPath = obj.getString("target_path")
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse PoW config")
            null
        }
    }

    /**
     * 从 API 响应获取 PoW 挑战
     */
    fun extractChallenge(responseBody: String): PowConfig? {
        return try {
            val obj = JSONObject(responseBody)
            if (obj.has("code") && obj.getString("code") == "pow_required") {
                val data = obj.getJSONObject("data")
                parseConfig(data.toString())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 计算哈希
     */
    private fun calculateHash(algorithm: String, input: String): String {
        return when (algorithm.uppercase()) {
            "SHA3-256", "SHA3_256", "SHA3" -> {
                val md = MessageDigest.getInstance("SHA3-256")
                val digest = md.digest(input.toByteArray(Charsets.UTF_8))
                digest.joinToString("") { "%02x".format(it) }
            }
            "SHA-256", "SHA256" -> {
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(input.toByteArray(Charsets.UTF_8))
                digest.joinToString("") { "%02x".format(it) }
            }
            else -> {
                // 默认使用 SHA3-256
                val md = MessageDigest.getInstance("SHA3-256")
                val digest = md.digest(input.toByteArray(Charsets.UTF_8))
                digest.joinToString("") { "%02x".format(it) }
            }
        }
    }

    /**
     * 构建 PoW 响应
     */
    private fun buildResponse(config: PowConfig, answer: Long): String {
        val result = JSONObject().apply {
            put("algorithm", config.algorithm)
            put("challenge", config.challenge)
            put("salt", config.salt)
            put("answer", answer)
            put("signature", config.signature)
            put("target_path", config.targetPath)
        }
        
        return Base64.getEncoder().encodeToString(result.toString().toByteArray())
    }

    companion object {
        /**
         * 默认难度
         */
        const val DEFAULT_DIFFICULTY = 5

        /**
         * 最大迭代次数
         */
        const val MAX_ITERATIONS = 10_000_000L
    }
}
