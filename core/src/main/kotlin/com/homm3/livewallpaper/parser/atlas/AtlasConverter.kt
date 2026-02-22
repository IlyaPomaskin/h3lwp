package com.homm3.livewallpaper.parser.atlas

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.homm3.livewallpaper.parser.def.DefFrame
import com.homm3.livewallpaper.parser.def.DefReader
import com.homm3.livewallpaper.parser.lod.LodEntry
import com.homm3.livewallpaper.parser.lod.LodFileType
import com.homm3.livewallpaper.parser.lod.LodReader
import com.homm3.livewallpaper.parser.pcx.PcxReader
import java.io.File
import java.io.InputStream
import java.util.logging.Level
import java.util.logging.Logger

class InvalidFileException(msg: String) : Exception(msg)
class OutputFileWriteException(msg: String) : Exception(msg)

class AtlasConverter(
    lodFileInputStream: InputStream,
    private val outputDirectory: File,
    private val atlasName: String,
    private val minimalDefCount: Int = 1000
) {
    private val packer = PixmapPacker(2048, 2048, Pixmap.Format.RGBA4444, 0, false)
    private val lodReader = LodReader(lodFileInputStream)
    private val atlasPacker = AtlasPacker(packer)
    private val atlasWriter = AtlasWriter(packer, outputDirectory, atlasName)

    private val fixedPalette = byteArrayOf(
        0, 0, 0,
        0, 0, 0,
        0, 0, 0,
        0, 0, 0,
        0, 0, 0,
        0x80.toByte(), 0x80.toByte(), 0x80.toByte(),
        0, 0, 0,
        0, 0, 0
    )

    private val ignoredFiles = listOf(
        "arrow.def", "avwattack.def", "adag.def",
        "avwmon1.def", "avwmon2.def", "avwmon3.def", "avwmon4.def", "avwmon5.def", "avwmon6.def",
        "avarnd1.def", "avarnd2.def", "avarnd3.def", "avarnd4.def", "avarnd5.def", "avtrndm0.def"
    )

    private val terrainPcxPrefixes = mapOf(
        "wstlt" to "wastlnd.def",
        "hglnt" to "highlnd.def"
    )

    @Throws(InvalidFileException::class, OutputFileWriteException::class)
    fun convert(onProgress: (String) -> Unit = {}) {
        onProgress("Reading LOD archive...")
        runCatching(::readFilesList)
            .onFailure { throw InvalidFileException("Can't parse. Try another file.") }
            .also { files ->
                val list = files.getOrDefault(emptyList())
                val defCount = list.count { it.name.endsWith(".def", true) }
                val pcxCount = list.count { it.name.endsWith(".pcx", true) }
                val types = list.mapNotNull { it.fileType }.distinct().sorted()
                val nullTypeCount = list.count { it.fileType == null }
                log.info("LOD entries: ${list.size} total ($defCount DEF, $pcxCount PCX), types=$types, nullType=$nullTypeCount")
                log.info("LOD file names: ${list.map { it.name }}")
                onProgress("Reading sprite definitions (${list.size} files)...")
            }
            .mapCatching { readDefs(it, onProgress) }
            .onFailure { e ->
                log.log(Level.SEVERE, "readDefs failed", e)
                throw InvalidFileException("Can't read content. Try another file.")
            }
            .also { defs ->
                val frames = defs.getOrDefault(emptyList())
                log.info("Read ${frames.size} frames from LOD")
                if (frames.size < minimalDefCount) {
                    throw InvalidFileException("Wrong file selected. Try another file.")
                }
            }
            .also { onProgress("Packing atlas frames...") }
            .mapCatching(atlasPacker::packFrames)
            .also { packed ->
                val count = packed.getOrDefault(mutableMapOf()).size
                log.info("Packed $count unique sprites")
                onProgress("Writing atlas files...")
            }
            .mapCatching { sprites ->
                atlasWriter.writePackerContent(sprites) { current, total ->
                    onProgress("Writing atlas files... ($current/$total)")
                }
            }
            .onFailure { e ->
                log.log(Level.SEVERE, "writePackerContent failed", e)
                throw OutputFileWriteException("Can't save files. Check free space.")
            }
        onProgress("Done!")
    }

    private fun readFilesList(): List<LodEntry> {
        return lodReader
            .read()
            .files
            .filter { lodFile ->
                val isDef = lodFile.name.endsWith(".def", true)
                val isPcx = lodFile.name.endsWith(".pcx", true)
                val isIgnored = ignoredFiles.any { it.equals(lodFile.name, true) }
                (isDef || isPcx) && !isIgnored
            }
    }

    private fun readDefs(lodFiles: List<LodEntry>, onProgress: (String) -> Unit = {}): List<PackableFrame> {
        // Build group filenames for terrain PCX tiles, sorted by tile index
        val terrainGroupFilenames = buildTerrainGroupFilenames(lodFiles)

        // Classify entries into those we want to read
        data class ReadTask(val entry: LodEntry, val terrainDefName: String?)

        val hotaDefPrefixes = listOf(
            "av", "ah", "crates", "tp", "frog_", "rooster_", "zreef", "grsmnt"
        )
        val hotaPcxPrefixes = listOf(
            "zsand", "pig", "miniftws", "swpmsh", "zwat", "zice", "fvwnd"
        )
        val hotaExtraNames = setOf(
            "h4wt2f", "swpmsh05", "swpmsh01", "miniftsw", "swpmsh06", "miniftsn",
            "zsand00", "zsand01", "zsand03", "zsand06", "zwat05", "jaws",
            "4lvlxshrn", "mntswp02", "mntswp04", "mntswpab", "mntswp01", "mntswp03",
            "mntswp05", "zice06", "zice07", "zice01", "zsand05", "zsand02", "hatel",
            "zwat06", "fortnixr", "swshcamp", "nimphfo", "wt_seerh", "fvwnd05",
            "fvwnd06", "fvwnd00", "fvwnd01", "fvwnd03", "fvwnd02", "zwat00", "portu",
            "sanct_wt", "wt_tvrn01", "watermag", "zwat01", "swddtree", "miniftsd",
            "fortnix", "nestas", "swpmsh03", "zwat02", "swpmsh02", "swvflw03",
            "swpmsh04", "swvflw02", "swvflw01", "swvflw05", "swvflw04", "avwccoat",
            "zsand07", "zsand04", "fregat", "mntswp06", "zwat07", "minifthl",
            "minifort", "pig", "miniftws", "fvwnd04", "hapcave", "miniftrg",
            "fvwnd07", "h4wt1f", "h4wt3f", "towerza", "zwat04", "zwat03", "zice04",
            "zice08", "haexit01", "mntswpcr", "haspwtpl", "miniftlv", "zice02",
            "zice03", "mntswpgl", "miniftdr", "zice05", "hasphtpl"
        )

        val readTasks = lodFiles.mapNotNull { file ->
            val lowerBase = file.name.lowercase().substringBefore(".")
            if (file.name.endsWith(".pcx", true)) {
                val terrainDefName = terrainPcxDefName(file.name)
                val isHotaPcx = file.fileType == null
                    && (hotaPcxPrefixes.any { lowerBase.startsWith(it) } || lowerBase in hotaExtraNames)
                when {
                    terrainDefName != null -> ReadTask(file, terrainDefName)
                    isHotaPcx -> ReadTask(file, null)
                    else -> null
                }
            } else {
                val isTerrain = file.fileType == LodFileType.TERRAIN
                val isExtraSprite = file.fileType == LodFileType.SPRITE
                    && file.name.startsWith("av", true)
                val isMapSprite = file.fileType == LodFileType.MAP
                // HotA.lod has unknown file type IDs (null); include by prefix or name
                val isHotaDef = file.fileType == null
                    && file.name.endsWith(".def", true)
                    && (hotaDefPrefixes.any { lowerBase.startsWith(it) } || lowerBase in hotaExtraNames)
                if (isTerrain || isExtraSprite || isMapSprite || isHotaDef) {
                    ReadTask(file, null)
                } else {
                    null
                }
            }
        }

        val skippedCount = lodFiles.size - readTasks.size
        log.info("Processing ${readTasks.size} entries ($skippedCount skipped by filter)")

        // Process all entries in offset order for sequential LOD reading
        val sorted = readTasks.sortedBy { it.entry.offset }
        val total = sorted.size
        var processed = 0
        var failed = 0
        return sorted
            .flatMap { task ->
                log.info("Reading ${task.entry.name} (type=${task.entry.fileType}, size=${task.entry.size}, offset=${task.entry.offset})")
                try {
                    when {
                        task.entry.name.endsWith(".def", true) -> readDefFromLod(task.entry)
                        task.terrainDefName != null -> readTerrainPcxFromLod(
                            task.entry,
                            task.terrainDefName,
                            terrainGroupFilenames[task.terrainDefName]!!
                        )
                        else -> readPcxFromLod(task.entry)
                    }.also {
                        processed++
                        onProgress("Reading sprite definitions ($processed/$total)...")
                    }
                } catch (e: Throwable) {
                    failed++
                    log.log(Level.WARNING, "Failed to read ${task.entry.name} (type=${task.entry.fileType}, size=${task.entry.size})", e)
                    onProgress("Reading sprite definitions ($processed/$total)...")
                    emptyList()
                }
            }
            .distinctBy { it.defName + it.frame.frameName }
            .also { log.info("Read complete: $processed ok, $failed failed, ${it.size} unique frames") }
    }

    private fun buildTerrainGroupFilenames(lodFiles: List<LodEntry>): Map<String, List<String>> {
        val groups = mutableMapOf<String, MutableList<Pair<Int, String>>>()
        lodFiles.forEach { file ->
            if (!file.name.endsWith(".pcx", true)) return@forEach
            val defName = terrainPcxDefName(file.name) ?: return@forEach
            groups.getOrPut(defName) { mutableListOf() }
                .add(extractTileIndex(file.name) to file.name)
        }
        return groups.mapValues { (_, tiles) ->
            tiles.sortedBy { it.first }.map { it.second }
        }
    }

    private fun readPcxFromLod(entry: LodEntry): List<PackableFrame> {
        val stream = lodReader.readFileContent(entry)
        val pcxReader = PcxReader(stream)
        val pcx = pcxReader.read()

        val palette = if (pcx.palette != null) {
            val p = pcx.palette.clone()
            System.arraycopy(fixedPalette, 0, p, 0, fixedPalette.size)
            p
        } else {
            null
        }

        val frame = DefFrame(
            entry.name, pcx.width, pcx.height,
            pcx.width, pcx.height, 0, 0, pcx.data
        )
        return listOf(PackableFrame(frame, entry.name, entry.fileType, palette, listOf(entry.name)))
    }

    private fun readTerrainPcxFromLod(
        entry: LodEntry,
        defName: String,
        groupFilenames: List<String>
    ): List<PackableFrame> {
        val stream = lodReader.readFileContent(entry)
        val pcxReader = PcxReader(stream)
        val pcx = pcxReader.read()

        val palette = if (pcx.palette != null) {
            val p = pcx.palette.clone()
            System.arraycopy(fixedPalette, 0, p, 0, fixedPalette.size)
            p
        } else {
            null
        }

        val frame = DefFrame(
            entry.name, pcx.width, pcx.height,
            pcx.width, pcx.height, 0, 0, pcx.data
        )
        return listOf(PackableFrame(frame, defName, LodFileType.TERRAIN, palette, groupFilenames))
    }

    private fun terrainPcxDefName(name: String): String? {
        val lowerName = name.lowercase()
        return terrainPcxPrefixes.entries.firstOrNull { (prefix, _) ->
            lowerName.startsWith(prefix)
        }?.value
    }

    private fun extractTileIndex(name: String): Int {
        val baseName = name.substringBefore(".")
        val digits = baseName.takeLastWhile { it.isDigit() }
        return digits.toIntOrNull() ?: 0
    }

    private fun readDefFromLod(entry: LodEntry): List<PackableFrame> {
        val stream = lodReader.readFileContent(entry)
        val defReader = DefReader(stream)
        val def = defReader.read()
        val palette = def.rawPalette.clone()
        System.arraycopy(fixedPalette, 0, palette, 0, fixedPalette.size)

        return def.groups.flatMap { group ->
            group.frames.map { frame ->
                PackableFrame(frame, entry.name, entry.fileType, palette.clone(), group.filenames)
            }
        }
    }

    companion object {
        private val log = Logger.getLogger(AtlasConverter::class.java.name)
    }
}
