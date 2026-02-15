package net.easecation.bedrockloader.render.state

//? if >=1.21.2 {
/*import net.minecraft.client.render.entity.state.LivingEntityRenderState
import net.minecraft.util.Identifier

/^*
 * 数据驱动实体的渲染状态
 * 用于在1.21.2+版本中存储渲染所需的数据
 ^/
class EntityDataDrivenRenderState : LivingEntityRenderState() {
    /^*
     * 实体标识符,用于查找纹理和模型
     ^/
    var identifier: Identifier? = null

    /^*
     * 动画管理器引用
     * 类型为EntityAnimationManager,但声明为Any?避免客户端类依赖
     ^/
    var animationManager: Any? = null

    /^*
     * 实体缩放比例
     ^/
    var scale: Float = 1.0f
}
*///?}
