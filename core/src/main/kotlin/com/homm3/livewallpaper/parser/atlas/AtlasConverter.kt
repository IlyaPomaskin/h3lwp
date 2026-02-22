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
            .also { onProgress("Reading sprite definitions...") }
            .mapCatching(::readDefs)
            .onFailure { throw InvalidFileException("Can't read content. Try another file.") }
            .also { defs ->
                if (defs.getOrDefault(emptyList()).size < minimalDefCount) {
                    throw InvalidFileException("Wrong file selected. Try another file.")
                }
            }
            .also { onProgress("Packing atlas frames...") }
            .mapCatching(atlasPacker::packFrames)
            .also { onProgress("Writing atlas files...") }
            .mapCatching { sprites ->
                atlasWriter.writePackerContent(sprites) { current, total ->
                    onProgress("Writing atlas files... ($current/$total)")
                }
            }
            .onFailure { throw OutputFileWriteException("Can't save files. Check free space.") }
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

    private fun readDefs(lodFiles: List<LodEntry>): List<PackableFrame> {
        // Build group filenames for terrain PCX tiles, sorted by tile index
        val terrainGroupFilenames = buildTerrainGroupFilenames(lodFiles)

        // Classify entries into those we want to read
        data class ReadTask(val entry: LodEntry, val terrainDefName: String?)

        val readTasks = lodFiles.mapNotNull { file ->
            if (file.name.endsWith(".pcx", true)) {
                ReadTask(file, terrainPcxDefName(file.name))
            } else {
                val isTerrain = file.fileType == LodFileType.TERRAIN
                val isExtraSprite = file.fileType == LodFileType.SPRITE
                    && file.name.startsWith("av", true)
                val isMapSprite = file.fileType == LodFileType.MAP
                if (isTerrain || isExtraSprite || isMapSprite) {
                    ReadTask(file, null)
                } else {
                    null
                }
            }
        }

        // Process all entries in offset order for sequential LOD reading
        return readTasks
            .sortedBy { it.entry.offset }
            .flatMap { task ->
                when {
                    task.entry.name.endsWith(".def", true) -> readDefFromLod(task.entry)
                    task.terrainDefName != null -> readTerrainPcxFromLod(
                        task.entry,
                        task.terrainDefName,
                        terrainGroupFilenames[task.terrainDefName]!!
                    )
                    else -> readPcxFromLod(task.entry)
                }
            }
            .distinctBy { it.defName + it.frame.frameName }
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
}
