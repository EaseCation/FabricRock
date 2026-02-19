package net.easecation.bedrockloader.sync.common

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 资源包加密/解密工具
 *
 * 功能:
 * - AES-256-CFB8 整包加密/解密（与基岩版标准算法一致）
 * - HMAC-SHA256 用于 Challenge-Response 握手
 * - Shared Secret 自动派生（客户端零配置）
 *
 * 安全模型:
 * - MOD_KEY 硬编码在 mod JAR 中，经 ProGuard 混淆
 * - server_token 公开发布在 manifest 中
 * - shared_secret = deriveSharedSecret(serverToken)，需要 MOD_KEY 才能计算
 * - 浏览器/curl 无法获取密钥，需反编译 mod 才能破解
 */
object PackEncryption {

    private const val AES_ALGORITHM = "AES/CFB8/NoPadding"
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val IV_SIZE = 16
    private const val KEY_SIZE = 32 // 256 bits

    private val secureRandom = SecureRandom()

    // ============================================================
    // MOD_KEY: 内嵌常量，不通过网络传输
    // 拆分为多个片段并 XOR 编码，增加逆向提取难度
    // ProGuard 会混淆类名/方法名/字段名
    // ============================================================
    private val MOD_KEY_PART_A = byteArrayOf(
        0x4B, 0x72, 0x61, 0x6E, 0x64, 0x6F, 0x6D, 0x53,
        0x65, 0x63, 0x72, 0x65, 0x74, 0x4B, 0x65, 0x79
    )
    private val MOD_KEY_PART_B = byteArrayOf(
        0x46, 0x6F, 0x72, 0x42, 0x65, 0x64, 0x72, 0x6F,
        0x63, 0x6B, 0x4C, 0x6F, 0x61, 0x64, 0x65, 0x72
    )
    private const val MOD_KEY_XOR_MASK: Byte = 0x5A

    private val modKey: ByteArray by lazy {
        val combined = ByteArray(MOD_KEY_PART_A.size + MOD_KEY_PART_B.size)
        for (i in MOD_KEY_PART_A.indices) {
            combined[i] = (MOD_KEY_PART_A[i].toInt() xor MOD_KEY_XOR_MASK.toInt()).toByte()
        }
        for (i in MOD_KEY_PART_B.indices) {
            combined[MOD_KEY_PART_A.size + i] = (MOD_KEY_PART_B[i].toInt() xor MOD_KEY_XOR_MASK.toInt()).toByte()
        }
        combined
    }

    // 派生用盐值（硬编码）
    private const val SALT_TOKEN = "BdLdr::TokenGen::v1"
    private const val SALT_DERIVE = "BdLdr::SecretDerive::v1"

    // ============================================================
    // AES-256-CFB8 加密/解密
    // ============================================================

    /**
     * 生成随机的 AES-256 密钥
     * @return 64字符的十六进制密钥字符串
     */
    fun generateKey(): String {
        val key = ByteArray(KEY_SIZE)
        secureRandom.nextBytes(key)
        return key.toHexString()
    }

    /**
     * 加密数据（整包加密）
     * 输出格式: [16字节随机IV] + [AES-256-CFB8加密数据]
     *
     * @param data 原始数据（ZIP文件字节）
     * @param keyHex 密钥的十六进制字符串（64字符）
     * @return 加密后的数据（IV + 密文）
     */
    fun encrypt(data: ByteArray, keyHex: String): ByteArray {
        require(isValidKey(keyHex)) { "Invalid encryption key: must be 64 hex characters" }

        val keyBytes = keyHex.hexToByteArray()
        val iv = ByteArray(IV_SIZE)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(iv))
        val encrypted = cipher.doFinal(data)

