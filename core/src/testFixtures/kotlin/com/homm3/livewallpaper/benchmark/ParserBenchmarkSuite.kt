package com.homm3.livewallpaper.benchmark

import com.homm3.livewallpaper.core.assets.DefAliases
import com.homm3.livewallpaper.core.assets.SpriteArchive
import com.homm3.livewallpaper.core.assets.SpriteCollector
import com.homm3.livewallpaper.core.assets.indexed.IndexedBundle
import com.homm3.livewallpaper.core.assets.indexed.IndexedSpriteLoader
import com.homm3.livewallpaper.core.assets.indexed.LodSource
import com.homm3.livewallpaper.parser.def.DefReader
import com.homm3.livewallpaper.parser.h3m.H3mHeaderReader
import com.homm3.livewallpaper.parser.h3m.H3mMap
import com.homm3.livewallpaper.parser.h3m.H3mReader
import com.homm3.livewallpaper.parser.lod.FileLodDataSource
import com.homm3.livewallpaper.parser.lod.LodReader
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.CRC32
import java.util.zip.GZIPInputStream

/** Measures process-wide CPU time and bytes allocated by all threads; each platform has its own source. */
interface ResourceMeter {
    fun cpuNanos(): Long
    fun allocatedBytes(): Long
}

class SpriteLods(val hota: File, val base: File)

class ParserBenchmarkInputs(
    val mapFiles: List<File>,
    /** LODs whose named .def entries are parsed directly; preloaded up to [defBytesBudget]. */
    val defLods: List<File>,
    val indexLods: List<File>,
    val spriteLods: SpriteLods,
    val defBytesBudget: Long = Long.MAX_VALUE,
)

class PassCounts(val warmup: Int, val measured: Int)

/**
 * Parser and sprite-loader benchmark shared by the desktop test and the Android instrumented test.
 * Gdx natives must be loaded by the caller. Per timed pass it reports median wall time, median process CPU time
 * and median bytes allocated by all threads; a checksum of the output (computed once, untimed) lets runs before
 * and after a change be compared for identical results.
 */
