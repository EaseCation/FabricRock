package net.easecation.bedrockloader.bedrock.entity.components

data class ComponentNavigationWalk(
        val avoid_damage_blocks: Boolean?,
        val avoid_portals: Boolean?,
        val avoid_sun: Boolean?,
        val avoid_water: Boolean?,
        val blocks_to_avoid: List<String>?,
        val can_breach: Boolean?,
        val can_break_doors: Boolean?,
        val can_float: Boolean?,
        val can_jump: Boolean?,
        val can_open_doors: Boolean?,
        val can_open_iron_doors: Boolean?,
        val can_pass_doors: Boolean?,
        val can_path_from_air: Boolean?,
        val can_path_over_lava: Boolean?,
        val can_path_over_water: Boolean?,
        val can_sink: Boolean?,
        val can_swim: Boolean?,
        val can_walk: Boolean?,
        val can_walk_in_lava: Boolean?,
        val is_amphibious: Boolean?
) : IEntityComponent
