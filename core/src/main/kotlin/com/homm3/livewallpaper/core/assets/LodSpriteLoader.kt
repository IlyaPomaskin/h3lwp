package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.homm3.livewallpaper.parser.def.D32Reader
import com.homm3.livewallpaper.parser.def.DefFrame
import com.homm3.livewallpaper.parser.def.DefReader
import com.homm3.livewallpaper.parser.def.DefSprite
import com.homm3.livewallpaper.parser.lod.LodEntry
import com.homm3.livewallpaper.parser.lod.LodReader
import com.homm3.livewallpaper.parser.pcx.PcxReader
import java.io.ByteArrayInputStream
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
    // Expected source colors for special palette indices 0-7 (from VCMI)
    // H3 uses 16-bit color (565 RGB) so actual values may differ by up to 7
    private val sourceColors = arrayOf(
        intArrayOf(0, 255, 255),     // 0: cyan (transparency)
        intArrayOf(255, 150, 255),   // 1: light magenta (shadow border)
        intArrayOf(255, 100, 255),   // 2: mid magenta (shadow border) - fuzzy matched
        intArrayOf(255, 50, 255),    // 3: dark magenta (shadow body) - fuzzy matched
        intArrayOf(255, 0, 255),     // 4: pure magenta (shadow body)
        intArrayOf(255, 255, 0),     // 5: yellow (selection/flag)
        intArrayOf(180, 0, 255),     // 6: purple (shadow below selection)
        intArrayOf(0, 255, 0)        // 7: green (shadow border below selection)
    )

    // Target alpha for each special index
    private val targetAlpha = byteArrayOf(
        0x00.toByte(), // 0: fully transparent
        0x40.toByte(), // 1: shadow border (alpha 64)
        0x40.toByte(), // 2: shadow border (alpha 64)
        0x80.toByte(), // 3: shadow body (alpha 128)
        0x80.toByte(), // 4: shadow body (alpha 128)
        0xFF.toByte(), // 5: flag/selection — neutral gray (no player ownership in wallpaper)
        0x80.toByte(), // 6: shadow below selection (alpha 128)
        0x40.toByte()  // 7: shadow border below selection (alpha 64)
    )

    // Neutral flag color (VCMI neutral: 0x84, 0x84, 0x84)
    private val neutralFlagColor = intArrayOf(0x84, 0x84, 0x84)

    // Known flag colors used for detection: yellow marker + 8 player colors + neutral gray
    // Castle/mine DEFs have one of these at index 5 instead of the standard yellow
    // All matched entries are replaced with neutralFlagColor (gray)
    // Original VCMI player colors:
    //   red (255, 0, 0), blue (49, 82, 255), tan (156, 115, 82),
    //   green (66, 148, 41), orange (255, 132, 0), purple (140, 41, 165),
    //   teal (9, 156, 165), pink (198, 123, 140)
    private val flagColors = arrayOf(
        intArrayOf(255, 255, 0),     // yellow (standard marker)
        intArrayOf(255, 0, 0),       // red (player 0)
        intArrayOf(49, 82, 255),     // blue (player 1)
        intArrayOf(156, 115, 82),    // tan (player 2)
        intArrayOf(66, 148, 41),     // green (player 3)
        intArrayOf(255, 132, 0),     // orange (player 4)
        intArrayOf(140, 41, 165),    // purple (player 5)
        intArrayOf(9, 156, 165),     // teal (player 6)
        intArrayOf(198, 123, 140),   // pink (player 7)
        intArrayOf(0x84, 0x84, 0x84) // neutral gray
    )

    // Indices 0, 1, 4 are always replaced (shadow/transparency)
    // Index 5: replaced only if it matches a known flag/player color
    // Indices 2, 3, 6, 7 only replaced if original color matches expected source
    private val alwaysReplace = setOf(0, 1, 4)
    private val fuzzyReplace = setOf(2, 3, 6, 7)
    private val colorThreshold = 8

    private fun colorSimilar(palette: ByteArray, index: Int, expected: IntArray): Boolean {
        val offset = index * 3
        val r = palette[offset].toInt() and 0xFF
        val g = palette[offset + 1].toInt() and 0xFF
        val b = palette[offset + 2].toInt() and 0xFF
        return Math.abs(r - expected[0]) < colorThreshold &&
                Math.abs(g - expected[1]) < colorThreshold &&
                Math.abs(b - expected[2]) < colorThreshold
    }

    private fun isFlagColor(palette: ByteArray, index: Int): Boolean {
        return flagColors.any { colorSimilar(palette, index, it) }
    }

    /**
     * Apply special palette handling: overwrite palette RGB for shadow/flag indices,
     * and build the corresponding tRNS alpha array.
     * Indices 0,1,4: always replaced (shadow/transparency).
     * Index 5: replaced with neutral gray only if color matches a known flag/player color.
     * Indices 2,3,6,7: replaced only if color matches expected source (fuzzy).
     */
    private fun applySpecialPalette(originalPalette: ByteArray): Pair<ByteArray, ByteArray> {
        val palette = originalPalette.clone()
        val alpha = ByteArray(8) { 0xFF.toByte() } // default opaque

        for (i in 0 until 8) {
            val shouldReplace = when {
                i in alwaysReplace -> true
                i == 5 -> isFlagColor(originalPalette, i)
                i in fuzzyReplace -> colorSimilar(originalPalette, i, sourceColors[i])
                else -> false
            }
            if (shouldReplace) {
                val offset = i * 3
                if (i == 5) {
                    // Flag/selection: use neutral gray
                    palette[offset] = neutralFlagColor[0].toByte()
                    palette[offset + 1] = neutralFlagColor[1].toByte()
                    palette[offset + 2] = neutralFlagColor[2].toByte()
                } else {
                    palette[offset] = 0
                    palette[offset + 1] = 0
                    palette[offset + 2] = 0
                }
                alpha[i] = targetAlpha[i]
            }
        }
        // Replace any sourceColor found anywhere in the full palette with neutral gray
        for (i in 0 until palette.size / 3) {
            if (sourceColors.any { colorSimilar(palette, i, it) }) {
                val offset = i * 3
                palette[offset] = neutralFlagColor[0].toByte()
                palette[offset + 1] = neutralFlagColor[1].toByte()
                palette[offset + 2] = neutralFlagColor[2].toByte()
            }
        }

        return palette to alpha
    }

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

        if (archive.isHota18) {
            return loadHota18Sprites(lodReader, archive, neededNames, packer, onProgress)
        }

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
        val (palette, alpha) = applySpecialPalette(def.rawPalette)

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
                        packTerrainFrame(frame, defNameLower, palette.clone(), alpha, allFilenames, packer)
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
                    packObjectFrame(frame, defNameLower, palette, alpha, group.filenames, packer)
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

        if (pcx.palette == null) return emptyList()
        val (palette, alpha) = applySpecialPalette(pcx.palette)

        val frame = DefFrame(
            entry.name, pcx.width, pcx.height,
            pcx.width, pcx.height, 0, 0, pcx.data
        )

        return packTerrainFrame(frame, terrainDefName, palette, alpha, groupFilenames, packer)
    }

    private fun loadPcx(
        lodReader: LodReader,
        entry: LodEntry,
        packer: PixmapPacker
    ): List<RegionInfo> {
        val stream = lodReader.readFileContent(entry)
        val pcx = PcxReader(stream).read()

        val paletteAndAlpha = if (pcx.palette != null) applySpecialPalette(pcx.palette) else null

        val frame = DefFrame(
            entry.name, pcx.width, pcx.height,
            pcx.width, pcx.height, 0, 0, pcx.data
        )

        return packObjectFrame(frame, entry.name, paletteAndAlpha?.first, paletteAndAlpha?.second, listOf(entry.name), packer)
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
        alpha: ByteArray,
        groupFilenames: List<String>,
        packer: PixmapPacker
    ): List<RegionInfo> {
        val regionInfos = mutableListOf<RegionInfo>()
        val initialPalette = palette.clone()
        val rotations = paletteRotations[defName] ?: emptyList()
        val defNameNoExt = defName.removeSuffix(".def")
        var rotationStep = 0

        do {
            val pixmap = makeTerrainPixmap(frame, palette, alpha)
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
        alpha: ByteArray?,
        groupFilenames: List<String>,
        packer: PixmapPacker
    ): List<RegionInfo> {
        // Skip zero-dimension frames (old pipeline threw in makePixmap and silently skipped them)
        if (frame.width == 0 || frame.height == 0) return emptyList()

        val packerName = "$defName/${frame.frameName}"
        val pixmap = makePixmap(frame, palette, alpha)
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

    private fun makePixmap(frame: DefFrame, palette: ByteArray?, alpha: ByteArray?): Pixmap {
        if (palette == null || alpha == null) {
            return makeRgbPixmap(frame)
        }
        val encoder = PngEncoderInternal()
        val pngData = encoder.create(
            frame.width, frame.height, palette, alpha, frame.data
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

    private fun makeTerrainPixmap(frame: DefFrame, palette: ByteArray, alpha: ByteArray): Pixmap {
        val image = makePixmap(frame, palette, alpha)
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

    private val D32_MAGIC_BYTES = byteArrayOf(0x44, 0x33, 0x32, 0x46) // "D32F" little-endian: 0x46323344

    private fun loadHota18Sprites(
        lodReader: LodReader,
        archive: com.homm3.livewallpaper.parser.lod.LodArchive,
        neededNames: Set<String>,
        packer: PixmapPacker,
        onProgress: (String) -> Unit
    ): List<RegionInfo> {
        val neededLower = neededNames.map { it.lowercase(Locale.ROOT) }.toSet()
        val sortedEntries = archive.files.sortedBy { it.offset }

        onProgress("Loading HotA 1.8 sprites...")
        val allRegionInfos = mutableListOf<RegionInfo>()
        var processed = 0

        for (entry in sortedEntries) {
            try {
                val stream = lodReader.readFileContent(entry)
                val bytes = stream.readBytes()
                if (bytes.size < 4) continue

                val isD32 = bytes[0] == D32_MAGIC_BYTES[0] &&
                        bytes[1] == D32_MAGIC_BYTES[1] &&
                        bytes[2] == D32_MAGIC_BYTES[2] &&
                        bytes[3] == D32_MAGIC_BYTES[3]

                // Extract DEF name from first frame name
                val defName = extractDefNameFromBytes(bytes, isD32) ?: continue
                if (defName !in neededLower) continue

                if (isD32) {
                    val d32 = D32Reader(ByteArrayInputStream(bytes)).read()
                    allRegionInfos.addAll(packD32Sprite(d32, defName, packer))
                } else {
                    allRegionInfos.addAll(loadDefFromBytes(bytes, defName, packer))
                }

                processed++
                if (processed % 50 == 0) {
                    onProgress("Loading HotA 1.8 sprites ($processed)...")
                }
            } catch (e: Throwable) {
                log.log(Level.WARNING, "Failed to load HotA 1.8 entry ${entry.name}", e)
            }
        }

        onProgress("Loaded $processed HotA 1.8 sprites")
        return allRegionInfos
    }

    private fun extractDefNameFromBytes(bytes: ByteArray, isD32: Boolean): String? {
        // Extract first frame name from the binary data
        val frameNameOffset = if (isD32) {
            // D32: 32 (header) + 4+4+4+4 (group header) = 48
            48
        } else {
            // Classic DEF: 16 (header) + 768 (palette) + 4+4+8 (group header start) = 800
            // But group header: 4(type) + 4(count) + 8(unknown) = 16, then filenames start
            // So: 16 + 768 + 16 = 800
            800
        }
        if (bytes.size <= frameNameOffset + 8) return null

        // Read 13-byte null-terminated string
        val nameEnd = minOf(frameNameOffset + 13, bytes.size)
        val rawName = bytes.copyOfRange(frameNameOffset, nameEnd)
        val frameName = String(rawName).replace("\u0000.*".toRegex(), "")
        if (frameName.isEmpty()) return null

        // Strip trailing A\d{3} suffix (e.g., "AVCRAND0A000" -> "AVCRAND0")
        val baseName = frameName.replace(Regex("[Aa]\\d{3}$"), "")
        if (baseName.isEmpty()) return null

        return "${baseName.lowercase(Locale.ROOT)}.def"
    }

    private fun loadDefFromBytes(
        bytes: ByteArray,
        defName: String,
        packer: PixmapPacker
    ): List<RegionInfo> {
        val stream = ByteArrayInputStream(bytes)
        stream.mark(bytes.size)
        val def = DefReader(stream).read()
        val (palette, alpha) = applySpecialPalette(def.rawPalette)

        val isTerrain = defName in terrainDefNames
        val regionInfos = mutableListOf<RegionInfo>()
        val seenFrames = mutableSetOf<String>()

        if (isTerrain) {
            val allFilenames = def.groups.flatMap { it.filenames }
            for (group in def.groups) {
                for (frame in group.frames) {
                    if (frame.frameName in seenFrames) continue
                    seenFrames.add(frame.frameName)
                    regionInfos.addAll(
                        packTerrainFrame(frame, defName, palette.clone(), alpha, allFilenames, packer)
                    )
                }
            }
            return regionInfos
        }

        for (group in def.groups) {
            for (frame in group.frames) {
                val frameKey = defName + frame.frameName
                if (frameKey in seenFrames) continue
                seenFrames.add(frameKey)
                regionInfos.addAll(
                    packObjectFrame(frame, defName, palette, alpha, group.filenames, packer)
                )
            }
        }
        return regionInfos
    }

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
