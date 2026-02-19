package net.easecation.bedrockloader.standalone

import com.google.gson.GsonBuilder
import net.easecation.bedrockloader.bedrock.pack.PackManifest
import net.easecation.bedrockloader.bedrock.pack.SemVersion
import net.easecation.bedrockloader.loader.BedrockPackRegistry
import net.easecation.bedrockloader.sync.common.MD5Util
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.security.MessageDigest
import java.util.*
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Standalone pack scanner
 *
 * Scans pack directory for .zip/.mcpack/.mcaddon files and folder packs,
 * extracts manifest info and registers to BedrockPackRegistry.
 * No Fabric/Minecraft dependencies.
 */
object StandalonePackScanner {

    private val logger = LoggerFactory.getLogger("BedrockPackServer/Scanner")

    private val gson = GsonBuilder()
        .registerTypeAdapter(SemVersion::class.java, SemVersion.Serializer())
        .create()

    /**
     * Scan pack directory and register all packs to BedrockPackRegistry
     */
    fun scanAndRegister(packDir: File, cacheDir: File) {
        BedrockPackRegistry.clear()

        val files = packDir.listFiles { file ->
            when {
                file.isDirectory -> {
                    val name = file.name
                    name != ".cache" && name != "remote" && !name.startsWith(".")
                }
                else -> {
                    val name = file.name.lowercase()
                    name.endsWith(".zip") || name.endsWith(".mcpack") || name.endsWith(".mcaddon")
                }
            }
        } ?: emptyArray()

        if (files.isEmpty()) {
            logger.warn("No packs found in ${packDir.absolutePath}")
            return
        }

        logger.info("Found ${files.size} entries to scan")

        for (file in files) {
            try {
                when {
                    file.isDirectory -> scanDirectory(file, cacheDir)
                    file.name.endsWith(".mcaddon", ignoreCase = true) -> scanMcAddon(file)
                    file.name.endsWith(".mcpack", ignoreCase = true) -> scanSingleZip(file)
                    file.name.endsWith(".zip", ignoreCase = true) -> {
                        if (hasManifestInZipRoot(file)) {
                            scanSingleZip(file)
                        } else if (hasSubPacksInZip(file)) {
                            scanMcAddon(file)
                        } else {
                            logger.warn("Cannot identify: ${file.name}")
                        }
                    }
                    else -> logger.warn("Unknown file: ${file.name}")
                }
            } catch (e: Exception) {
                logger.error("Failed to scan: ${file.name}", e)
            }
        }

        logger.info("Registered ${BedrockPackRegistry.getPackCount()} pack(s)")
    }

    /**
     * Scan directory (single pack or addon)
     */
    private fun scanDirectory(dir: File, cacheDir: File) {
        val hasManifest = File(dir, "manifest.json").exists() ||
                          File(dir, "pack_manifest.json").exists()

        if (hasManifest) {
            // Single pack directory -> package to ZIP
            val zipFile = packageDirectory(dir, cacheDir) ?: return
            scanSingleZip(zipFile)
        } else {
            // Check if addon directory (sub-dirs with manifest.json)
            val subDirs = dir.listFiles { f ->
                f.isDirectory && (File(f, "manifest.json").exists() ||
                                  File(f, "pack_manifest.json").exists())
            } ?: emptyArray()

            if (subDirs.isNotEmpty()) {
                val mcaddonFile = packageAddonDirectory(dir, cacheDir) ?: return
                scanMcAddon(mcaddonFile, addonName = dir.name)
            } else {
                logger.warn("Cannot identify directory: ${dir.name}")
            }
        }
    }

    /**
     * Scan single ZIP/MCPACK file
     */
    private fun scanSingleZip(file: File) {
        ZipFile(file).use { zip ->
            val manifestEntry = zip.getEntry("manifest.json")
                ?: zip.getEntry("pack_manifest.json")
                ?: run {
                    logger.warn("No manifest.json in: ${file.name}")
                    return
                }

            val manifest: PackManifest = zip.getInputStream(manifestEntry).use { stream ->
                gson.fromJson(InputStreamReader(stream), PackManifest::class.java)
            }

            if (!manifest.isValid()) {
                logger.warn("Invalid manifest in: ${file.name}")
                return
            }

            registerPack(manifest, file, addonName = null, isFromAddon = false)
            logger.info("  Pack: ${manifest.header?.name} [${manifest.header?.uuid}] (${file.name})")
        }
    }

