package net.easecation.bedrockloader.loader.error

import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 加载错误收集器
 * 在加载过程中收集所有的错误和警告，供UI显示
 */
object LoadingErrorCollector {

    private val logger = LoggerFactory.getLogger("bedrock-loader")
    private val errors = CopyOnWriteArrayList<LoadingError>()

    /**
     * 添加警告
     */
    fun addWarning(source: String, phase: LoadingError.Phase, message: String) {
        val error = LoadingError(
            level = LoadingError.Level.WARNING,
            source = source,
            phase = phase,
            message = message,
            exception = null
        )
        errors.add(error)
        // 同时写入日志
        logger.warn("[{}] {}: {}", phase.displayName, source, message)
    }

    /**
     * 添加错误
     */
    fun addError(source: String, phase: LoadingError.Phase, message: String, exception: Throwable? = null) {
        val error = LoadingError(
            level = LoadingError.Level.ERROR,
            source = source,
            phase = phase,
            message = message,
            exception = exception
        )
        errors.add(error)
        // 同时写入日志（包含堆栈）
        if (exception != null) {
            logger.error("[{}] {}: {}", phase.displayName, source, message, exception)
        } else {
            logger.error("[{}] {}: {}", phase.displayName, source, message)
        }
    }

    /**
     * 获取所有错误和警告
     */
    fun getErrors(): List<LoadingError> = errors.toList()

    /**
     * 获取警告数量
     */
    fun getWarningCount(): Int = errors.count { it.level == LoadingError.Level.WARNING }

    /**
     * 获取错误数量
     */
    fun getErrorCount(): Int = errors.count { it.level == LoadingError.Level.ERROR }

    /**
     * 获取总数量
     */
    fun getTotalCount(): Int = errors.size

    /**
     * 是否有错误或警告
     */
    fun hasErrors(): Boolean = errors.isNotEmpty()

    /**
     * 是否有严重错误（ERROR级别）
     */
    fun hasSevereErrors(): Boolean = errors.any { it.level == LoadingError.Level.ERROR }

    /**
     * 清空所有错误
     */
    fun clear() {
        errors.clear()
    }

    /**
     * 获取按阶段分组的错误
     */
    fun getErrorsByPhase(): Map<LoadingError.Phase, List<LoadingError>> {
        return errors.groupBy { it.phase }
    }

    /**
     * 获取按来源分组的错误
     */
    fun getErrorsBySource(): Map<String, List<LoadingError>> {
        return errors.groupBy { it.source }
    }
}
