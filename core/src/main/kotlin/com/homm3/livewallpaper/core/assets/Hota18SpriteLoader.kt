package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.homm3.livewallpaper.parser.def.D32Reader
import com.homm3.livewallpaper.parser.def.DefSprite
import com.homm3.livewallpaper.parser.lod.LodArchive
import com.homm3.livewallpaper.parser.lod.LodEntry
import com.homm3.livewallpaper.parser.lod.LodReader
import java.io.ByteArrayInputStream
import java.util.Locale
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Loads sprites from HotA 1.8 LOD archives, where entry names are replaced by
 * FNV-1a hashes of the lowercased name. Needed names are resolved via [LodArchive.find].
 */
class Hota18SpriteLoader(private val base: LodSpriteLoader) {

    fun loadSprites(
        lodReader: LodReader,
        archive: LodArchive,
        neededNames: Set<String>,
        packer: PixmapPacker,
        aliases: Map<String, String>,
        onProgress: (String) -> Unit
    ): List<RegionInfo> {
        onProgress("Loading HotA 1.8 sprites...")
        val neededLower = neededNames.map { it.lowercase(Locale.ROOT) }.toSet()
        val defEntries = neededLower
            .mapNotNull { defName -> findDefEntry(archive, aliases, defName)?.let { defName to it } }
            .sortedBy { (_, entry) -> entry.offset }

        val defRegions = defEntries.flatMapIndexed { index, (defName, entry) ->
            if (index % PROGRESS_STEP == 0) onProgress("Loading HotA 1.8 sprites ($index)...")
            loadDefEntry(lodReader, entry, defName, packer)
        }

        // Factory terrains are always loaded, even if no map in the current batch references them.
        val terrainRegions = base.terrainPcxPrefixes.flatMap { (tilePrefix, terrainDefName) ->
            loadTerrainTiles(lodReader, archive, tilePrefix, terrainDefName, packer)
        }

        log.info("HotA 1.8: found ${defEntries.size} of ${neededLower.size} needed DEFs")
        onProgress("Loaded ${defEntries.size} HotA 1.8 sprites")
        return defRegions + terrainRegions
    }

    private fun findDefEntry(archive: LodArchive, aliases: Map<String, String>, defName: String): LodEntry? =
        archive.find(defName) ?: aliases[defName]?.let { archive.find(it) }

    private fun loadDefEntry(
        lodReader: LodReader,
        entry: LodEntry,
        defName: String,
        packer: PixmapPacker
    ): List<RegionInfo> = try {
        val bytes = lodReader.readFileContent(entry).readBytes()
        if (isD32(bytes)) {
            packD32Sprite(D32Reader(ByteArrayInputStream(bytes)).read(), defName, packer)
        } else {
            base.loadDef(ByteArrayInputStream(bytes), defName, packer)
        }
    } catch (e: Throwable) {
        log.log(Level.WARNING, "Failed to load HotA 1.8 $defName (entry ${entry.name})", e)
        emptyList()
    }

    private fun isD32(bytes: ByteArray): Boolean =
        bytes.size >= D32_MAGIC_BYTES.size && bytes.copyOf(D32_MAGIC_BYTES.size).contentEquals(D32_MAGIC_BYTES)

    // -- Terrain PCX tiles -----------------------------------------------------

    /** HotA 1.8 stores each Factory terrain tile as a separate PCX: "<prefix>000.pcx", "<prefix>001.pcx", ... */
    private fun loadTerrainTiles(
        lodReader: LodReader,
        archive: LodArchive,
        tilePrefix: String,
        terrainDefName: String,
        packer: PixmapPacker
    ): List<RegionInfo> {
        val tileEntries = generateSequence(0) { it + 1 }
            .map { index -> archive.find("$tilePrefix${"%03d".format(index)}.pcx") }
            .takeWhile { it != null }
            .filterNotNull()
            .toList()
        val groupFilenames = tileEntries.map { it.name }
        log.info("HotA 1.8: loading ${tileEntries.size} PCX tiles as $terrainDefName")

        return tileEntries.flatMap { entry ->
            try {
                base.loadTerrainPcx(lodReader, entry, terrainDefName, groupFilenames, packer)
            } catch (e: Throwable) {
                log.log(Level.WARNING, "Failed to load terrain PCX tile ${entry.name} for $terrainDefName", e)
                emptyList()
            }
        }
    }

    // -- D32 -------------------------------------------------------------------

    private fun packD32Sprite(
        sprite: DefSprite,
        defName: String,
        packer: PixmapPacker
    ): List<RegionInfo> {
        val regionInfos = mutableListOf<RegionInfo>()
        val seenFrames = mutableSetOf<String>()

        for (group in sprite.groups) {
            for (frame in group.frames) {
                val frameKey = defName + frame.frameName
                if (frameKey in seenFrames) continue
                seenFrames.add(frameKey)

                if (frame.width == 0 || frame.height == 0) continue

                val packerName = "$defName/${frame.frameName}"
                val pixmap = Pixmap(frame.width, frame.height, Pixmap.Format.RGBA8888)
                val buffer = pixmap.pixels
                buffer.put(frame.data)
                buffer.flip()
                packer.pack(packerName, pixmap)
                pixmap.dispose()

                val defNameNoExt = defName.removeSuffix(".def")
                group.filenames.forEachIndexed { index, fileName ->
                    if (fileName != frame.frameName) return@forEachIndexed
                    regionInfos.add(
                        RegionInfo(
                            packerName = packerName,
                            regionName = defNameNoExt,
                            regionIndex = index,
                            width = frame.width,
                            height = frame.height,
                            fullWidth = frame.fullWidth,
                            fullHeight = frame.fullHeight,
                            x = frame.x,
                            y = frame.y,
                            isTerrain = false
                        )
                    )
                }
            }
        }
        return regionInfos
    }

    // -- Constants & data ------------------------------------------------------

    private val D32_MAGIC_BYTES = byteArrayOf(0x44, 0x33, 0x32, 0x46) // "D32F"

    companion object {
        private const val PROGRESS_STEP = 50
        private val log = Logger.getLogger(Hota18SpriteLoader::class.java.name)
    }
}
