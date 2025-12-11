package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.bedrock.block.component.ComponentTransformation
import net.easecation.bedrockloader.bedrock.definition.BlockCullingDefinition
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.util.math.Direction

/**
 * 存储方块的面剔除信息（客户端渲染用）
 *
 * @param faceCulling 面 -> 剔除方向的映射
 *   - Key: (boneName, cubeIndex, face) 的组合标识
 *   - Value: 当该方向有完整方块时剔除此面
 */
@Environment(EnvType.CLIENT)
data class BlockCullingInfo(
    val faceCulling: Map<FaceKey, Direction>
) {
    /**
     * 面的唯一标识键
     *
     * @param boneName 骨骼名称
     * @param cubeIndex 该骨骼中的立方体索引（从 0 开始）
     * @param face 该立方体的面方向
     */
    data class FaceKey(
        val boneName: String,
        val cubeIndex: Int,
        val face: Direction
    )

    /**
     * 获取指定面的剔除方向
     *
     * @param boneName 骨骼名称
     * @param cubeIndex 立方体索引
     * @param face 面方向
     * @return 剔除方向，如果该面没有配置剔除规则则返回 null
     */
    fun getCullDirection(boneName: String, cubeIndex: Int, face: Direction): Direction? {
        return faceCulling[FaceKey(boneName, cubeIndex, face)]
    }

    /**
     * 根据方块的旋转变换调整 culling 方向
     *
     * 当方块使用 minecraft:transformation 旋转时，cullFace 方向需要相应旋转。
     * 例如：原本 north 面的规则，旋转 90° 后应该变成 east 面。
     *
     * @param transformation 方块的 transformation 组件
     * @return 旋转后的 culling 信息
     */
    fun rotated(transformation: ComponentTransformation?): BlockCullingInfo {
        if (transformation == null) return this

        val rotation = transformation.rotation ?: return this
        val yRotation = rotation.getOrNull(1)?.toInt() ?: 0

        // 如果没有 Y 轴旋转，直接返回
        if (yRotation == 0) return this

        // 根据 Y 轴旋转调整水平方向
        val rotatedCulling = faceCulling.map { (key, cullDir) ->
            val rotatedFace = rotateDirection(key.face, yRotation)
            val rotatedCullDir = rotateDirection(cullDir, yRotation)
            FaceKey(key.boneName, key.cubeIndex, rotatedFace) to rotatedCullDir
        }.toMap()

        return copy(faceCulling = rotatedCulling)
    }

    companion object {
        /**
         * 从基岩版 culling 规则创建 BlockCullingInfo
         *
         * @param rules 基岩版定义的 culling 规则
         * @return BlockCullingInfo 实例，如果规则为空则返回 null
         */
        fun fromRules(rules: BlockCullingDefinition.BlockCullingRules?): BlockCullingInfo? {
            if (rules == null) return null

            val faceCulling = mutableMapOf<FaceKey, Direction>()
            rules.rules.forEach { rule ->
                val faceDir = directionFromString(rule.geometry_part.face)
                val cullDir = directionFromString(rule.direction)
                if (faceDir != null && cullDir != null) {
                    faceCulling[FaceKey(
                        rule.geometry_part.bone,
                        rule.geometry_part.cube,
                        faceDir
                    )] = cullDir
                }
            }

            return if (faceCulling.isEmpty()) null else BlockCullingInfo(faceCulling)
        }

        /**
         * 将字符串方向转换为 Direction 枚举
         */
        private fun directionFromString(str: String): Direction? {
            return when (str.lowercase()) {
                "north" -> Direction.NORTH
                "south" -> Direction.SOUTH
                "east" -> Direction.EAST
                "west" -> Direction.WEST
                "up" -> Direction.UP
                "down" -> Direction.DOWN
                else -> null
            }
        }

        /**
         * 根据 Y 轴旋转角度旋转方向
         *
         * @param direction 原始方向
         * @param yRotationDegrees Y 轴旋转角度（度数）
         * @return 旋转后的方向
         */
        private fun rotateDirection(direction: Direction, yRotationDegrees: Int): Direction {
            // 垂直方向不受 Y 轴旋转影响
            if (direction.axis.isVertical) return direction

            // 规范化角度到 [0, 360)
            val normalizedDegrees = ((yRotationDegrees % 360) + 360) % 360
            val steps = normalizedDegrees / 90

            // 按 90 度增量顺时针旋转
            var result = direction
            repeat(steps) {
                result = result.rotateYClockwise()
            }
            return result
        }
    }
}
