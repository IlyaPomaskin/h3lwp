package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.homm3.livewallpaper.parser.def.DefFrame
import com.homm3.livewallpaper.parser.def.DefReader
import com.homm3.livewallpaper.parser.lod.LodEntry
import com.homm3.livewallpaper.parser.lod.LodFileType
import com.homm3.livewallpaper.parser.lod.LodReader
import com.homm3.livewallpaper.parser.pcx.PcxReader
import java.io.InputStream
import java.util.Locale
import java.util.logging.Level
import java.util.logging.Logger

/**
 * One entry per AtlasRegion to create.
 * A single packer rectangle may produce multiple RegionInfos
 * (e.g. same frame at multiple indices in groupFilenames).
 */
data class RegionInfo(
    val packerName: String,
    val regionName: String,
    val regionIndex: Int,
    val width: Int,
    val height: Int,
    val fullWidth: Int,
    val fullHeight: Int,
    val x: Int,
    val y: Int,
    val isTerrain: Boolean
)

class LodSpriteLoader {
    // RGB values for special palette indices 0-7 (all black for shadow/transparency)
    private val fixedPalette = byteArrayOf(
        0, 0, 0, // 0: fully transparent
        0, 0, 0, // 1: shadow border
        0, 0, 0, // 2: shadow border
        0, 0, 0, // 3: shadow body
        0, 0, 0, // 4: shadow body
        0, 0, 0, // 5: selection/flag color (transparent in wallpaper)
        0, 0, 0, // 6: shadow below selection
        0, 0, 0  // 7: shadow border below selection
    )

    // Alpha values for special palette indices 0-7 (PNG tRNS chunk)
    private val transparent = byteArrayOf(
        0x00.toByte(), // 0: fully transparent
        0x40.toByte(), // 1: shadow border (alpha 64)
        0x40.toByte(), // 2: shadow border (alpha 64)
        0x80.toByte(), // 3: shadow body (alpha 128)
        0x80.toByte(), // 4: shadow body (alpha 128)
        0x00.toByte(), // 5: transparent (no player flags in wallpaper)
        0x80.toByte(), // 6: shadow below selection (alpha 128)
        0x40.toByte()  // 7: shadow border below selection (alpha 64)
    )

    private val paletteRotations = hashMapOf(
        "watrtl.def" to listOf(229 to 241, 242 to 254),
        "lavatl.def" to listOf(246 to 254),
        "clrrvr.def" to listOf(183 to 195, 195 to 201),
        "mudrvr.def" to listOf(183 to 189, 240 to 246),
        "lavrvr.def" to listOf(240 to 248)
    )

    private val terrainDefNames = setOf(
        "dirttl.def", "sandtl.def", "grastl.def", "snowtl.def", "swmptl.def",
        "rougtl.def", "subbtl.def", "lavatl.def", "watrtl.def", "rocktl.def",
        "highlnd.def", "wastlnd.def",
        "clrrvr.def", "icyrvr.def", "mudrvr.def", "lavrvr.def",
        "dirtrd.def", "gravrd.def", "cobbrd.def",
        "edg.def"
    )

    private val terrainPcxPrefixes = mapOf(
        "wstlt" to "wastlnd.def",
        "hglnt" to "highlnd.def"
    )

