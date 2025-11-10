package net.easecation.bedrockloader.sync.common

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * MD5哈希计算工具
 */
object MD5Util {
    /**
     * 计算文件的MD5哈希值
     *
     * @param file 要计算MD5的文件
     * @return 32位十六进制字符串表示的MD5值
     * @throws java.io.IOException 如果文件读取失败
     */
    fun calculateMD5(file: File): String {
        if (!file.exists() || !file.isFile) {
            throw IllegalArgumentException("文件不存在或不是有效文件: ${file.absolutePath}")
        }

        val digest = MessageDigest.getInstance("MD5")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192) // 8KB 缓冲区
            var bytesRead: Int

            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        // 将字节数组转换为十六进制字符串
        return digest.digest().joinToString("") { byte ->
            "%02x".format(byte)
        }
    }

    /**
     * 验证文件的MD5是否匹配
     *
     * @param file 要验证的文件
     * @param expectedMD5 期望的MD5值（不区分大小写）
     * @return 如果MD5匹配返回true，否则返回false
     */
    fun verifyMD5(file: File, expectedMD5: String): Boolean {
        return try {
            val actualMD5 = calculateMD5(file)
            actualMD5.equals(expectedMD5, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}