    /**
     * Scan .mcaddon file (contains multiple sub-packs)
     */
    private fun scanMcAddon(file: File, addonName: String? = null) {
        val effectiveAddonName = addonName ?: file.nameWithoutExtension

        ZipFile(file).use { zip ->
            val manifestEntries = zip.entries().asSequence()
                .filter { entry ->
                    val name = entry.name
                    (name.endsWith("manifest.json") || name.endsWith("pack_manifest.json")) &&
                    name.count { it == '/' } == 1 // Only first-level subdirectories
                }
                .toList()

            if (manifestEntries.isEmpty()) {
                logger.warn("No sub-packs found in: ${file.name}")
                return
            }

            logger.info("  Addon: $effectiveAddonName (${manifestEntries.size} sub-pack(s))")

            for (entry in manifestEntries) {
                try {
                    val manifest: PackManifest = zip.getInputStream(entry).use { stream ->
                        gson.fromJson(InputStreamReader(stream), PackManifest::class.java)
                    }

                    if (manifest.isValid()) {
                        registerPack(manifest, file, effectiveAddonName, isFromAddon = true)
                        val packPath = entry.name.substringBeforeLast("/")
                        logger.info("    - ${manifest.header?.name} [${manifest.header?.uuid}] ($packPath)")
                    }
                } catch (e: Exception) {
                    logger.error("Failed to parse: ${entry.name}", e)
                }
            }
        }
    }

    /**
     * Register pack to BedrockPackRegistry
     */
    private fun registerPack(
        manifest: PackManifest,
        file: File,
        addonName: String?,
        isFromAddon: Boolean
    ) {
        val packId = manifest.header?.uuid?.toString() ?: return
        val packName = manifest.header?.name ?: "Unknown"
        val packVersion = manifest.header?.version?.toString() ?: "0.0.0"
        val packType = manifest.modules.firstOrNull()?.type ?: "resources"

        val packInfo = BedrockPackRegistry.PackInfo(
            id = packId,
            name = packName,
            version = packVersion,
            type = packType,
            file = file,
            md5 = MD5Util.calculateMD5(file),
            size = file.length(),
            manifest = manifest,
            addonName = addonName,
            isFromAddon = isFromAddon
        )

        BedrockPackRegistry.register(packInfo)
    }

    // =========== Directory packaging (ported from BedrockAddonsLoader/AddonScanner) ===========

    /**
     * Package single pack directory to ZIP with content-hash based caching.
     * Algorithm matches BedrockAddonsLoader.loadDirectoryPack() for MD5 consistency.
     */
    private fun packageDirectory(dir: File, cacheDir: File): File? {
        val cacheFile = File(cacheDir, "${dir.name}.zip")
        val hashFile = File(cacheDir, "${dir.name}.hash")

        val currentHash = try {
            calculateDirectoryHash(dir)
        } catch (e: Exception) {
            logger.error("Failed to calculate directory hash: ${dir.name}", e)
            return null
        }

        // Check cache validity
        if (cacheFile.exists() && hashFile.exists()) {
            val cachedHash = try { hashFile.readText().trim() } catch (_: Exception) { "" }
            if (cachedHash == currentHash) {
                logger.debug("Using cached pack: ${dir.name}")
                return cacheFile
            }
        }

        logger.info("Packaging directory: ${dir.name}")

        try {
            val time = FileTime.fromMillis(0)
            ZipOutputStream(FileOutputStream(cacheFile)).use { stream ->
                stream.setLevel(Deflater.BEST_COMPRESSION)
                // Use TreeSet for deterministic ordering (same as BedrockAddonsLoader)
                val files = TreeSet<File>(Comparator.comparing { it.absolutePath })
                dir.walk().filter { it.isFile }.forEach { files.add(it) }

                for (file in files) {
                    val relativePath = dir.toPath().relativize(file.toPath()).toString().replace("\\", "/")
                    val entry = ZipEntry(relativePath)
                        .setCreationTime(time)
                        .setLastModifiedTime(time)
                        .setLastAccessTime(time)
                    stream.putNextEntry(entry)
                    stream.write(file.readBytes())
                    stream.closeEntry()
                }
            }
            logger.info("Directory packaged: ${dir.name} -> ${cacheFile.name}")

            // Save content hash
            try {
                hashFile.writeText(currentHash, Charsets.UTF_8)
            } catch (e: Exception) {
                logger.warn("Failed to save hash file: ${hashFile.name}", e)
            }
        } catch (e: Exception) {
            logger.error("Failed to package directory: ${dir.name}", e)
            return null
        }

        return cacheFile
    }

