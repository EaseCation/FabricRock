package net.easecation.bedrockloader.loader.error

/**
 * 加载错误/警告的数据模型
 */
data class LoadingError(
    val level: Level,
    val source: String,
    val phase: Phase,
    val message: String,
    val exception: Throwable? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 错误级别
     */
    enum class Level {
        WARNING,
        ERROR
    }

    /**
     * 加载阶段
     */
    enum class Phase(val displayName: String) {
        PACK_SCAN("包扫描"),
        PACK_LOAD("包加载"),
        BLOCK_REGISTER("方块注册"),
        ENTITY_REGISTER("实体注册"),
        RESOURCE_LOAD("资源加载")
    }

    /**
     * 获取级别的显示名称
     */
    fun getLevelDisplayName(): String = when (level) {
        Level.WARNING -> "警告"
        Level.ERROR -> "错误"
    }

    /**
     * 获取完整的错误描述
     */
    fun getFullDescription(): String {
        val sb = StringBuilder()
        sb.append("[${getLevelDisplayName()}] ")
        sb.append("[${phase.displayName}] ")
        sb.append("$source: $message")
        if (exception != null) {
            sb.append("\n")
            sb.append(exception.stackTraceToString())
        }
        return sb.toString()
    }
}
