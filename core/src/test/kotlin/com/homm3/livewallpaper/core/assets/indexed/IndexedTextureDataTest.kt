package com.homm3.livewallpaper.core.assets.indexed

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.utils.GdxNativesLoader
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IndexedTextureDataTest {
    private class Upload(val name: String, val args: List<Any?>) {
        val bytes: ByteArray?
            get() = (args.last() as ByteBuffer?)?.let { buffer ->
                ByteArray(buffer.remaining()).also { buffer.duplicate().get(it) }
            }
    }

    private val uploads = mutableListOf<Upload>()
    private var previousGl: GL20? = null

    @Before
    fun installRecordingGl() {
        previousGl = Gdx.gl
        Gdx.gl = Proxy.newProxyInstance(GL20::class.java.classLoader, arrayOf(GL20::class.java)) { _, method, args ->
            val isUpload = method.name == "glTexImage2D" || method.name == "glTexSubImage2D"
            if (isUpload) uploads.add(Upload(method.name, args.toList()))
            null
        } as GL20
    }

    @After
    fun restoreGl() {
        Gdx.gl = previousGl
    }

    private fun palette(seed: Int) = ByteArray(PALETTE_BYTES) { (it * 3 + seed).toByte() }

    @Test
    fun `index page is managed alpha and re-uploads identical bytes on every reload`() {
        val width = 4
        val height = 3
        val indices = ByteArray(width * height) { it.toByte() }
        val data = IndexPageTextureData(width, height, ByteBuffer.wrap(indices))

        repeat(2) { data.consumeCustomData(GL20.GL_TEXTURE_2D) }

        assertTrue(data.isManaged)
        assertEquals(Pixmap.Format.Alpha, data.format)
        assertEquals(2, uploads.size)
        uploads.forEach { upload ->
            assertEquals("glTexImage2D", upload.name)
            assertEquals(listOf(GL20.GL_TEXTURE_2D, 0, GL20.GL_ALPHA, width, height, 0, GL20.GL_ALPHA), upload.args.take(7))
            assertContentEquals(indices, upload.bytes)
        }
    }

    @Test
    fun `index page keeps its own copy of the source bytes`() {
        val indices = ByteArray(4) { (it + 1).toByte() }
        val data = IndexPageTextureData(2, 2, ByteBuffer.wrap(indices))
        indices.fill(0)

        assertContentEquals(byteArrayOf(1, 2, 3, 4), data.indexBytes())
    }

    @Test
    fun `index page copies a disposed pixmap's bytes`() {
        GdxNativesLoader.load()
        val indices = ByteArray(6) { (it * 40).toByte() }
        val page = indexPixmap(3, 2, indices)
        val data = IndexPageTextureData.of(page)
        page.dispose()

        assertContentEquals(indices, data.indexBytes())
    }

    @Test
    fun `index page rejects mismatched sizes`() {
        assertFailsWith<IllegalArgumentException> { IndexPageTextureData(4, 4, ByteBuffer.allocate(15)) }
    }

    @Test
    fun `palette rows hold slots in texture layout with zero padding`() {
        val table = PaletteTable()
        val slotCount = PALETTES_PER_ROW + 1
        repeat(slotCount) { table.add(PaletteData(palette(it))) }

        val rows = paletteRowsBytes(table)

        assertEquals(2 * PALETTE_ROW_BYTES, rows.size)
        repeat(slotCount) { slot ->
            val row = slot / PALETTES_PER_ROW
            val column = slot % PALETTES_PER_ROW
            val start = (row * PALETTE_TEXTURE_WIDTH + column * PALETTE_SIZE) * PALETTE_ENTRY_BYTES
            assertContentEquals(palette(slot), rows.copyOfRange(start, start + PALETTE_BYTES), "slot $slot")
        }
        assertTrue(rows.copyOfRange(slotCount * PALETTE_BYTES, rows.size).all { it == 0.toByte() })
    }

    @Test
    fun `palette reload uploads current rotation state`() {
        val table = PaletteTable()
        table.add(PaletteData(palette(0)))
        val rotating = table.add(PaletteData(palette(1), listOf(229 to 241))).slot
        repeat(3) { table.step() }
        val data = PaletteTextureData(table)

        data.consumeCustomData(GL20.GL_TEXTURE_2D)

        val (allocation, rows) = uploads
        assertEquals("glTexImage2D", allocation.name)
        assertEquals(listOf(PALETTE_TEXTURE_WIDTH, PALETTE_TEXTURE_HEIGHT), allocation.args.subList(3, 5))
        assertNull(allocation.args.last())
        assertEquals("glTexSubImage2D", rows.name)
        assertEquals(listOf(0, 0, PALETTE_TEXTURE_WIDTH, 1), rows.args.subList(2, 6))
        val uploadedRotating = rows.bytes!!.copyOfRange(rotating * PALETTE_BYTES, (rotating + 1) * PALETTE_BYTES)
        assertContentEquals(table.slotBytes(rotating), uploadedRotating)
        assertTrue(!palette(1).contentEquals(uploadedRotating))
    }

    @Test
    fun `empty palette table only allocates the texture`() {
        PaletteTextureData(PaletteTable()).consumeCustomData(GL20.GL_TEXTURE_2D)

        assertEquals(listOf("glTexImage2D"), uploads.map { it.name })
    }
}
