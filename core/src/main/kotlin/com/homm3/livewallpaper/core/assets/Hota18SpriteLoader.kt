package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.homm3.livewallpaper.parser.def.D32Reader
import com.homm3.livewallpaper.parser.def.DefFrame
import com.homm3.livewallpaper.parser.def.DefReader
import com.homm3.livewallpaper.parser.def.DefSprite
import com.homm3.livewallpaper.parser.lod.LodEntry
import com.homm3.livewallpaper.parser.lod.LodReader
import java.io.ByteArrayInputStream
import java.util.Locale
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Loads sprites from HotA 1.8 LOD archives where entries are nameless
 * (identified by MD5 hashes). Identifies DEF/D32 names by parsing frame
 * names from file headers and matching against needed sprite names.
 */
class Hota18SpriteLoader(private val base: LodSpriteLoader) {

    fun loadSprites(
        lodReader: LodReader,
        archive: com.homm3.livewallpaper.parser.lod.LodArchive,
        neededNames: Set<String>,
        packer: PixmapPacker,
        onProgress: (String) -> Unit
    ): List<RegionInfo> {
        // Always pull both Factory terrains, even if no map in the current batch
        // references them. Cost is ~250KB of atlas; in exchange we don't depend
        // on which maps are in the batch to load them in the right order.
        val neededLower = (neededNames + setOf("highlnd.def", "wastlnd.def"))
            .map { it.lowercase(Locale.ROOT) }.toSet()
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

        // Pass 1: Partial decompress to identify entries and collect terrain PCX tiles
        val HEADER_SIZE = 2048 // DEF header (784) + group headers + frame names (13 bytes each)
        data class MatchedEntry(val entry: LodEntry, val defNames: List<String>, val isD32: Boolean)
        val matchedEntries = mutableListOf<MatchedEntry>()

        for (entry in sortedEntries) {
            try {
                // Check if this is a 32x32 terrain PCX by size alone (no decompression needed for uncompressed)
                val expectedPcxSize = 12 + 1024 + 768 // header + 32*32 pixels + palette
                if (entry.compressedSize == 0 && entry.size == expectedPcxSize) {
                    val stream = lodReader.readFileContent(entry)
                    val bytes = stream.readBytes()
                    val buf12 = java.nio.ByteBuffer.wrap(bytes, 0, 12).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    val fSize = buf12.int; val w = buf12.int; val h = buf12.int
                    if (w == 32 && h == 32 && fSize == 1024) {
                        pcxTiles.add((entryOriginalIndex[entry] ?: -1) to bytes)
                        continue
                    }
                }

                // Partial decompress: only read enough bytes for header identification
                val header = lodReader.readFileContentPartial(entry, HEADER_SIZE)
                if (header.size < 4) continue

                // Check for 32x32 PCX (compressed)
                if (header.size >= 12) {
                    val buf12 = java.nio.ByteBuffer.wrap(header, 0, 12).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    val fSize = buf12.int; val w = buf12.int; val h = buf12.int
                    if (w == 32 && h == 32 && fSize == 1024) {
                        val stream = lodReader.readFileContent(entry)
                        val bytes = stream.readBytes()
                        if (fSize + 12 + 768 == bytes.size) {
                            pcxTiles.add((entryOriginalIndex[entry] ?: -1) to bytes)
                            continue
                        }
                    }
                }

                val isD32 = header[0] == D32_MAGIC_BYTES[0] &&
                        header[1] == D32_MAGIC_BYTES[1] &&
                        header[2] == D32_MAGIC_BYTES[2] &&
                        header[3] == D32_MAGIC_BYTES[3]

                val defNames = extractDefNamesFromBytes(header, isD32, neededLower)
                val neededDefNames = defNames.filter { it in neededLower }
                if (neededDefNames.isEmpty()) continue

                matchedEntries.add(MatchedEntry(entry, neededDefNames, isD32))
            } catch (e: Throwable) {
                log.log(Level.WARNING, "Failed to identify HotA 1.8 entry ${entry.name}", e)
            }
        }

        // Pass 2: Fully decompress only matched entries
        for ((entry, defNames, isD32) in matchedEntries) {
            try {
                matchedNames.addAll(defNames)
                val stream = lodReader.readFileContent(entry)
                val bytes = stream.readBytes()

                if (isD32) {
                    val d32 = D32Reader(ByteArrayInputStream(bytes)).read()
                    allRegionInfos.addAll(packD32Sprite(d32, defNames.first(), packer))
                } else {
                    allRegionInfos.addAll(loadDefFromBytes(bytes, defNames, neededLower, packer))
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
        val terrainRegions = loadTerrainPcxRuns(pcxTiles, neededLower, packer)
        allRegionInfos.addAll(terrainRegions)

        log.info("HotA 1.8: loaded $processed DEF/D32 sprites, matched: ${matchedNames.size} names, " +
                "${pcxTiles.size} terrain PCX tiles")
        onProgress("Loaded $processed HotA 1.8 sprites")
        return allRegionInfos
    }

    // -- Terrain PCX grouping --------------------------------------------------

    /**
     * Group 32x32 PCX terrain tiles by consecutive original LOD entry index
     * and assign runs to highland/wasteland terrain types.
     * HotA 1.8 stores terrain tiles as consecutive groups of individual PCX files
     * (alphabetical: highland before wasteland).
     */
    private fun loadTerrainPcxRuns(
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

        // Runs come back in LOD index order: highland first, wasteland second.
        // Always load both — neededNames upstream already injects them.
        val allTerrainNames = listOf("highlnd.def", "wastlnd.def")
        val allRegionInfos = mutableListOf<RegionInfo>()

        for ((i, terrainName) in allTerrainNames.withIndex()) {
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
        val groupFilenames = tiles.indices.map { "TILE$it" }

        for ((index, tileBytes) in tiles.withIndex()) {
            try {
                // Each tile has: fSize(4) + width(4) + height(4) + pixels(1024) + palette(768)
                val pixelData = tileBytes.copyOfRange(12, 12 + 1024)
                val palette = tileBytes.copyOfRange(12 + 1024, 12 + 1024 + 768)
                val (processedPalette, alpha) = base.applySpecialPalette(palette)

                val frame = DefFrame("TILE$index", 32, 32, 32, 32, 0, 0, pixelData)
                allRegionInfos.addAll(
                    base.packTerrainFrame(frame, terrainDefName, processedPalette, alpha, groupFilenames, packer)
                )
            } catch (e: Throwable) {
                log.log(Level.WARNING, "Failed to load terrain PCX tile $index for $terrainDefName", e)
            }
        }
        return allRegionInfos
    }

    // -- DEF/D32 loading -------------------------------------------------------

    private fun loadDefFromBytes(
        bytes: ByteArray,
        defNames: List<String>,
        neededNames: Set<String>,
        packer: PixmapPacker
    ): List<RegionInfo> {
        val stream = ByteArrayInputStream(bytes)
        stream.mark(bytes.size)
        val def = DefReader(stream).read()
        val (palette, alpha) = base.applySpecialPalette(def.rawPalette)

        val primaryDefName = defNames.first()
        val isTerrain = primaryDefName in base.terrainDefNames
        val regionInfos = mutableListOf<RegionInfo>()
        val seenFrames = mutableSetOf<String>()

        if (isTerrain) {
            val allFilenames = def.groups.flatMap { it.filenames }
            for (group in def.groups) {
                for (frame in group.frames) {
                    if (frame.frameName in seenFrames) continue
                    seenFrames.add(frame.frameName)
                    regionInfos.addAll(
                        base.packTerrainFrame(frame, primaryDefName, palette.clone(), alpha, allFilenames, packer)
                    )
                }
            }
            return regionInfos
        }

        // When multiple def names match this LOD entry, resolve each frame
        // to its correct def name based on its frame name
        val hasMultipleNames = defNames.size > 1

        for ((gi, group) in def.groups.withIndex()) {
            for (frame in group.frames) {
                // Resolve which def name(s) this frame belongs to. One frame
                // may pack under several names when a sprite is aliased to
                // additional needed defs (e.g. avxboat5 → also avxboat6).
                val resolvedNames: List<String> = if (hasMultipleNames) {
                    val cleaned = cleanFrameName(frame.frameName)
                    if (cleaned.name.isNotEmpty()) {
                        val matches = findMatchingDefNames(cleaned.name.lowercase(Locale.ROOT), neededNames, cleaned.wasPrefixed)
                        if (matches.isNotEmpty()) matches else listOf(primaryDefName)
                    } else {
                        listOf(primaryDefName)
                    }
                } else {
                    listOf(primaryDefName)
                }

                for (resolvedDefName in resolvedNames) {
                    val frameKey = resolvedDefName + frame.frameName
                    if (frameKey in seenFrames) continue
                    seenFrames.add(frameKey)
                    regionInfos.addAll(
                        base.packObjectFrame(frame, resolvedDefName, palette, alpha, group.filenames, packer)
                    )
                }
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

    // -- Frame name matching ---------------------------------------------------

    /**
     * Extract DEF/D32 name by parsing the file header and reading frame names from all groups.
     *
     * HotA 1.8 frame names use several obfuscation patterns:
     * - Standard: "AVCRAND0A000" → "avcrand0"
     * - Dot extension: "AVLGLC01.7oh" → "avlglc01"
     * - Prefixed: "0w_pilr0.yn4" → "pilr0" → try "avlpilr0"
     * - AWL typo: "AWLswd4A001.P" → swap AWL→AVL → "avlswd4"
     * - Abbreviated: "ZREF1A000." → alias → "zreef1"
     * - Multi-group: DEF has group 0="AVMgosn0" and group 1="AVMgosn1"
     */
    private fun extractDefNamesFromBytes(
        bytes: ByteArray,
        isD32: Boolean,
        neededNames: Set<String>
    ): List<String> {
        val frameNames = if (isD32) {
            extractD32FrameNames(bytes)
        } else {
            extractDefFrameNames(bytes)
        }
        if (frameNames.isEmpty()) return emptyList()

        // Try each group's frame name against needed names, collect ALL matches
        val matched = mutableSetOf<String>()
        for (frameName in frameNames) {
            val cleaned = cleanFrameName(frameName)
            if (cleaned.name.isEmpty()) continue
            matched.addAll(findMatchingDefNames(cleaned.name.lowercase(Locale.ROOT), neededNames, cleaned.wasPrefixed))
        }

        if (matched.isNotEmpty()) return matched.toList()

        // Fallback: use first frame name
        val cleaned = cleanFrameName(frameNames[0])
        return if (cleaned.name.isNotEmpty()) listOf("${cleaned.name.lowercase(Locale.ROOT)}.def") else emptyList()
    }

    private data class CleanedFrame(val name: String, val wasPrefixed: Boolean)

    private fun cleanFrameName(frameName: String): CleanedFrame {
        var cleaned = frameName
        val wasPrefixed = cleaned.startsWith("0w_", ignoreCase = true)
        if (wasPrefixed) cleaned = cleaned.substring(3)
        val dotIdx = cleaned.indexOf('.')
        if (dotIdx > 0) cleaned = cleaned.substring(0, dotIdx)
        cleaned = cleaned.replace(Regex("[Aa]\\d{3}$"), "")
        return CleanedFrame(cleaned, wasPrefixed)
    }

    /**
     * Try to match a cleaned frame base name against needed DEF names.
     * Returns all matches — direct match plus every alias whose target is
     * needed — so one sprite can satisfy multiple def-name requests
     * (e.g. avxboat5 is reused to fill the avxboat6 slot in 1.8).
     */
    private fun findMatchingDefNames(base: String, neededNames: Set<String>, tryPrefixes: Boolean = false): List<String> {
        val results = mutableListOf<String>()

        // Direct match
        val direct = "$base.def"
        if (direct in neededNames) results.add(direct)

        // Aliases (a single base name may alias to multiple needed defs)
        defAliases[base]?.forEach { if (it in neededNames && it !in results) results.add(it) }

        if (results.isNotEmpty()) return results

        // Fallbacks — first match wins
        if (tryPrefixes) {
            for (prefix in h3Prefixes) {
                if ("$prefix$base.def" in neededNames) return listOf("$prefix$base.def")
            }
        }
        if (base.startsWith("awl")) {
            val swapped = "avl${base.substring(3)}.def"
            if (swapped in neededNames) return listOf(swapped)
        }
        val pcxMapped = terrainPcxPrefixes[base]
        if (pcxMapped != null && pcxMapped in neededNames) return listOf(pcxMapped)

        return emptyList()
    }

    // -- Binary header parsing -------------------------------------------------

    /**
     * Extract first frame name from EACH group of a D32 file.
     */
    private fun extractD32FrameNames(bytes: ByteArray): List<String> {
        if (bytes.size < 48 + 13) return emptyList()
        val buf = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buf.position(16)
        val groupCount = buf.int
        if (groupCount <= 0 || groupCount > 256) return emptyList()
        buf.position(32)

        val names = mutableListOf<String>()
        for (g in 0 until groupCount) {
            if (buf.remaining() < 16) break
            buf.int // groupType
            val framesCount = buf.int
            buf.int // unknown
            buf.int // unknown
            if (framesCount in 1..10000 && buf.remaining() >= 13) {
                readFixedString(bytes, buf.position(), 13)?.let { names.add(it) }
            }
            if (framesCount < 0) break
            val skip = framesCount.toLong() * 17
            if (skip < 0 || skip > buf.remaining()) break
            buf.position(buf.position() + skip.toInt())
        }
        return names
    }

    /**
     * Extract unique frame names from all groups of a DEF file.
     * Reads all frame names within each group (not just the first) to handle
     * multi-def entries where different frame names map to different defs.
     */
    private fun extractDefFrameNames(bytes: ByteArray): List<String> {
        if (bytes.size < 784 + 16) return emptyList()
        val buf = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)

        buf.position(4)
        val fullW = buf.int
        val fullH = buf.int
        if (fullW <= 0 || fullW > 4096 || fullH <= 0 || fullH > 4096) return emptyList()

        val groupCount = buf.int
        if (groupCount <= 0 || groupCount > 256) return emptyList()
        buf.position(784)

        val seen = mutableSetOf<String>()
        val names = mutableListOf<String>()
        for (g in 0 until groupCount) {
            if (buf.remaining() < 16) break
            buf.int // groupType
            val framesCount = buf.int
            buf.position(buf.position() + 8)

            if (framesCount < 0) break
            // Read all frame names in this group (each is 13 bytes)
            val namesSize = framesCount * 13
            if (framesCount in 1..10000 && buf.remaining() >= namesSize) {
                val namesStart = buf.position()
                for (f in 0 until framesCount) {
                    val name = readFixedString(bytes, namesStart + f * 13, 13)
                    if (name != null && name !in seen) {
                        seen.add(name)
                        names.add(name)
                    }
                }
            }
            // Skip past frame names (13 bytes each) + frame offsets (4 bytes each)
            val skip = framesCount.toLong() * 17
            if (skip < 0 || skip > buf.remaining()) break
            buf.position(buf.position() + skip.toInt())
        }
        return names
    }

    private fun readFixedString(bytes: ByteArray, offset: Int, length: Int): String? {
        if (offset + length > bytes.size) return null
        val raw = bytes.copyOfRange(offset, offset + length)
        val str = String(raw).replace("\u0000.*".toRegex(), "")
        return if (str.isNotEmpty() && str.all { it.code in 0x20..0x7E }) str else null
    }

    // -- Constants & data ------------------------------------------------------

    private val D32_MAGIC_BYTES = byteArrayOf(0x44, 0x33, 0x32, 0x46) // "D32F" little-endian: 0x46323344

    // Common H3/HotA adventure sprite prefixes
    private val h3Prefixes = listOf("avl", "avg", "avw", "avm", "avx", "ava", "avc", "avs")

    // PCX terrain prefix → terrain DEF name
    private val terrainPcxPrefixes = mapOf(
        "wstlt" to "wastlnd.def",
        "hglnt" to "highlnd.def"
    )

    // Manual mapping: frame name base (lowercase) → needed def name
    // For HotA 1.8 sprites where frame names don't match the expected def names
    private val defAliases = listOf(
        "4lvlshrn" to "4lvlxshrn.def",
        "pnd05" to "ava0128w.def",
        "avgarma" to "avgarha.def",
        "avgcoatl" to "avwcoat.def",
        "avgjotn0" to "avgjotu.def",
        "avgkbl01" to "avgkobo.def",
        "avgmmth0" to "avgmamm.def",
        "avgshmn0" to "avgsham.def",
        "garwh" to "avcgarw1.def",
        "garwv" to "avcvgrw.def",
        "avlldrt1" to "avllogdrt1.def",
        "avlwloi" to "avlwloi1.def",
        "avmgosn0" to "avmgosn1.def",
        "acad" to "avswacad.def",
        "avwctl00" to "avwccoat.def",
        "avxamsn0" to "avxamsn1.def",
        "avxboat5" to "avxboat6.def",
        "1sh0w" to "avxl1sh0w.def",
        "2sh0w" to "avxl2sh0w.def",
        "3sh0w" to "avxl3sh0w.def",
        "4sh0w" to "avxl4sh0.def",
        "amn01" to "avxmaltw.def",
        "mnr01" to "avxptw_0.def",
        "mnb_01" to "avxptw_1.def",
        "mng_01" to "avxptw_2.def",
        "mny_01" to "avxptw_3.def",
        "wbor7" to "avxwbor6.def",
        "gr_bk" to "avxwgtbk.def",
        "gr_bl" to "avxwgtbl.def",
        "gr_br" to "avxwgtbr.def",
        "gr_gr" to "avxwgtgr.def",
        "gr_lb" to "avxwgtlb.def",
        "quest" to "avxwgtqu.def",
        "gr_rd" to "avxwgtrd.def",
        "gr_vl" to "avxwgtvl.def",
        "gr_wh" to "avxwgtwh.def",
        "obs0" to "avxwobs.def",
        "avxmink0" to "avxmn2pink0.def",
        "avxseek" to "avxseek0.def",
        "avgyeti0" to "avgyeti.def",
        "avlklp500" to "avlklp50.def",
        "avlklp600" to "avlklp60.def",
        "avlklp700" to "avlklp70.def",
        "avlklp800" to "avlklp80.def",
        "h4wt1f00" to "h4wt1f.def",
        "h4wt2f00" to "h4wt2f.def",
        "h4wt3f00" to "h4wt3f.def",
        "mntswpgl01" to "mntswpgl.def",
        "roosr_01" to "rooster_01.def",
        "roosr_02" to "rooster_02.def",
        "bhld" to "sanct_wt.def",
        "wball" to "waterball.def",
        "wt_trn01" to "wt_tvrn01.def",
        "zref1" to "zreef1.def",
        "zref2" to "zreef2.def",
        "zref3" to "zreef3.def",
        "zref4" to "zreef4.def",
        "zref5" to "zreef5.def",
    ).groupBy({ it.first }, { it.second })

    companion object {
        private val log = Logger.getLogger(Hota18SpriteLoader::class.java.name)
    }
}
