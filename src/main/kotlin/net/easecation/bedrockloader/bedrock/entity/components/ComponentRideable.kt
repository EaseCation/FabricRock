package net.easecation.bedrockloader.bedrock.entity.components

data class ComponentRideable(
        val controlling_seat: Int?,
        val crouching_skip_interact: Boolean?,
        val family_types: List<Any>?, // Replace 'Any' with the appropriate type if known
        val interact_text: String?,
        val passenger_max_width: Double?,
        val priority: Int?,
        val pull_in_entities: Boolean?,
        val rider_can_interact: Boolean?,
        val seat_count: Int,
        val seats: List<Seat>
) : IEntityComponent {

    data class Seat(
            val lock_rider_rotation: Int?,
            val max_rider_count: Int?,
            val min_rider_count: Int?,
            val position: List<Double>?,
            val rotate_rider_by: Any?
    )

}