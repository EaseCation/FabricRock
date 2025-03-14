package net.easecation.bedrockloader.bedrock.definition

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import net.easecation.bedrockloader.bedrock.block.component.BlockComponents
import net.easecation.bedrockloader.bedrock.block.permutation.BlockPermutation
import net.easecation.bedrockloader.bedrock.block.state.IBlockState
import net.easecation.bedrockloader.bedrock.block.traits.BlockTraits
import net.minecraft.util.Identifier

data class BlockBehaviourDefinition(
        @SerializedName("format_version") val formatVersion: String,
        @SerializedName("minecraft:block") val minecraftBlock: BlockBehaviour
) {

    data class BlockBehaviour(
        val description: Description,
        val components: BlockComponents,
        val component_groups: Map<String, JsonElement>,
        val events: Map<String, JsonElement>?,
        val permutations: List<BlockPermutation>?
    )

    data class Description(
        val identifier: Identifier,
        val menu_category: MenuCategory?,
        val states: Map<String, IBlockState>?,
        val traits: BlockTraits?
    )

    data class MenuCategory(
        val category: String?,
        val group: String?,
        val is_hidden_in_commands: Boolean?
    )
}
