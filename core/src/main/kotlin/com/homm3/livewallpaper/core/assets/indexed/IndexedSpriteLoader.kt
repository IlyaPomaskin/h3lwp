package com.homm3.livewallpaper.core.assets.indexed

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.homm3.livewallpaper.core.assets.LodSpriteLoader
import com.homm3.livewallpaper.core.assets.PackedRect
import com.homm3.livewallpaper.parser.def.DefFrame
import com.homm3.livewallpaper.parser.def.DefReader
import com.homm3.livewallpaper.parser.lod.LodArchive
import com.homm3.livewallpaper.parser.lod.LodEntry
import com.homm3.livewallpaper.parser.lod.LodReader
import com.homm3.livewallpaper.parser.pcx.PcxReader
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.util.Locale
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Loads sprites from LOD archives as 8-bit palette indices packed into Pixmap.Format.Alpha pages.
 * CPU only; every call works on its own state, so it is safe to run on a background thread.
 *
 * Factory terrains (wastlnd/highlnd) stored as "<prefix>000.pcx".. tiles are loaded only when their
 * terrain DEF is needed and the same source has no DEF with that name.
 */
class IndexedSpriteLoader {
    private val paletteSource = LodSpriteLoader()

    fun load(sources: List<LodSource>, neededNames: Set<String>): IndexedBundle {
        val needed = neededNames.map { it.lowercase(Locale.ROOT) }.toSet()
        val builder = BundleBuilder(paletteSource)

        val resolved = sources.fold(emptySet<String>()) { resolvedSoFar, source ->
            val unresolved = needed - resolvedSoFar
            if (unresolved.isEmpty()) return@fold resolvedSoFar
            resolvedSoFar + loadSource(source, unresolved, builder)
        }

        val missing = needed - resolved
        if (missing.isNotEmpty()) log.warning("Indexed sprites: ${missing.size} DEFs not found: $missing")
        return builder.build()
    }

    private fun loadSource(source: LodSource, names: Set<String>, builder: BundleBuilder): Set<String> =
        LodReader(source.open()).use { lodReader -> loadFromReader(lodReader, source, names, builder) }

    private fun loadFromReader(
        lodReader: LodReader,
        source: LodSource,
        names: Set<String>,
        builder: BundleBuilder,
    ): Set<String> {
        val startMs = System.currentTimeMillis()
        val archive = lodReader.read()

        val jobs = names.mapNotNull { name -> findJob(archive, source, name) }.sortedBy { it.offset }
        val loadedNames = jobs
            .filter { job -> runJob(lodReader, job, builder) }
            .map { it.defName }
            .toSet()

        log.info(
            "Indexed sprites ${source.name}: ${loadedNames.size} of ${names.size} DEFs, " +
                "${builder.regionCount} regions total, ${System.currentTimeMillis() - startMs}ms"
        )
        return loadedNames
    }

    private fun findJob(archive: LodArchive, source: LodSource, name: String): LoadJob? {
        val defEntry = archive.find(name) ?: source.aliases[name]?.let { archive.find(it) }
        if (defEntry != null) return LoadJob.Def(name, defEntry)

        val tilePrefix = factoryTilePrefixes[name] ?: return null
        val tiles = generateSequence(0) { it + 1 }
            .map { index -> "$tilePrefix${TILE_INDEX_FORMAT.format(index)}.pcx" }
            .map { tileName -> tileName to archive.find(tileName) }
            .takeWhile { (_, entry) -> entry != null }
            .map { (tileName, entry) -> tileName to entry!! }
            .toList()
        if (tiles.isEmpty()) return null
        return LoadJob.Tiles(name, tiles)
    }

    private fun runJob(lodReader: LodReader, job: LoadJob, builder: BundleBuilder): Boolean = try {
        val regionsBefore = builder.regionCount
        when (job) {
            is LoadJob.Def -> loadDef(lodReader, job, builder)
            is LoadJob.Tiles -> loadTiles(lodReader, job, builder)
        }
        builder.regionCount > regionsBefore
    } catch (e: Throwable) {
        log.log(Level.WARNING, "Failed to load indexed ${job.defName}", e)
        false
    }

