package net.easecation.bedrockloader.bedrock.pack

import com.google.gson.reflect.TypeToken
import net.easecation.bedrockloader.util.GsonUtil
import java.io.File
import java.io.InputStreamReader
import java.util.zip.ZipFile

class ZippedBedrockPack(file: File) : AbstractBedrockPack() {

    init {
        if (!file.exists()) {
            throw IllegalArgumentException("File not found: " + file.name)
        }

        ZipFile(file).use { zip ->
            val entry = zip.getEntry("manifest.json") ?: zip.getEntry("pack_manifest.json")
            if (entry == null) {
                throw IllegalArgumentException("No manifest found in " + file.name)
            }
            val type = object : TypeToken<PackManifest>() {}.type
            manifest = GsonUtil.GSON.fromJson(InputStreamReader(zip.getInputStream(entry)), type)
        }

        manifest?.let {
            if (!it.isValid()) {
                throw IllegalArgumentException("Invalid manifest in " + file.name)
            }

            id = it.header?.uuid.toString()
            version = it.header?.version.toString()
            type = it.modules.stream()
                    .findFirst()
                    .map { module -> module.type }
                    .orElse("resources")
        }
    }

}