    /**
     * Package addon directory to .mcaddon with content-hash based caching.
     * Algorithm matches AddonScanner.packageAddonDirectory() for MD5 consistency.
     */
    private fun packageAddonDirectory(dir: File, cacheDir: File): File? {
        val cacheFile = File(cacheDir, "${dir.name}.mcaddon")
        val hashFile = File(cacheDir, "${dir.name}.mcaddon.hash")

        val currentHash = try {
            calculateAddonDirectoryHash(dir)
        } catch (e: Exception) {
            logger.error("Failed to calculate addon directory hash: ${dir.name}", e)
            return null
        }

        // Check cache validity
        if (cacheFile.exists() && hashFile.exists()) {
            val cachedHash = try { hashFile.readText().trim() } catch (_: Exception) { "" }
            if (cachedHash == currentHash) {
                logger.debug("Using cached addon: ${dir.name}")
                return cacheFile
            }
        }

        logger.info("Packaging addon directory: ${dir.name}")

        try {
            val time = FileTime.fromMillis(0)
            ZipOutputStream(FileOutputStream(cacheFile)).use { stream ->
                stream.setLevel(Deflater.BEST_COMPRESSION)

                val subDirs = dir.listFiles { f ->
                    f.isDirectory && (File(f, "manifest.json").exists() || File(f, "pack_manifest.json").exists())
                } ?: emptyArray()

                for (subDir in subDirs) {
                    // Use TreeSet for deterministic ordering (same as AddonScanner)
                    val files = TreeSet<File>(Comparator.comparing { it.absolutePath })
                    subDir.walk().filter { it.isFile }.forEach { files.add(it) }

                    for (file in files) {
                        val relativePath = "${subDir.name}/${subDir.toPath().relativize(file.toPath()).toString().replace("\\", "/")}"
                        val entry = ZipEntry(relativePath)
                            .setCreationTime(time)
                            .setLastModifiedTime(time)
                            .setLastAccessTime(time)
                        stream.putNextEntry(entry)
                        stream.write(file.readBytes())
                        stream.closeEntry()
                    }
                }
            }
            logger.info("Addon packaged: ${dir.name} -> ${cacheFile.name}")

            try {
                hashFile.writeText(currentHash, Charsets.UTF_8)
            } catch (e: Exception) {
                logger.warn("Failed to save addon hash file: ${hashFile.name}", e)
            }
        } catch (e: Exception) {
            logger.error("Failed to package addon directory: ${dir.name}", e)
            return null
        }

        return cacheFile
    }

    // =========== Helper methods ===========

    private fun hasManifestInZipRoot(file: File): Boolean {
        return try {
            ZipFile(file).use { zip ->
                zip.getEntry("manifest.json") != null || zip.getEntry("pack_manifest.json") != null
            }
        } catch (_: Exception) { false }
    }

    private fun hasSubPacksInZip(file: File): Boolean {
        return try {
            ZipFile(file).use { zip ->
                zip.entries().asSequence()
                    .any { entry ->
                        val name = entry.name
                        (name.endsWith("manifest.json") || name.endsWith("pack_manifest.json")) &&
                        name.contains("/")
                    }
            }
        } catch (_: Exception) { false }
    }

    /**
     * Calculate directory content hash (matching BedrockAddonsLoader algorithm).
     * Uses MD5 of sorted file paths + contents.
     */
    private fun calculateDirectoryHash(dir: File): String {
        val md = MessageDigest.getInstance("MD5")
        // TreeSet with absolute path comparison matches TreeSet<File>(FileUtils.listFiles(...))
        val files = TreeSet<File>(Comparator.comparing { it.absolutePath })
        dir.walk().filter { it.isFile }.forEach { files.add(it) }

        for (file in files) {
            val relativePath = dir.toPath().relativize(file.toPath()).toString()
            md.update(relativePath.toByteArray(Charsets.UTF_8))
            md.update(file.readBytes())
        }

        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculate addon directory content hash (matching AddonScanner algorithm).
     * Only hashes sub-directories that contain manifest.json.
     */
    private fun calculateAddonDirectoryHash(dir: File): String {
        val md = MessageDigest.getInstance("MD5")

        val subDirs = dir.listFiles { f ->
            f.isDirectory && (File(f, "manifest.json").exists() || File(f, "pack_manifest.json").exists())
        }?.sortedBy { it.name } ?: emptyList()

        for (subDir in subDirs) {
            val files = TreeSet<File>(Comparator.comparing { it.absolutePath })
            subDir.walk().filter { it.isFile }.forEach { files.add(it) }

            for (file in files) {
                val relativePath = "${subDir.name}/${subDir.toPath().relativize(file.toPath())}"
                md.update(relativePath.toByteArray(Charsets.UTF_8))
                md.update(file.readBytes())
            }
        }

        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
