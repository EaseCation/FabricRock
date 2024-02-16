package net.easecation.bedrockloader.bedrock.pack

import com.google.gson.annotations.SerializedName
import java.util.*

data class PackManifest(
        @SerializedName("format_version")
        var formatVersion: String? = null,
        var header: Header? = null,
        var modules: List<Module> = emptyList(),
        var metadata: Metadata? = null,
        var dependencies: List<Dependency> = emptyList(),
        var capabilities: List<String> = emptyList(),
        var subpacks: List<SubPack> = emptyList()
) {
    fun isValid(): Boolean {
        return formatVersion != null && header != null && modules.isNotEmpty() &&
                header?.description != null && header?.name != null &&
                header?.uuid != null && header?.version != null
    }

    data class Header(
            var name: String? = null,
            var description: String? = null,
            var uuid: UUID? = null,
            @SerializedName("platform_locked")
            var platformLocked: Boolean = false,
            var version: SemVersion? = null,
            @SerializedName("min_engine_version")
            var minEngineVersion: SemVersion? = null,
            @SerializedName("pack_scope")
            var packScope: String = "global",
            @SerializedName("directory_load")
            var directoryLoad: Boolean = false,
            @SerializedName("load_before_game")
            var loadBeforeGame: Boolean = false,
            @SerializedName("lock_template_options")
            var lockTemplateOptions: Boolean = false,
            @SerializedName("population_control")
            var populationControl: Boolean = false
    )

    data class Module(
            var uuid: UUID? = null,
            var description: String? = null,
            var version: SemVersion? = null,
            var type: String? = null
    )

    data class Metadata(
            var authors: List<String>? = null,
            var license: String? = null,
            var url: String? = null
    )

    data class Dependency(
            var uuid: UUID? = null,
            var version: SemVersion? = null
    )

    data class SubPack(
            @SerializedName("folder_name")
            var folderName: String? = null,
            var name: String? = null,
            @SerializedName("memory_tier")
            var memoryTier: Int = 0
    )
}