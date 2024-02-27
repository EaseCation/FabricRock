package net.easecation.bedrockloader.bedrock.entity.components

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

sealed class ComponentRideable(

) : IEntityComponent {
        data class Seat(
                val lock_rider_rotation: Int?,
                val max_rider_count: Int?,
                val min_rider_count: Int?,
                val position: List<Double>?,
                val rotate_rider_by: Any?
        )

        data class ComponentRideableWithSeat (
                val controlling_seat: Int?,
                val crouching_skip_interact: Boolean?,
                val family_types: List<Any>?, // Replace 'Any' with the appropriate type if known
                val interact_text: String?,
                val passenger_max_width: Double?,
                val priority: Int?,
                val pull_in_entities: Boolean?,
                val rider_can_interact: Boolean?,
                val seat_count: Int,
                val seats: Seat
        ): ComponentRideable()

        data class ComponentRideableWithSeats (
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
        ): ComponentRideable()

        class Deserializer : JsonDeserializer<ComponentRideable> {
                override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ComponentRideable {
                        val seats = json.asJsonObject["seats"]
                        return if (seats.isJsonArray) {
                                val type = object :TypeToken<ComponentRideableWithSeats?>() {}.type
                                context.deserialize<ComponentRideableWithSeats>(json, type)
                        } else if (seats.isJsonObject){
                                val type = object :TypeToken<ComponentRideableWithSeat?>() {}.type
                                context.deserialize<ComponentRideableWithSeat>(json, type)
                        } else {
                                throw JsonParseException("Unexpected JSON type for ComponentRideable")
                        }
                }
        }
}