class ParserBenchmarkSuite(
    private val inputs: ParserBenchmarkInputs,
    private val meter: ResourceMeter,
    private val report: (String) -> Unit,
    private val parserPasses: PassCounts = PassCounts(warmup = 5, measured = 15),
    private val loaderPasses: PassCounts = PassCounts(warmup = 3, measured = 9),
) {
    fun run() {
        val mapFiles = inputs.mapFiles.sortedBy { it.name }
        require(mapFiles.isNotEmpty()) { "no maps to benchmark" }
        val maps = mapFiles.map { it.readBytes() }
        val defs = loadDefs()
        val spriteSources = spriteSources()

        val mapSizes = mapFiles.associateWith { file -> file.inputStream().use(H3mHeaderReader::readMapSize) }
        val largestSize = mapSizes.values.max()
        val largestMap = mapSizes.entries.first { it.value == largestSize }.key
        val largestMapSprites = SpriteCollector(hasHotaAssets = true).collectNeededSprites(listOf(parseMap(largestMap)))
        report(
            "Inputs: ${maps.size} maps (${maps.sumOf { it.size } / KIB} KiB gz), ${defs.size} DEFs " +
                "(${defs.sumOf { it.size } / KIB} KiB), ${inputs.indexLods.size} LODs, " +
                "sprites needed: ${largestMapSprites.size} (largest map ${largestMap.name}, ${largestSize}x$largestSize), " +
                "${Runtime.getRuntime().availableProcessors()} CPUs"
        )

        measure(
            name = "H3M gunzip only (floor)",
            pass = { maps.sumOf { bytes -> GZIPInputStream(ByteArrayInputStream(bytes)).readBytes().size } },
            checksum = { 0 },
        )
        measure(
            name = "H3M full parse (all maps)",
            pass = { maps.sumOf { bytes -> runCatching { H3mReader(ByteArrayInputStream(bytes)).read().objects.size }.getOrDefault(0) } },
            checksum = { maps.fold(CRC32()) { crc, bytes -> crc.add(mapSignature(bytes)) }.value },
        )
        measure(
            name = "H3M header size (all maps)",
            pass = { maps.sumOf { bytes -> H3mHeaderReader.readMapSize(ByteArrayInputStream(bytes)) } },
            checksum = { maps.fold(CRC32()) { crc, bytes -> crc.add(H3mHeaderReader.readMapSize(ByteArrayInputStream(bytes)).toString()) }.value },
        )
        measure(
            name = "DEF parse (all DEFs)",
            pass = { defs.sumOf { bytes -> runCatching { DefReader(ByteArrayInputStream(bytes)).read().groups.size }.getOrDefault(0) } },
            checksum = { defs.fold(CRC32()) { crc, bytes -> crc.addDef(bytes) }.value },
        )
        measure(
            name = "LOD index read (all LODs)",
            pass = { inputs.indexLods.sumOf { file -> readLodIndex(file).size } },
            checksum = {
                inputs.indexLods.fold(CRC32()) { crc, file -> crc.add(readLodIndex(file).joinToString { "${it.name}:${it.offset}" }) }.value
            },
        )
        measure(
            name = "Sprite load (largest map)",
            pass = { loadSprites(spriteSources, largestMapSprites) { it.regions.size } },
            checksum = { loadSprites(spriteSources, largestMapSprites, ::bundleChecksum) },
            passes = loaderPasses,
        )
    }

    /** Reads .def entries in archive order until the next one would exceed the byte budget. */
    private fun loadDefs(): List<ByteArray> {
        val defs = mutableListOf<ByteArray>()
        var remainingBytes = inputs.defBytesBudget
        for (lod in inputs.defLods) {
            LodReader(FileLodDataSource(lod)).use { reader ->
                val defEntries = reader.read().files.filter { it.name.endsWith(DEF_EXTENSION, ignoreCase = true) }
                for (entry in defEntries) {
                    val bytes = reader.readFileContent(entry).readBytes()
                    val isD32 = bytes.size >= D32_MAGIC.size && bytes.copyOf(D32_MAGIC.size).contentEquals(D32_MAGIC)
                    if (isD32) continue
                    if (bytes.size > remainingBytes) return defs
                    remainingBytes -= bytes.size
                    defs += bytes
                }
            }
        }
        return defs
    }

    private fun spriteSources(): List<LodSource> {
        val hotaLod = inputs.spriteLods.hota
        val baseLod = inputs.spriteLods.base
        return listOf(
            LodSource(hotaLod.name, { FileLodDataSource(hotaLod) }, DefAliases.targetsIn(SpriteArchive.HOTA)),
            LodSource(baseLod.name, { FileLodDataSource(baseLod) }, DefAliases.targetsIn(SpriteArchive.H3SPRITE)),
        )
    }

    private fun <T> loadSprites(sources: List<LodSource>, names: Set<String>, summarize: (IndexedBundle) -> T): T {
        val bundle = IndexedSpriteLoader().load(sources, names)
        try {
            return summarize(bundle)
        } finally {
            bundle.dispose()
        }
    }

    private fun bundleChecksum(bundle: IndexedBundle): Long {
        val crc = CRC32()
        crc.add(bundle.regions.joinToString())
        crc.add(bundle.packerRects.entries.sortedBy { it.key }.joinToString())
        bundle.palettes.forEach { palette ->
            crc.update(palette.rgba)
            crc.add(palette.rotations.toString())
        }
        bundle.pages.forEach { page ->
            crc.add("${page.width}x${page.height}")
            val pixels = page.pixels.duplicate()
            pixels.rewind()
            crc.update(pixels)
        }
        return crc.value
    }

    private fun parseMap(file: File): H3mMap = file.inputStream().use { H3mReader(it).read() }

    private fun readLodIndex(file: File) = LodReader(FileLodDataSource(file)).use { it.read().files }

    private fun mapSignature(bytes: ByteArray): String = try {
        val map = H3mReader(ByteArrayInputStream(bytes)).read()
        "${map.version}|${map.header}|${map.tiles}|${map.defs}|" +
            map.objects.joinToString { "${it.x},${it.y},${it.z},${it.def.spriteName},${it.objectType}" }
    } catch (e: Exception) {
        "error:${e.javaClass.name}:${e.message}"
    }

    private fun CRC32.addDef(bytes: ByteArray): CRC32 = try {
        val sprite = DefReader(ByteArrayInputStream(bytes)).read()
        add("${sprite.type}|${sprite.fullWidth}x${sprite.fullHeight}")
        update(sprite.rawPalette)
        sprite.groups.forEach { group ->
            add("${group.groupType}|${group.filenames}")
            group.frames.forEach { frame ->
                add("${frame.frameName}|${frame.width}x${frame.height}|${frame.x},${frame.y}")
                update(frame.data)
            }
        }
        this
    } catch (e: Exception) {
        add("error:${e.javaClass.name}:${e.message}")
    }

    private fun CRC32.add(text: String): CRC32 = apply { update(text.toByteArray()) }

    private fun <T> measure(name: String, pass: () -> T, checksum: () -> Long, passes: PassCounts = parserPasses) {
        repeat(passes.warmup) { pass() }

        val samples = (1..passes.measured).map {
            val allocatedBefore = meter.allocatedBytes()
            val cpuBefore = meter.cpuNanos()
            val startNs = System.nanoTime()
            val result = pass()
            Sample(
                durationNs = System.nanoTime() - startNs,
                cpuNs = meter.cpuNanos() - cpuBefore,
                allocatedBytes = meter.allocatedBytes() - allocatedBefore,
                result = result,
            )
        }

        check(samples.map { it.result }.distinct().size == 1) { "$name: non-deterministic output" }
        val wallMs = samples.map { it.durationNs }.median() / NANOS_PER_MILLI
        val minMs = samples.minOf { it.durationNs } / NANOS_PER_MILLI
        val cpuMs = samples.map { it.cpuNs }.median() / NANOS_PER_MILLI
        val allocatedMiB = samples.map { it.allocatedBytes }.median() / BYTES_PER_MIB
        report(
            "%-28s wall %8.1f ms  min %8.1f ms  cpu %8.1f ms  alloc %8.1f MiB  checksum %08x"
                .format(name, wallMs, minMs, cpuMs, allocatedMiB, checksum())
        )
    }

    private fun List<Long>.median(): Long = sorted()[size / 2]

    private class Sample(val durationNs: Long, val cpuNs: Long, val allocatedBytes: Long, val result: Any?)

    private companion object {
        const val KIB = 1024
        const val NANOS_PER_MILLI = 1_000_000.0
        const val BYTES_PER_MIB = 1024.0 * 1024.0
        const val DEF_EXTENSION = ".def"
        val D32_MAGIC = "D32F".toByteArray(Charsets.US_ASCII)
    }
}
