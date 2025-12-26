package net.easecation.bedrockloader.sync.client

import net.easecation.bedrockloader.sync.client.model.SyncPlan
import net.easecation.bedrockloader.sync.common.RemotePackInfo
import net.easecation.bedrockloader.sync.common.RemotePackManifest
import org.slf4j.LoggerFactory
import java.io.File

/**
 * 资源包对比器
 * 对比remote/目录中的包和远程manifest，生成同步计划
 */
class PackComparator(
    private val packDirectory: File,
    private val config: ClientConfig
) {
    private val logger = LoggerFactory.getLogger("BedrockLoader/PackComparator")
    private val scanner = LocalPackScanner(packDirectory)

    /**
     * 对比本地包和远程manifest（基于UUID智能比较）
     *
     * @param manifest 远程资源包manifest
     * @return 同步计划（包含UUID冲突信息）
     */
    fun compare(manifest: RemotePackManifest): SyncPlan {
        logger.info("开始对比本地包和远程manifest（基于UUID）")

        // 扫描本地包（包括手动包和remote/包）
        val localPacks = scanner.scan()
        logger.info("本地包数量: ${localPacks.size} (手动: ${localPacks.count { it.isManual }}, 远程: ${localPacks.count { !it.isManual }})")
        logger.info("远程服务器包数量: ${manifest.packs.size}")

        // 按UUID和isManual分组本地包
        val manualPacksByUUID = mutableMapOf<String, LocalPackScanner.LocalPackInfo>()
        val remotePacksByUUID = mutableMapOf<String, LocalPackScanner.LocalPackInfo>()
        val localPacksByFilename = mutableMapOf<String, LocalPackScanner.LocalPackInfo>()

        for (localPack in localPacks) {
            // 文件名映射（用于兼容旧逻辑）
            localPacksByFilename[localPack.filename] = localPack

            // UUID映射（优先使用）
            if (localPack.uuid != null) {
                if (localPack.isManual) {
                    manualPacksByUUID[localPack.uuid] = localPack
                } else {
                    remotePacksByUUID[localPack.uuid] = localPack
                }
            }
        }

        logger.debug("手动包UUID映射: ${manualPacksByUUID.size} 个, 远程包UUID映射: ${remotePacksByUUID.size} 个")

        // 分类远程包
        val toDownload = mutableListOf<RemotePackInfo>()  // 需要下载的新包
        val toUpdate = mutableListOf<RemotePackInfo>()    // 需要更新的包
        val upToDate = mutableListOf<String>()            // 已是最新的包
        val uuidConflicts = mutableListOf<Pair<RemotePackInfo, LocalPackScanner.LocalPackInfo>>()  // UUID冲突的包

        // 遍历远程包
        for (remotePack in manifest.packs) {
            val remoteUUID = remotePack.uuid

            // 策略1: 基于UUID比较（如果UUID可用）
            if (remoteUUID != null && remoteUUID.isNotBlank()) {
                // 检查与手动包的UUID冲突
                val manualPack = manualPacksByUUID[remoteUUID]
                if (manualPack != null) {
                    // UUID冲突：优先使用手动包，跳过下载
                    logger.warn("UUID冲突: 远程包 ${remotePack.name} (UUID: $remoteUUID) 与手动包 ${manualPack.filename} 冲突，使用手动包")
                    uuidConflicts.add(Pair(remotePack, manualPack))
                    upToDate.add(remotePack.name)  // 标记为最新，不下载
                    continue
                }

                // 检查与remote/包的UUID匹配
                val remotePack本地 = remotePacksByUUID[remoteUUID]
                if (remotePack本地 != null) {
                    // UUID匹配，比较MD5
                    if (remotePack本地.md5.equals(remotePack.md5, ignoreCase = true)) {
                        logger.debug("UUID匹配，已是最新: ${remotePack.name} (UUID: $remoteUUID)")
                        upToDate.add(remotePack.name)
                    } else {
                        logger.debug("UUID匹配，需要更新: ${remotePack.name} (UUID: $remoteUUID, MD5不匹配)")
                        toUpdate.add(remotePack)
                    }
                    continue
                }

                // UUID不在本地存在，需要下载
                logger.debug("UUID不存在本地，需要下载: ${remotePack.name} (UUID: $remoteUUID)")
                toDownload.add(remotePack)
            } else {
                // 策略2: 回退到文件名比较（UUID不可用时）
                logger.debug("远程包 ${remotePack.name} 没有UUID信息，回退到文件名比较")
                val localPack = localPacksByFilename[remotePack.name]

                if (localPack == null) {
                    logger.debug("需要下载新包: ${remotePack.name} (按文件名)")
                    toDownload.add(remotePack)
                } else {
                    if (localPack.md5.equals(remotePack.md5, ignoreCase = true)) {
                        logger.debug("已是最新: ${remotePack.name} (按文件名)")
                        upToDate.add(remotePack.name)
                    } else {
                        logger.debug("需要更新: ${remotePack.name} (按文件名, MD5不匹配)")
                        toUpdate.add(remotePack)
                    }
                }
            }
        }

        // 找出仅remote/目录中存在的包（远程manifest中没有）
        val remotePackUUIDs = manifest.packs.mapNotNull { it.uuid }.toSet()
        val remotePackNames = manifest.packs.map { it.name }.toSet()
        val localOnly = localPacks
            .filter { !it.isManual }  // 只考虑remote/目录中的包
            .filter { localPack ->
                // 如果本地包有UUID，按UUID匹配；否则按文件名匹配
                if (localPack.uuid != null) {
                    localPack.uuid !in remotePackUUIDs
                } else {
                    localPack.filename !in remotePackNames
                }
            }
            .map { it.filename }

        if (localOnly.isNotEmpty()) {
            logger.info("发现仅remote/目录中存在的包（远程已删除）: ${localOnly.joinToString(", ")}")
        }

        // UUID冲突报告
        if (uuidConflicts.isNotEmpty()) {
            logger.info("检测到 ${uuidConflicts.size} 个UUID冲突:")
            for ((remote, manual) in uuidConflicts) {
                logger.info("  - 远程: ${remote.name} (UUID: ${remote.uuid}) <-> 手动: ${manual.filename}")
            }
        }

        // 根据配置决定是否需要清理
        val packagesToCleanup = if (config.autoCleanupRemovedPacks) {
            logger.info("自动清理已启用，将清理 ${localOnly.size} 个远程已删除的包")
            localOnly
        } else {
            logger.info("自动清理已禁用，保留远程已删除的包")
            emptyList()
        }

        val plan = SyncPlan(
            toDownload = toDownload,
            toUpdate = toUpdate,
            upToDate = upToDate,
            localOnly = localOnly,
            packagesToCleanup = packagesToCleanup,
            uuidConflicts = uuidConflicts.map { (remote, manual) ->
                SyncPlan.UUIDConflict(
                    remotePackName = remote.name,
                    remoteUUID = remote.uuid ?: "",
                    localPackName = manual.filename,
                    localIsManual = manual.isManual
                )
            }
        )

        logger.info("对比完成: 下载=${toDownload.size}, 更新=${toUpdate.size}, 最新=${upToDate.size}, UUID冲突=${uuidConflicts.size}")

        return plan
    }

    /**
     * 快速检查是否需要同步
     * 只比较remote/目录中包的数量，不计算MD5
     *
     * @param manifest 远程资源包manifest
     * @return true: 可能需要同步, false: 包数量匹配（可能已是最新）
     */
    fun quickCheck(manifest: RemotePackManifest): Boolean {
        val remoteDirectory = File(packDirectory, "remote")
        val localPackCount = remoteDirectory.listFiles()?.count { file ->
            file.isFile && (file.name.endsWith(".zip") || file.name.endsWith(".mcpack") || file.name.endsWith(".mcaddon"))
        } ?: 0

        val needSync = localPackCount != manifest.packs.size

        logger.debug("快速检查: remote/目录中=$localPackCount, 远程=${manifest.packs.size}, 需要同步=$needSync")

        return needSync
    }
}