        // [IV] + [encrypted data]
        val result = ByteArray(IV_SIZE + encrypted.size)
        System.arraycopy(iv, 0, result, 0, IV_SIZE)
        System.arraycopy(encrypted, 0, result, IV_SIZE, encrypted.size)
        return result
    }

    /**
     * 解密数据
     * 输入格式: [16字节IV] + [AES-256-CFB8加密数据]
     *
     * @param encryptedData 加密数据（IV + 密文）
     * @param keyHex 密钥的十六进制字符串（64字符）
     * @return 解密后的原始数据
     */
    fun decrypt(encryptedData: ByteArray, keyHex: String): ByteArray {
        require(isValidKey(keyHex)) { "Invalid encryption key: must be 64 hex characters" }
        require(encryptedData.size > IV_SIZE) { "Encrypted data too short: must be > $IV_SIZE bytes" }

        val keyBytes = keyHex.hexToByteArray()

        // 提取 IV
        val iv = encryptedData.copyOfRange(0, IV_SIZE)
        val ciphertext = encryptedData.copyOfRange(IV_SIZE, encryptedData.size)

        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(iv))
        return cipher.doFinal(ciphertext)
    }

    /**
     * 验证密钥格式（64字符十六进制）
     */
    fun isValidKey(keyHex: String): Boolean {
        return keyHex.length == KEY_SIZE * 2 && keyHex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    // ============================================================
    // Challenge-Response
    // ============================================================

    /**
     * 生成随机 challenge 字符串（32字符十六进制）
     */
    fun generateChallenge(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.toHexString()
    }

    /**
     * 计算 HMAC-SHA256
     *
     * @param sharedSecret 共享密钥
     * @param challenge 服务端生成的一次性挑战
     * @param filename 目标文件名
     * @return HMAC 的十六进制字符串
     */
    fun computeHmac(sharedSecret: String, challenge: String, filename: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(sharedSecret.toByteArray(Charsets.UTF_8), HMAC_ALGORITHM))
        val data = "$challenge|$filename"
        return mac.doFinal(data.toByteArray(Charsets.UTF_8)).toHexString()
    }

    /**
     * 验证 HMAC-SHA256
     */
    fun verifyHmac(sharedSecret: String, challenge: String, filename: String, hmac: String): Boolean {
        val expected = computeHmac(sharedSecret, challenge, filename)
        // 常量时间比较，防止时序攻击
        return MessageDigest.isEqual(
            expected.lowercase().toByteArray(Charsets.UTF_8),
            hmac.lowercase().toByteArray(Charsets.UTF_8)
        )
    }

    // ============================================================
    // Shared Secret 派生
    // ============================================================

    /**
     * 从 server_secret 生成 server_token（服务端调用）
     *
     * server_token 是公开的，发布在 manifest 中。
     * 只知道 server_token 而不知道 MOD_KEY，无法推导 shared_secret。
     *
     * @param serverSecret 服务端配置的密钥
     * @return 公开的 server_token（64字符十六进制）
     */
    fun generateServerToken(serverSecret: String): String {
        // server_token = SHA-256(server_secret + SALT_TOKEN)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(serverSecret.toByteArray(Charsets.UTF_8))
        digest.update(SALT_TOKEN.toByteArray(Charsets.UTF_8))
        return digest.digest().toHexString()
    }

    /**
     * 从 server_token 派生 shared_secret（客户端和服务端均调用）
     *
     * 使用 MOD_KEY（mod内嵌常量）+ server_token 经过多轮变换。
     * 浏览器用户看到 server_token，但不知道 MOD_KEY 和变换函数，无法推导。
     *
     * @param serverToken 从 manifest 获取的公开令牌
     * @return shared_secret（64字符十六进制）
     */
    fun deriveSharedSecret(serverToken: String): String {
        // Step 1: HMAC-SHA256(MOD_KEY, serverToken)
        val mac1 = Mac.getInstance(HMAC_ALGORITHM)
        mac1.init(SecretKeySpec(modKey, HMAC_ALGORITHM))
        val intermediate1 = mac1.doFinal(serverToken.toByteArray(Charsets.UTF_8))

        // Step 2: XOR with reversed token bytes, then SHA-256
        val tokenBytes = serverToken.toByteArray(Charsets.UTF_8)
        val xored = ByteArray(intermediate1.size)
        for (i in intermediate1.indices) {
            xored[i] = (intermediate1[i].toInt() xor tokenBytes[i % tokenBytes.size].toInt()).toByte()
        }

        // Step 3: SHA-256(xored + SALT_DERIVE)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(xored)
        digest.update(SALT_DERIVE.toByteArray(Charsets.UTF_8))
        val intermediate2 = digest.digest()

        // Step 4: 再做一轮 HMAC 混合
        val mac2 = Mac.getInstance(HMAC_ALGORITHM)
        mac2.init(SecretKeySpec(intermediate2, HMAC_ALGORITHM))
        mac2.update(modKey)
        mac2.update(intermediate1)
        return mac2.doFinal().toHexString()
    }

    /**
     * 从 server_secret 直接派生 shared_secret（服务端便捷方法）
     *
     * 等价于: deriveSharedSecret(generateServerToken(serverSecret))
     *
     * @param serverSecret 服务端配置的密钥
     * @return shared_secret
     */
    fun deriveSharedSecretFromServerSecret(serverSecret: String): String {
        val serverToken = generateServerToken(serverSecret)
        return deriveSharedSecret(serverToken)
    }

    /**
     * 计算字节数组的 MD5
     */
    fun calculateMD5(data: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(data).toHexString()
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }

    private fun String.hexToByteArray(): ByteArray {
        require(length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