    fun loadSprites(
        lodStream: InputStream,
        neededNames: Set<String>,
        packer: PixmapPacker,
        onProgress: (String) -> Unit = {}
    ): List<RegionInfo> {
        val lodReader = LodReader(lodStream)
        val archive = lodReader.read()

        val neededLower = neededNames.map { it.lowercase(Locale.ROOT) }.toSet()

        val terrainGroupFilenames = buildTerrainGroupFilenames(archive.files, neededLower)
        val entriesToRead = archive.files.filter { entry ->
            val lowerName = entry.name.lowercase(Locale.ROOT)
            val baseName = lowerName.substringBefore(".")
            when {
                lowerName.endsWith(".def") -> lowerName in neededLower
                lowerName.endsWith(".pcx") -> {
                    val terrainDefName = terrainPcxDefName(lowerName)
                    if (terrainDefName != null) {
                        terrainDefName in neededLower
                    } else {
                        "$baseName.def" in neededLower || lowerName in neededLower
                    }
                }
                else -> false
            }
        }.sortedBy { it.offset }

        onProgress("Loading ${entriesToRead.size} sprites...")
        val allRegionInfos = mutableListOf<RegionInfo>()
        var processed = 0

        for (entry in entriesToRead) {
            try {
                val lowerName = entry.name.lowercase(Locale.ROOT)
                when {
                    lowerName.endsWith(".def") -> {
                        allRegionInfos.addAll(loadDef(lodReader, entry, packer))
                    }
                    else -> {
                        val terrainDefName = terrainPcxDefName(lowerName)
                        if (terrainDefName != null) {
                            val groupFilenames = terrainGroupFilenames[terrainDefName] ?: listOf(entry.name)
                            allRegionInfos.addAll(
                                loadTerrainPcx(lodReader, entry, terrainDefName, groupFilenames, packer)
                            )
                        } else {
                            allRegionInfos.addAll(loadPcx(lodReader, entry, packer))
                        }
                    }
                }
                processed++
                if (processed % 50 == 0) {
                    onProgress("Loading sprites ($processed/${entriesToRead.size})...")
                }
            } catch (e: Throwable) {
                log.log(Level.WARNING, "Failed to load ${entry.name}", e)
            }
        }

        onProgress("Loaded $processed sprites")
        return allRegionInfos
    }

    private fun buildTerrainGroupFilenames(files: List<LodEntry>, neededLower: Set<String>): Map<String, List<String>> {
        val groups = mutableMapOf<String, MutableList<Pair<Int, String>>>()
        for (file in files) {
            val lowerName = file.name.lowercase(Locale.ROOT)
            if (!lowerName.endsWith(".pcx")) continue
            val defName = terrainPcxDefName(lowerName) ?: continue
            if (defName !in neededLower) continue
            groups.getOrPut(defName) { mutableListOf() }
                .add(extractTileIndex(file.name) to file.name)
        }
        return groups.mapValues { (_, tiles) ->
            tiles.sortedBy { it.first }.map { it.second }
        }
    }

    private fun terrainPcxDefName(name: String): String? {
        val lowerName = name.lowercase(Locale.ROOT)
        return terrainPcxPrefixes.entries.firstOrNull { (prefix, _) ->
            lowerName.startsWith(prefix)
        }?.value
    }

    private fun extractTileIndex(name: String): Int {
        val baseName = name.substringBefore(".")
        val digits = baseName.takeLastWhile { it.isDigit() }
        return digits.toIntOrNull() ?: 0
    }

    private fun loadDef(
        lodReader: LodReader,
        entry: LodEntry,
        packer: PixmapPacker
    ): List<RegionInfo> {
        val stream = lodReader.readFileContent(entry)
        val def = DefReader(stream).read()
        val palette = def.rawPalette.clone()
        System.arraycopy(fixedPalette, 0, palette, 0, fixedPalette.size)

        val defNameLower = entry.name.lowercase(Locale.ROOT)
        val isTerrain = defNameLower in terrainDefNames
        val regionInfos = mutableListOf<RegionInfo>()
        val seenFrames = mutableSetOf<String>()

        if (isTerrain) {
            // HotA logic: flatten all groups into one global filename list.
            // Tile variant index = position in the combined list.
            // Handles both single-group H3 DEFs and multi-group HotA DEFs.
            val allFilenames = def.groups.flatMap { it.filenames }
            for (group in def.groups) {
                for (frame in group.frames) {
                    if (frame.frameName in seenFrames) continue
                    seenFrames.add(frame.frameName)
                    regionInfos.addAll(
                        packTerrainFrame(frame, defNameLower, palette.clone(), allFilenames, packer)
                    )
                }
            }
            return regionInfos
        }

        log.fine("DEF: $defNameLower type=${entry.fileType} groups=${def.groups.size} fullW=${def.fullWidth} fullH=${def.fullHeight}")
        for ((gi, group) in def.groups.withIndex()) {
            log.fine("  group[$gi] filenames=${group.filenames.size}: ${group.filenames.take(8)}")
            for (frame in group.frames) {
                log.fine("    frame=${frame.frameName} w=${frame.width} h=${frame.height} fullW=${frame.fullWidth} fullH=${frame.fullHeight} x=${frame.x} y=${frame.y} dataSize=${frame.data.size}")
                val frameKey = defNameLower + frame.frameName
                if (frameKey in seenFrames) continue
                seenFrames.add(frameKey)

                regionInfos.addAll(
                    packObjectFrame(frame, defNameLower, palette, group.filenames, packer)
                )
            }
        }
        for (ri in regionInfos) {
            log.fine("  -> region: name=${ri.regionName} idx=${ri.regionIndex} w=${ri.width} h=${ri.height} fullW=${ri.fullWidth} fullH=${ri.fullHeight} x=${ri.x} y=${ri.y}")
        }
        return regionInfos
    }

