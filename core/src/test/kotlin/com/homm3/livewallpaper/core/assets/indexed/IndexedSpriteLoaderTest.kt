package com.homm3.livewallpaper.core.assets.indexed

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.homm3.livewallpaper.core.assets.DefAliases
import com.homm3.livewallpaper.core.assets.PackedRect
import com.homm3.livewallpaper.core.assets.SpriteArchive
import com.homm3.livewallpaper.core.assets.SpriteCollector
import com.homm3.livewallpaper.parser.def.DefReader
import com.homm3.livewallpaper.parser.def.DefSprite
import com.homm3.livewallpaper.parser.h3m.H3mReader
import com.homm3.livewallpaper.parser.lod.FileLodDataSource
import com.homm3.livewallpaper.parser.lod.LodReader
import org.junit.AfterClass
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexedSpriteLoaderTest {
    companion object {
        private const val ASSETS_DIR = "/Users/ilyapomaskin/temp/h3lwp-claude/assets-desktop"
        private val hotaLod = File("$ASSETS_DIR/HotA.lod")
        private val h3SpriteLod = File("$ASSETS_DIR/H3sprite.lod")
        private val aetenMap = File("$ASSETS_DIR/user-maps/Aeten Noemere 1.87.h3m")

        private lateinit var app: HeadlessApplication

        @JvmStatic
        @BeforeClass
        fun setUp() {
            app = HeadlessApplication(object : ApplicationAdapter() {}, HeadlessApplicationConfiguration())
        }

        @JvmStatic
        @AfterClass
        fun tearDown() {
            app.exit()
        }
    }

    @Test
    fun `packing keeps every index byte unchanged`() {
        val packer = PixmapPacker(256, 256, Pixmap.Format.Alpha, 4, true)
        val samples = listOf(
            IndexSample("all-bytes", 16, 16, ByteArray(256) { it.toByte() }),
            IndexSample("odd", 7, 3, ByteArray(7 * 3) { (255 - it * 11).toByte() }),
        )
        samples.forEach { sample ->
            val pixmap = indexPixmap(sample.width, sample.height, sample.data)
            packer.pack(sample.name, pixmap)
            pixmap.dispose()
        }

        samples.forEach { sample ->
            val page = packer.pages.first { sample.name in it.rects.keys() }
            val rect = page.rects[sample.name]
            val packedRect = PackedRect(0, rect.x.toInt(), rect.y.toInt(), rect.width.toInt(), rect.height.toInt())
            assertContentEquals(sample.data, readRect(page.pixmap, packedRect), "bytes changed for ${sample.name}")
        }
        packer.dispose()
    }

    @Test
    fun `loads real sprites as indices`() {
        assumeTrue(hotaLod.exists() && h3SpriteLod.exists())
        val names = setOf("watrtl.def", "avwpike.def", "highlnd.def", "tpbridg4.def", "avccast0.def")

        val bundle = IndexedSpriteLoader().load(sources(), names)

        names.forEach { name ->
            val loadedDefs = bundle.regions.map { it.packerName.substringBefore("/") }.toSet()
            assertTrue(name in loadedDefs, "no regions for $name")
        }
        val watrtl = bundle.regions.filter { it.packerName.startsWith("watrtl.def/") }
        assertTrue(watrtl.all { it.isTerrain && it.regionIndex == 0 })
        assertTrue(watrtl.any { it.regionName == "watrtl/0" })
        assertTrue(bundle.palettes[watrtl.first().paletteIndex].rotations.isNotEmpty(), "watrtl must rotate")
        assertTrue(bundle.regions.any { it.regionName == "highlnd/0" && it.isTerrain })

        val pike = bundle.regions.first { it.regionName == "avwpike" && it.width > 0 }
        val frameName = pike.packerName.substringAfter("/")
        val frame = readFirstDef(listOf(hotaLod, h3SpriteLod), "avwpike.def")
            .groups.flatMap { it.frames }.first { it.frameName == frameName }
        val rect = bundle.packerRects.getValue(pike.packerName)
        assertEquals(frame.width to frame.height, rect.w to rect.h)
        assertContentEquals(frame.data, readRect(bundle.pages[rect.pageIndex], rect))

        bundle.dispose()
    }

    @Test
    fun `times loading the Aeten map`() {
        assumeTrue(hotaLod.exists() && h3SpriteLod.exists() && aetenMap.exists())
        val map = aetenMap.inputStream().use { H3mReader(it).read() }
        val needed = SpriteCollector(true).collectNeededSprites(listOf(map))

        val startMs = System.currentTimeMillis()
        val bundle = IndexedSpriteLoader().load(sources(), needed)
        val elapsedMs = System.currentTimeMillis() - startMs

        val loadedDefs = bundle.regions.map { it.packerName.substringBefore("/") }.toSet()
        println(
            "Aeten: ${needed.size} needed, ${loadedDefs.size} loaded, ${bundle.regions.size} regions, " +
                "${bundle.palettes.size} palettes, ${bundle.pages.size} pages, ${elapsedMs}ms"
        )
        assertTrue(bundle.pages.isNotEmpty())
        bundle.dispose()
    }

    private fun sources() = listOf(
        LodSource("HotA.lod", { FileLodDataSource(hotaLod) }, DefAliases.targetsIn(SpriteArchive.HOTA)),
        LodSource("H3sprite.lod", { FileLodDataSource(h3SpriteLod) }, DefAliases.targetsIn(SpriteArchive.H3SPRITE)),
    )

    private fun readFirstDef(lods: List<File>, name: String): DefSprite = lods.firstNotNullOf { lod ->
        val reader = lod.inputStream().use { LodReader(it) }
        reader.read().find(name)?.let { entry -> DefReader(reader.readFileContent(entry)).read() }
    }

    private class IndexSample(val name: String, val width: Int, val height: Int, val data: ByteArray)

    private fun readRect(page: Pixmap, rect: PackedRect): ByteArray {
        val pixels = page.pixels
        val out = ByteArray(rect.w * rect.h)
        for (row in 0 until rect.h) {
            for (col in 0 until rect.w) {
                out[row * rect.w + col] = pixels.get((rect.y + row) * page.width + rect.x + col)
            }
        }
        return out
    }
}