    private fun loadDef(lodReader: LodReader, job: LoadJob.Def, builder: BundleBuilder) {
        val bytes = lodReader.readFileContent(job.entry).readBytes()
        if (isD32(bytes)) {
            log.info("Indexed sprites: skipping D32 ${job.defName} (not supported)")
            return
        }

        val def = DefReader(ByteArrayInputStream(bytes)).read()
        val paletteIndex = builder.addPalette(def.rawPalette, paletteSource.paletteRotations[job.defName].orEmpty())
        val uniqueFrames = def.groups.flatMap { it.frames }.distinctBy { it.frameName }

        if (job.defName in paletteSource.terrainDefNames) {
            val allFilenames = def.groups.flatMap { it.filenames }
            uniqueFrames.forEach { frame -> builder.packTerrainFrame(job.defName, frame, allFilenames, paletteIndex) }
            return
        }

        uniqueFrames.forEach { frame ->
            val filenames = def.groups.first { group -> frame.frameName in group.filenames }.filenames
            builder.packObjectFrame(job.defName, frame, filenames, paletteIndex)
        }
    }

    private fun loadTiles(lodReader: LodReader, job: LoadJob.Tiles, builder: BundleBuilder) {
        val tileNames = job.tiles.map { (tileName, _) -> tileName }
        job.tiles.forEach { (tileName, entry) ->
            val pcx = PcxReader(lodReader.readFileContent(entry)).read()
            val palette = pcx.palette
            if (palette == null) {
                log.warning("Indexed sprites: ${job.defName} tile $tileName is not 8-bit, skipped")
                return@forEach
            }
            val frame = DefFrame(tileName, pcx.width, pcx.height, pcx.width, pcx.height, 0, 0, pcx.data)
            builder.packTerrainFrame(job.defName, frame, tileNames, builder.addPalette(palette, emptyList()))
        }
    }

    private fun isD32(bytes: ByteArray): Boolean =
        bytes.size >= D32_MAGIC.size && bytes.copyOf(D32_MAGIC.size).contentEquals(D32_MAGIC)

    private val factoryTilePrefixes: Map<String, String> =
        paletteSource.terrainPcxPrefixes.entries.associate { (prefix, defName) -> defName to prefix }

    private sealed class LoadJob(val defName: String) {
        abstract val offset: Int

        class Def(defName: String, val entry: LodEntry) : LoadJob(defName) {
            override val offset get() = entry.offset
        }

        class Tiles(defName: String, val tiles: List<Pair<String, LodEntry>>) : LoadJob(defName) {
            override val offset get() = tiles.first().second.offset
        }
    }

    companion object {
        private const val TILE_INDEX_FORMAT = "%03d"
        private val D32_MAGIC = "D32F".toByteArray(Charsets.US_ASCII)
        private val log = Logger.getLogger(IndexedSpriteLoader::class.java.name)
    }
}

/** Mutable accumulator for one load() call: packer pages, regions and deduplicated palettes. */
private class BundleBuilder(private val paletteSource: LodSpriteLoader) {
    private val packer = PixmapPacker(PAGE_SIZE, PAGE_SIZE, Pixmap.Format.Alpha, PAGE_PADDING, true)
    private val regions = mutableListOf<IndexedRegionInfo>()
    private val palettes = mutableListOf<PaletteData>()
    private val staticPaletteIndices = mutableMapOf<ByteBuffer, Int>()

    val regionCount get() = regions.size

    fun addPalette(rawPalette: ByteArray, rotations: List<Pair<Int, Int>>): Int {
        val rgba = toRgba(rawPalette)
        val isStatic = rotations.isEmpty()
        if (isStatic) {
            staticPaletteIndices[ByteBuffer.wrap(rgba)]?.let { return it }
        }

        val index = palettes.size
        palettes.add(PaletteData(rgba, rotations))
        if (isStatic) staticPaletteIndices[ByteBuffer.wrap(rgba)] = index
        return index
    }

    private fun toRgba(rawPalette: ByteArray): ByteArray {
        val (rgb, specialAlpha) = paletteSource.applySpecialPalette(rawPalette)
        val rgba = ByteArray(PALETTE_BYTES)
        for (i in 0 until PALETTE_SIZE) {
            val rgbOffset = i * RGB_BYTES
            val rgbaOffset = i * PALETTE_ENTRY_BYTES
            rgba[rgbaOffset] = rgb[rgbOffset]
            rgba[rgbaOffset + 1] = rgb[rgbOffset + 1]
            rgba[rgbaOffset + 2] = rgb[rgbOffset + 2]
            rgba[rgbaOffset + ALPHA_OFFSET] = specialAlpha.getOrElse(i) { OPAQUE }
        }
        return rgba
    }