    private fun loadTerrainPcx(
        lodReader: LodReader,
        entry: LodEntry,
        terrainDefName: String,
        groupFilenames: List<String>,
        packer: PixmapPacker
    ): List<RegionInfo> {
        val stream = lodReader.readFileContent(entry)
        val pcx = PcxReader(stream).read()

        val palette = if (pcx.palette != null) {
            val p = pcx.palette.clone()
            System.arraycopy(fixedPalette, 0, p, 0, fixedPalette.size)
            p
        } else {
            return emptyList()
        }

        val frame = DefFrame(
            entry.name, pcx.width, pcx.height,
            pcx.width, pcx.height, 0, 0, pcx.data
        )

        return packTerrainFrame(frame, terrainDefName, palette, groupFilenames, packer)
    }

    private fun loadPcx(
        lodReader: LodReader,
        entry: LodEntry,
        packer: PixmapPacker
    ): List<RegionInfo> {
        val stream = lodReader.readFileContent(entry)
        val pcx = PcxReader(stream).read()

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

        return packObjectFrame(frame, entry.name, palette, listOf(entry.name), packer)
    }

    /**
     * Pack a terrain frame with palette rotation.
     * Produces one packer entry per rotation step, and one RegionInfo per
     * (groupFilename match × rotation step).
     *
     * Old atlas naming: region name = "defNameNoExt/groupIndex", atlas index = rotationStep
     */
    private fun packTerrainFrame(
        frame: DefFrame,
        defName: String,
        palette: ByteArray,
        groupFilenames: List<String>,
        packer: PixmapPacker
    ): List<RegionInfo> {
        val regionInfos = mutableListOf<RegionInfo>()
        val initialPalette = palette.clone()
        val rotations = paletteRotations[defName] ?: emptyList()
        val defNameNoExt = defName.removeSuffix(".def")
        var rotationStep = 0

        do {
            val pixmap = makeTerrainPixmap(frame, palette)
            val packerName = "$defName/${frame.frameName}/$rotationStep"
            packer.pack(packerName, pixmap)
            pixmap.dispose()

            // Create a RegionInfo for each position this frame occupies in groupFilenames
            groupFilenames.forEachIndexed { index, fileName ->
                if (fileName != frame.frameName) return@forEachIndexed
                regionInfos.add(
                    RegionInfo(
                        packerName = packerName,
                        regionName = "$defNameNoExt/$index",
                        regionIndex = rotationStep,
                        width = frame.fullWidth,
                        height = frame.fullHeight,
                        fullWidth = frame.fullWidth,
                        fullHeight = frame.fullHeight,
                        x = 0,
                        y = 0,
                        isTerrain = true
                    )
                )
            }

            rotations.forEach { rotatePalette(palette, it.first, it.second) }
            rotationStep++
        } while (!initialPalette.contentEquals(palette))

        return regionInfos
    }

