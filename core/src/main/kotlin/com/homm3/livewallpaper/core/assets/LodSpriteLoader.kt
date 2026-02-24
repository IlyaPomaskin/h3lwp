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

        // Build original-index map for terrain PCX grouping
        val entryOriginalIndex = HashMap<LodEntry, Int>(archive.files.size)
        archive.files.forEachIndexed { idx, entry -> entryOriginalIndex[entry] = idx }

        onProgress("Loading HotA 1.8 sprites...")
        val allRegionInfos = mutableListOf<RegionInfo>()
        var processed = 0
        val matchedNames = mutableSetOf<String>()

        // Collect 32x32 PCX terrain tiles with their original LOD indices
        val pcxTiles = mutableListOf<Pair<Int, ByteArray>>() // (originalIndex, decompressed bytes)

        for (entry in sortedEntries) {
            try {
                val stream = lodReader.readFileContent(entry)
                val bytes = stream.readBytes()
                if (bytes.size < 4) continue

                // Check for 32x32 8-bit PCX: fSize(4) + width(4) + height(4) + pixels + palette(768)
                if (bytes.size >= 12) {
                    val buf32 = java.nio.ByteBuffer.wrap(bytes, 0, 12).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    val fSize = buf32.int
                    val w = buf32.int
                    val h = buf32.int
                    if (w == 32 && h == 32 && fSize + 12 + 768 == bytes.size) {
                        pcxTiles.add((entryOriginalIndex[entry] ?: -1) to bytes)
                        continue
                    }
                }

                val isD32 = bytes[0] == D32_MAGIC_BYTES[0] &&
                        bytes[1] == D32_MAGIC_BYTES[1] &&
                        bytes[2] == D32_MAGIC_BYTES[2] &&
                        bytes[3] == D32_MAGIC_BYTES[3]

                val defName = extractDefNameFromBytes(bytes, isD32, neededLower) ?: continue
                if (defName !in neededLower) continue

                matchedNames.add(defName)

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

        // Load terrain tiles from grouped 32x32 PCX entries
        val terrainRegions = loadHota18TerrainPcxRuns(pcxTiles, neededLower, packer)
        allRegionInfos.addAll(terrainRegions)

        log.info("HotA 1.8: loaded $processed DEF/D32 sprites, matched: ${matchedNames.size} names, " +
                "${pcxTiles.size} terrain PCX tiles")
        onProgress("Loaded $processed HotA 1.8 sprites")
        return allRegionInfos
    }

    /**
     * Group 32x32 PCX terrain tiles by consecutive original LOD entry index
     * and assign runs to highland/wasteland terrain types.
     * HotA 1.8 stores terrain tiles as consecutive groups of individual PCX files
     * (alphabetical: highland before wasteland).
     */
    private fun loadHota18TerrainPcxRuns(
        pcxTiles: List<Pair<Int, ByteArray>>,
        neededNames: Set<String>,
        packer: PixmapPacker
    ): List<RegionInfo> {
        if (pcxTiles.isEmpty()) return emptyList()

        // Group tiles into runs of consecutive original LOD indices
        val sorted = pcxTiles.sortedBy { it.first }
        val runs = mutableListOf<MutableList<ByteArray>>()
        var currentRun = mutableListOf(sorted[0].second)
        var prevIndex = sorted[0].first

        for (i in 1 until sorted.size) {
            val (idx, bytes) = sorted[i]
            if (idx <= prevIndex + 2) { // allow small gap for non-PCX entries between tiles
                currentRun.add(bytes)
            } else {
                if (currentRun.size >= 20) runs.add(currentRun)
                currentRun = mutableListOf(bytes)
            }
            prevIndex = idx
        }
        if (currentRun.size >= 20) runs.add(currentRun)

        log.info("HotA 1.8 terrain PCX: ${pcxTiles.size} tiles in ${runs.size} runs (sizes: ${runs.map { it.size }})")

        // Assign runs to terrain types alphabetically: highland before wasteland
        val terrainNames = listOf("highlnd.def", "wastlnd.def").filter { it in neededNames }
        val allRegionInfos = mutableListOf<RegionInfo>()

        for ((i, terrainName) in terrainNames.withIndex()) {
            if (i >= runs.size) break
            log.info("HotA 1.8: loading ${runs[i].size} PCX tiles as $terrainName")
            allRegionInfos.addAll(loadTerrainPcxTiles(runs[i], terrainName, packer))
        }

        return allRegionInfos
    }

    /**
     * Load a run of 32x32 PCX terrain tiles. Each tile has its own palette.
     */
    private fun loadTerrainPcxTiles(
        tiles: List<ByteArray>,
        terrainDefName: String,
        packer: PixmapPacker
    ): List<RegionInfo> {
        val allRegionInfos = mutableListOf<RegionInfo>()
        val defNameNoExt = terrainDefName.removeSuffix(".def")
        val groupFilenames = tiles.indices.map { "TILE$it" }

        for ((index, tileBytes) in tiles.withIndex()) {
            try {
                // Each tile has: fSize(4) + width(4) + height(4) + pixels(1024) + palette(768)
                val pixelData = tileBytes.copyOfRange(12, 12 + 1024)
                val palette = tileBytes.copyOfRange(12 + 1024, 12 + 1024 + 768)
                val (processedPalette, alpha) = applySpecialPalette(palette)

                val frame = DefFrame("TILE$index", 32, 32, 32, 32, 0, 0, pixelData)
                allRegionInfos.addAll(
                    packTerrainFrame(frame, terrainDefName, processedPalette, alpha, groupFilenames, packer)
                )
            } catch (e: Throwable) {
                log.log(Level.WARNING, "Failed to load terrain PCX tile $index for $terrainDefName", e)
            }
        }
        return allRegionInfos
    }

    /**
     * Extract DEF/D32 name by parsing the file header and reading the first frame name.
     *
     * HotA 1.8 frame names use several obfuscation patterns:
     * - Standard: "AVCRAND0A000" → "avcrand0"
     * - Dot extension: "AVLGLC01.7oh" → "avlglc01"
     * - Prefixed: "0w_pilr0.yn4" → "pilr0" → try "avlpilr0"
     * - AWL typo: "AWLswd4A001.P" → swap AWL→AVL → "avlswd4"
     * - Abbreviated: "ZREF1A000." → try letter doubling → "zreef1"
     */
    private fun extractDefNameFromBytes(
        bytes: ByteArray,
        isD32: Boolean,
        neededNames: Set<String>
    ): String? {
        val frameName = if (isD32) {
            extractD32FrameName(bytes)
        } else {
            extractDefFrameName(bytes)
        } ?: return null

        // Clean frame name:
        // 1. Strip "0w_" prefix (HotA Cove/world prefix)
        var cleaned = frameName
        if (cleaned.startsWith("0w_", ignoreCase = true)) cleaned = cleaned.substring(3)
        // 2. Strip random dot extension (".7oh", ".yn4", ".P", ".")
        val dotIdx = cleaned.indexOf('.')
        if (dotIdx > 0) cleaned = cleaned.substring(0, dotIdx)
        // 3. Strip trailing A\d{3} suffix ("A000", "A001")
        cleaned = cleaned.replace(Regex("[Aa]\\d{3}$"), "")
        if (cleaned.isEmpty()) return null

        val match = findMatchingDefName(cleaned.lowercase(Locale.ROOT), neededNames)
        if (match != null) return match
        return "${cleaned.lowercase(Locale.ROOT)}.def"
    }

    /**
     * Try to match a cleaned frame base name against needed DEF names.
     * Uses progressive digit stripping, AVL prefix prepending, AWL→AVL swap,
     * and letter-doubling fuzzy match.
     */
    private fun findMatchingDefName(base: String, neededNames: Set<String>): String? {
        var candidate = base
        while (candidate.isNotEmpty()) {
            // Direct match
            if ("$candidate.def" in neededNames) return "$candidate.def"

            // Try prepending "avl" (HotA sometimes strips this prefix)
            if (!candidate.startsWith("avl") && !candidate.startsWith("awl")) {
                if ("avl$candidate.def" in neededNames) return "avl$candidate.def"
            }

            // Try swapping AWL→AVL (typo in some HotA frames)
            if (candidate.startsWith("awl")) {
                val swapped = "avl${candidate.substring(3)}"
                if ("$swapped.def" in neededNames) return "$swapped.def"
            }

            // PCX terrain prefix mapping (e.g., "wstlt" -> "wastlnd.def")
            val pcxMapped = terrainPcxPrefixes[candidate]
            if (pcxMapped != null && pcxMapped in neededNames) return pcxMapped

            if (candidate.last().isDigit()) {
                candidate = candidate.dropLast(1)
            } else {
                break
            }
        }

        // Fuzzy: try doubling each letter (handles "zref" → "zreef")
        val baseNoDigits = base.trimEnd { it.isDigit() }
        val digits = base.substring(baseNoDigits.length)
        for (i in baseNoDigits.indices) {
            val doubled = baseNoDigits.substring(0, i + 1) + baseNoDigits[i] +
                    baseNoDigits.substring(i + 1) + digits
            if ("$doubled.def" in neededNames) return "$doubled.def"
        }

        return null
    }

    private fun extractD32FrameName(bytes: ByteArray): String? {
        // D32 header: 32 bytes, then group header: 4+4+4+4 = 16 bytes, then filenames
        if (bytes.size < 48 + 13) return null
        val buf = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buf.position(16) // skip magic(4) + unknown(4) + width(4) + height(4)
        val groupCount = buf.int
        if (groupCount <= 0 || groupCount > 256) return null
        buf.position(32) // start of groups

        for (g in 0 until groupCount) {
            if (buf.remaining() < 16) return null
            buf.int // groupType
            val framesCount = buf.int
            buf.int // unknown
            buf.int // unknown
            if (framesCount in 1..10000 && buf.remaining() >= 13) {
                return readFixedString(bytes, buf.position(), 13)
            }
            if (framesCount < 0) return null
            val skip = framesCount.toLong() * 17 // 13 bytes name + 4 bytes offset
            if (skip < 0 || skip > buf.remaining()) return null
            buf.position(buf.position() + skip.toInt())
        }
        return null
    }

    private fun extractDefFrameName(bytes: ByteArray): String? {
        // DEF header: type(4) + fullWidth(4) + fullHeight(4) + groupCount(4) + palette(768) = 784
        if (bytes.size < 784 + 16) return null
        val buf = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)

        // Validate DEF header dimensions
        buf.position(4)
        val fullW = buf.int
        val fullH = buf.int
        if (fullW <= 0 || fullW > 4096 || fullH <= 0 || fullH > 4096) return null

        val groupCount = buf.int
        if (groupCount <= 0 || groupCount > 256) return null
        buf.position(784) // skip to first group (after header + palette)

        for (g in 0 until groupCount) {
            if (buf.remaining() < 16) return null
            buf.int // groupType
            val framesCount = buf.int
            buf.position(buf.position() + 8) // skip unknown

            if (framesCount in 1..10000 && buf.remaining() >= 13) {
                return readFixedString(bytes, buf.position(), 13)
            }
            if (framesCount < 0) return null
            val skip = framesCount.toLong() * 17 // 13 bytes name + 4 bytes offset
            if (skip < 0 || skip > buf.remaining()) return null
            buf.position(buf.position() + skip.toInt())
        }
        return null
    }

    private fun readFixedString(bytes: ByteArray, offset: Int, length: Int): String? {
        if (offset + length > bytes.size) return null
        val raw = bytes.copyOfRange(offset, offset + length)
        val str = String(raw).replace("\u0000.*".toRegex(), "")
        return if (str.isNotEmpty() && str.all { it.code in 0x20..0x7E }) str else null
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