    fun packTerrainFrame(defName: String, frame: DefFrame, filenames: List<String>, paletteIndex: Int) {
        val packerName = "$defName/${frame.frameName}"
        val canvas = makeTerrainCanvas(frame)
        packer.pack(packerName, canvas)
        canvas.dispose()

        val defNameNoExt = defName.removeSuffix(DEF_EXTENSION)
        regions += positionsOf(frame.frameName, filenames).map { position ->
            IndexedRegionInfo(
                packerName = packerName,
                regionName = "$defNameNoExt/$position",
                regionIndex = 0,
                width = frame.fullWidth,
                height = frame.fullHeight,
                fullWidth = frame.fullWidth,
                fullHeight = frame.fullHeight,
                x = 0,
                y = 0,
                isTerrain = true,
                paletteIndex = paletteIndex,
            )
        }
    }

    fun packObjectFrame(defName: String, frame: DefFrame, filenames: List<String>, paletteIndex: Int) {
        val packerName = "$defName/${frame.frameName}"
        val pixmap = makeObjectPixmap(frame)
        packer.pack(packerName, pixmap)
        pixmap.dispose()

        val defNameNoExt = defName.removeSuffix(DEF_EXTENSION)
        regions += positionsOf(frame.frameName, filenames).map { position ->
            IndexedRegionInfo(
                packerName = packerName,
                regionName = defNameNoExt,
                regionIndex = position,
                width = frame.width,
                height = frame.height,
                fullWidth = frame.fullWidth,
                fullHeight = frame.fullHeight,
                x = frame.x,
                y = frame.y,
                isTerrain = false,
                paletteIndex = paletteIndex,
            )
        }
    }

    fun build(): IndexedBundle {
        val packerRects = packer.pages.flatMapIndexed { pageIndex, page ->
            page.rects.map { entry ->
                val rect = entry.value
                entry.key to PackedRect(pageIndex, rect.x.toInt(), rect.y.toInt(), rect.width.toInt(), rect.height.toInt())
            }
        }.toMap()
        // Pages are handed over without packer.dispose(): the bundle owns and disposes the pixmaps.
        val pages = packer.pages.map { it.pixmap }
        return IndexedBundle(pages, regions.toList(), packerRects, palettes.toList())
    }

    private fun positionsOf(frameName: String, filenames: List<String>): List<Int> =
        filenames.indices.filter { filenames[it] == frameName }

    private fun makeObjectPixmap(frame: DefFrame): Pixmap {
        val isEmptyFrame = frame.width == 0 || frame.height == 0
        if (isEmptyFrame) return Pixmap(1, 1, Pixmap.Format.Alpha)
        return indexPixmap(frame.width, frame.height, frame.data)
    }

    private fun makeTerrainCanvas(frame: DefFrame): Pixmap {
        val canvas = Pixmap(frame.fullWidth, frame.fullHeight, Pixmap.Format.Alpha)
        val visibleWidth = minOf(frame.width, frame.fullWidth - frame.x)
        val visibleHeight = minOf(frame.height, frame.fullHeight - frame.y)
        if (visibleWidth <= 0 || visibleHeight <= 0) return canvas

        val pixels = canvas.pixels
        for (row in 0 until visibleHeight) {
            pixels.position((frame.y + row) * frame.fullWidth + frame.x)
            pixels.put(frame.data, row * frame.width, visibleWidth)
        }
        pixels.position(0)
        return canvas
    }

    companion object {
        private const val PAGE_SIZE = 2048
        private const val PAGE_PADDING = 4
        private const val RGB_BYTES = 3
        private const val ALPHA_OFFSET = 3
        private const val OPAQUE = 0xFF.toByte()
        private const val DEF_EXTENSION = ".def"
    }
}

/** Alpha pixmap whose bytes are exactly [indices] (one palette index per pixel). */
internal fun indexPixmap(width: Int, height: Int, indices: ByteArray): Pixmap {
    require(indices.size == width * height) { "index data ${indices.size} bytes, expected ${width * height}" }
    val pixmap = Pixmap(width, height, Pixmap.Format.Alpha)
    pixmap.pixels.put(indices).position(0)
    return pixmap
}