    /**
     * Pack an object/sprite frame.
     * Produces one packer entry, and one RegionInfo per matching position in groupFilenames.
     *
     * Old atlas naming: region name = "defNameNoExt" (lowercase, no .def), atlas index = groupIndex
     */
    private fun packObjectFrame(
        frame: DefFrame,
        defName: String,
        palette: ByteArray?,
        groupFilenames: List<String>,
        packer: PixmapPacker
    ): List<RegionInfo> {
        // Skip zero-dimension frames (old pipeline threw in makePixmap and silently skipped them)
        if (frame.width == 0 || frame.height == 0) return emptyList()

        val packerName = "$defName/${frame.frameName}"
        val pixmap = makePixmap(frame, palette)
        packer.pack(packerName, pixmap)
        pixmap.dispose()

        val defNameNoExt = defName.lowercase(Locale.ROOT).removeSuffix(".def")
        val regionInfos = mutableListOf<RegionInfo>()

        groupFilenames.forEachIndexed { index, fileName ->
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

        return regionInfos
    }

    private fun makePixmap(frame: DefFrame, palette: ByteArray?): Pixmap {
        if (palette == null) {
            return makeRgbPixmap(frame)
        }
        val encoder = PngEncoderInternal()
        val pngData = encoder.create(
            frame.width, frame.height, palette, transparent, frame.data
        )
        return Pixmap(pngData, 0, pngData.size)
    }

    private fun makeRgbPixmap(frame: DefFrame): Pixmap {
        val pixmap = Pixmap(frame.width, frame.height, Pixmap.Format.RGB888)
        val buffer = pixmap.pixels
        buffer.put(frame.data)
        buffer.flip()
        return pixmap
    }

    private fun makeTerrainPixmap(frame: DefFrame, palette: ByteArray): Pixmap {
        val image = makePixmap(frame, palette)
        val fullImage = Pixmap(frame.fullWidth, frame.fullHeight, Pixmap.Format.RGBA4444)
        fullImage.drawPixmap(image, frame.x, frame.y)
        image.dispose()
        return fullImage
    }

    private fun rotatePalette(array: ByteArray, from: Int, to: Int) {
        val stepSize = 3
        val fromStep = from * stepSize
        val toStep = to * stepSize
        val left = array.copyOfRange(fromStep, fromStep + stepSize)
        val right = array.copyOfRange(fromStep + stepSize, toStep)
        System.arraycopy(right, 0, array, fromStep, right.size)
        System.arraycopy(left, 0, array, fromStep + right.size, left.size)
    }

    companion object {
        private val log = Logger.getLogger(LodSpriteLoader::class.java.name)
    }
}

internal class PngEncoderInternal {
    private val output = java.io.ByteArrayOutputStream()

    fun create(width: Int, height: Int, palette: ByteArray, transparent: ByteArray, data: ByteArray): ByteArray {
        output.reset()
        output.write(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10))
        writeChunk("IHDR", createIHDR(width, height))
        writeChunk("PLTE", palette)
        writeChunk("tRNS", transparent)
        writeChunk("IDAT", createIDAT(data, width, height))
        writeChunk("IEND", ByteArray(0))
        return output.toByteArray()
    }

    private fun createIHDR(width: Int, height: Int): ByteArray {
        val ihdr = java.io.ByteArrayOutputStream()
        ihdr.write(java.nio.ByteBuffer.allocate(4).putInt(width).array())
        ihdr.write(java.nio.ByteBuffer.allocate(4).putInt(height).array())
        ihdr.write(8)
        ihdr.write(3)
        ihdr.write(0)
        ihdr.write(0)
        ihdr.write(0)
        return ihdr.toByteArray()
    }

    private fun createIDAT(data: ByteArray, width: Int, height: Int): ByteArray {
        val scanlines = java.io.ByteArrayOutputStream()
        for (i in 0 until height) {
            scanlines.write(0)
            scanlines.write(data.copyOfRange(width * i, width * i + width))
        }
        val input = scanlines.toByteArray()
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        val deflater = java.util.zip.Deflater()
        deflater.setInput(input)
        deflater.finish()
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            out.write(buffer, 0, count)
        }
        deflater.end()
        return out.toByteArray()
    }

    private fun writeChunk(type: String, content: ByteArray) {
        output.write(java.nio.ByteBuffer.allocate(4).putInt(content.size).array())
        output.write(type.toByteArray())
        output.write(content)
        output.write(java.nio.ByteBuffer.allocate(4).putInt(getCRC(type, content).toInt()).array())
    }

    private fun getCRC(type: String, content: ByteArray): Long {
        val crc32 = java.util.zip.CRC32()
        crc32.update(type.toByteArray(), 0, type.toByteArray().size)
        crc32.update(content, 0, content.size)
        return crc32.value
    }
}
