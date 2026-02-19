package net.easecation.bedrockloader.sync.server

import net.easecation.bedrockloader.sync.common.PackEncryption
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Challenge-Response 握手管理器
 *
 * 管理 challenge 的生成、验证和过期清理。
 * 每个 challenge 只能使用一次，有效期 30 秒。
 */
class ChallengeManager(private val derivedSharedSecret: String) {

    private val logger = LoggerFactory.getLogger("BedrockLoader/ChallengeManager")

    companion object {
        /** Challenge 有效期（毫秒） */
        const val CHALLENGE_TTL_MS = 30_000L

        /** 清理间隔（毫秒）- 每 60 秒清理一次过期 challenge */
        const val CLEANUP_INTERVAL_MS = 60_000L
    }

    data class ChallengeRecord(
        val challenge: String,
        val createdAt: Long,
        @Volatile var used: Boolean = false
    )

    private val challenges = ConcurrentHashMap<String, ChallengeRecord>()
    private var lastCleanup = System.currentTimeMillis()

    /**
     * 创建一个新的 challenge
     * @return challenge 字符串和过期时间
     */
    fun createChallenge(): Pair<String, Long> {
        // 触发清理（非阻塞）
        maybeCleanup()

        val challenge = PackEncryption.generateChallenge()
        val now = System.currentTimeMillis()
        val expiresAt = now + CHALLENGE_TTL_MS

        challenges[challenge] = ChallengeRecord(
            challenge = challenge,
            createdAt = now
        )

        logger.debug("Challenge created: ${challenge.substring(0, 8)}... (expires in ${CHALLENGE_TTL_MS / 1000}s)")
        return Pair(challenge, expiresAt)
    }

    /**
     * 验证 HMAC 并消费 challenge（一次性使用）
     *
     * @param challenge 客户端提交的 challenge
     * @param filename 目标文件名
     * @param hmac 客户端计算的 HMAC
     * @return true 如果验证通过
     */
    fun verifyAndConsume(challenge: String, filename: String, hmac: String): Boolean {
        val record = challenges[challenge]

        // 1. challenge 不存在
        if (record == null) {
            logger.warn("Challenge not found: ${challenge.take(8)}...")
            return false
        }

        // 2. 已使用
        if (record.used) {
            logger.warn("Challenge already used: ${challenge.take(8)}...")
            challenges.remove(challenge)
            return false
        }

        // 3. 已过期
        val now = System.currentTimeMillis()
        if (now - record.createdAt > CHALLENGE_TTL_MS) {
            logger.warn("Challenge expired: ${challenge.take(8)}...")
            challenges.remove(challenge)
            return false
        }

        // 4. 验证 HMAC
        if (!PackEncryption.verifyHmac(derivedSharedSecret, challenge, filename, hmac)) {
            logger.warn("HMAC verification failed for: $filename (challenge: ${challenge.take(8)}...)")
            // 标记为已使用，防止暴力尝试
            record.used = true
            return false
        }

        // 5. 验证通过，标记为已使用
        record.used = true
        challenges.remove(challenge)
        logger.debug("Challenge verified and consumed for: $filename")
        return true
    }

    /**
     * 清理过期的 challenge
     */
    fun cleanup() {
        val now = System.currentTimeMillis()
        var removed = 0

        val iterator = challenges.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.createdAt > CHALLENGE_TTL_MS || entry.value.used) {
                iterator.remove()
                removed++
            }
        }

        if (removed > 0) {
            logger.debug("Cleaned up $removed expired/used challenge(s), ${challenges.size} remaining")
        }

        lastCleanup = now
    }

    /**
     * 如果超过清理间隔，触发清理
     */
    private fun maybeCleanup() {
        val now = System.currentTimeMillis()
        if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
            cleanup()
        }
    }
}
