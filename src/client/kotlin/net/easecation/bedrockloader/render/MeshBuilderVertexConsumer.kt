package net.easecation.bedrockloader.render

import net.easecation.bedrockloader.bedrock.block.component.ComponentTransformation
import net.fabricmc.fabric.api.renderer.v1.Renderer
import net.fabricmc.fabric.api.renderer.v1.RendererAccess
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView
import net.fabricmc.fabric.api.util.TriState
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.texture.Sprite
import net.minecraft.util.math.Direction

class MeshBuilderVertexConsumer(
    private val defaultSprite: Sprite,
    private val sprites: Map<String, Sprite>,
    private val blockTransformation: ComponentTransformation? = null
) : VertexConsumer {

    private val renderer: Renderer = RendererAccess.INSTANCE.renderer!!
    private val meshBuilder: MeshBuilder = renderer.meshBuilder()
    private val emitter = meshBuilder.emitter

    // 创建禁用 AO 的材质（仅对设置了 cullFace 的 Quad 使用）
    private val noAoMaterial = renderer.materialFinder()
        .ambientOcclusion(TriState.FALSE)
        .find()

    private var vertexIndex = 0
    private var material: String? = null
    private var currentCullFace: Direction? = null  // 当前面的剔除方向

    fun material(material: String?) {
        this.material = material
    }

    /**
     * 设置当前 Quad 的剔除方向
     *
     * 当指定的方向有完整方块时，该 Quad 将不会被渲染。
     *
     * @param direction 剔除方向，null 表示不剔除
     */
    fun cullFace(direction: Direction?) {
        this.currentCullFace = direction
    }

    override fun vertex(x: Double, y: Double, z: Double): VertexConsumer {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.vertex($x, $y, $z)")
        emitter.pos(vertexIndex, x.toFloat(), y.toFloat(), z.toFloat())
        return this
    }

    override fun color(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.color($red, $green, $blue, $alpha)")
        emitter.color(vertexIndex, (red and 0xFF) or ((green and 0xFF) shl 8) or ((blue and 0xFF) shl 16) or ((alpha and 0xFF) shl 24))
        return this
    }

    override fun texture(u: Float, v: Float): VertexConsumer {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.texture($u, $v)")
        emitter.uv(vertexIndex, u, v)
        return this
    }

    override fun overlay(u: Int, v: Int): VertexConsumer {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.overlay($u, $v)")
        return this
    }

    override fun overlay(overlay: Int): VertexConsumer {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.overlay($overlay)")
        return this
    }

    override fun light(u: Int, v: Int): VertexConsumer {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.light($u, $v)")
        emitter.lightmap(vertexIndex, v)
        return this
    }

    override fun light(light: Int): VertexConsumer {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.light($light)")
        emitter.lightmap(vertexIndex, light)
        return this
    }

    override fun normal(x: Float, y: Float, z: Float): VertexConsumer {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.normal($x, $y, $z)")
        emitter.normal(vertexIndex, x, y, z)
        return this
    }

    /**
     * 应用基础旋转（X轴180° + Y轴180°）
     *
     * BedrockRenderUtil.bakeModelPartToMesh 中对模型应用了基础旋转：
     * matrixStack.multiply(Quaternionf().rotateXYZ(π, π, 0))
     *
     * 这个旋转的效果是所有方向都翻转：
     * - NORTH ↔ SOUTH (Y轴180°)
     * - EAST ↔ WEST (Y轴180°)
     * - UP ↔ DOWN (X轴180°)
     */
    private fun applyBaseRotation(direction: Direction): Direction {
        return direction.opposite
    }

    /**
     * 根据 blockTransformation 旋转方向
     * 仅处理 Y 轴旋转（水平方向旋转）
     */
    private fun rotateDirectionByTransformation(direction: Direction): Direction {
        if (blockTransformation == null) return direction

        // 只处理水平方向的旋转
        if (direction.axis == Direction.Axis.Y) return direction

        val rotation = blockTransformation.rotation
        if (rotation == null || rotation.size < 2) return direction

        // 获取 Y 轴旋转角度并标准化到 0-360
        val yRotation = ((rotation[1].toInt() % 360) + 360) % 360

        return when (yRotation) {
            90 -> when (direction) {
                Direction.NORTH -> Direction.EAST
                Direction.EAST -> Direction.SOUTH
                Direction.SOUTH -> Direction.WEST
                Direction.WEST -> Direction.NORTH
                else -> direction
            }
            180 -> when (direction) {
                Direction.NORTH -> Direction.SOUTH
                Direction.SOUTH -> Direction.NORTH
                Direction.EAST -> Direction.WEST
                Direction.WEST -> Direction.EAST
                else -> direction
            }
            270 -> when (direction) {
                Direction.NORTH -> Direction.WEST
                Direction.WEST -> Direction.SOUTH
                Direction.SOUTH -> Direction.EAST
                Direction.EAST -> Direction.NORTH
                else -> direction
            }
            else -> direction
        }
    }

    /**
     * 完整的方向旋转：先应用基础旋转，再应用 blockTransformation 旋转
     */
    private fun rotateDirection(direction: Direction): Direction {
        // 1. 先应用基础旋转（补偿 bakeModelPartToMesh 中的 X+Y 180度旋转）
        val baseRotated = applyBaseRotation(direction)
        // 2. 再应用 blockTransformation 的旋转
        return rotateDirectionByTransformation(baseRotated)
    }

    override fun next() {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.next()")
        vertexIndex++
        if (vertexIndex >= 4) {
            val sprite = if (material == null) defaultSprite else sprites[material] ?: defaultSprite

            // 设置面剔除方向（关键：当该方向有完整方块时，此面将不被渲染）
            // 根据 blockTransformation 旋转 cullFace 方向
            val rotatedCullFace = currentCullFace?.let { rotateDirection(it) }
            emitter.cullFace(rotatedCullFace)

            // 对设置了 cullFace 的 Quad 禁用 AO，避免在接触面出现暗色渐变
            if (rotatedCullFace != null) {
                emitter.material(noAoMaterial)
            }

            emitter.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED)
            emitter.emit()

            // 重置状态
            vertexIndex = 0
            currentCullFace = null
        }
    }

    override fun fixedColor(red: Int, green: Int, blue: Int, alpha: Int) {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.fixedColor($red, $green, $blue, $alpha)")
        emitter.color(vertexIndex, (red and 0xFF) or ((green and 0xFF) shl 8) or ((blue and 0xFF) shl 16) or ((alpha and 0xFF) shl 24))
    }

    override fun unfixColor() {
        // BedrockLoader.logger.info("MeshBuilderVertexConsumer.unfixColor()")
        emitter.color(vertexIndex, -1)
    }

    fun build(): Mesh {
        return meshBuilder.build()
    }
}