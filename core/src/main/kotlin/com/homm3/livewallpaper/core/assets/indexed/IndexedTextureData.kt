package com.homm3.livewallpaper.core.assets.indexed

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.TextureData
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.GdxRuntimeException
import java.nio.ByteBuffer

/**
 * Managed texture data uploaded straight from CPU-side bytes (TextureDataType.Custom).
 * Being managed, libGDX re-runs [consumeCustomData] on every EGL context loss
 * (AndroidGraphics.onSurfaceCreated -> Texture.invalidateAllTextures -> Texture.reload -> load), so the texture
 * survives it; filter and wrap are reapplied by Texture.load. Managed textures reject Texture.draw,
 * partial updates must go through glTexSubImage2D directly.
 */
internal abstract class ManagedCustomTextureData(
    private val width: Int,
    private val height: Int,
    private val format: Pixmap.Format,
) : TextureData {
    override fun getType() = TextureData.TextureDataType.Custom
    override fun isPrepared() = true
    override fun prepare() = Unit
    override fun consumePixmap(): Pixmap = throw GdxRuntimeException("custom texture data has no pixmap")
    override fun disposePixmap() = false
    override fun getWidth() = width
    override fun getHeight() = height
    override fun getFormat() = format
    override fun useMipMaps() = false
    override fun isManaged() = true
}

/** An index page (GL_ALPHA, one palette index per byte) with a CPU copy of its bytes for reloads. */
internal class IndexPageTextureData(width: Int, height: Int, indices: ByteBuffer) :
    ManagedCustomTextureData(width, height, Pixmap.Format.Alpha) {

    private val indices: ByteBuffer = copyOf(indices)

    init {
        val expectedSize = width * height
        require(this.indices.capacity() == expectedSize) {
            "index page ${width}x$height needs $expectedSize bytes, got ${this.indices.capacity()}"
        }
    }

    /** Copy of the index bytes, for tests. */
    internal fun indexBytes(): ByteArray = ByteArray(indices.capacity()).also { indices.duplicate().get(it) }

    override fun consumeCustomData(target: Int) {
        Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, UNPACK_ALIGNMENT)
        Gdx.gl.glTexImage2D(
            target, BASE_LEVEL, GL20.GL_ALPHA, getWidth(), getHeight(), NO_BORDER,
            GL20.GL_ALPHA, GL20.GL_UNSIGNED_BYTE, indices,
        )
    }

    companion object {
        fun of(page: Pixmap): IndexPageTextureData {
            require(page.format == Pixmap.Format.Alpha) { "index page must be Alpha, got ${page.format}" }
            return IndexPageTextureData(page.width, page.height, page.pixels)
        }

        private fun copyOf(source: ByteBuffer): ByteBuffer {
            val whole = source.duplicate().apply { clear() }
            return BufferUtils.newByteBuffer(whole.remaining()).put(whole).apply { flip() }
        }
    }
}

/** The palette texture, rebuilt on reload from [table]. */
internal class PaletteTextureData(private val table: PaletteTable) :
    ManagedCustomTextureData(PALETTE_TEXTURE_WIDTH, PALETTE_TEXTURE_HEIGHT, Pixmap.Format.RGBA8888) {

    override fun consumeCustomData(target: Int) {
        Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, UNPACK_ALIGNMENT)
        Gdx.gl.glTexImage2D(
            target, BASE_LEVEL, GL20.GL_RGBA, getWidth(), getHeight(), NO_BORDER,
            GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, null,
        )

        val rows = paletteRowsBytes(table)
        if (rows.isEmpty()) return

        val usedRowCount = rows.size / PALETTE_ROW_BYTES
        val buffer = BufferUtils.newByteBuffer(rows.size).put(rows).apply { flip() }
        Gdx.gl.glTexSubImage2D(
            target, BASE_LEVEL, 0, 0, getWidth(), usedRowCount,
            GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, buffer,
        )
    }
}

/**
 * Bytes of every palette-texture row that holds at least one used slot, in texture layout. Slots are stored
 * row-major PALETTES_PER_ROW per row, so slot s starts at byte s * PALETTE_BYTES; the last row is zero-padded.
 */
internal fun paletteRowsBytes(table: PaletteTable): ByteArray {
    val usedRowCount = (table.slotCount + PALETTES_PER_ROW - 1) / PALETTES_PER_ROW
    return ByteArray(usedRowCount * PALETTE_ROW_BYTES).also { rows ->
        (0 until table.slotCount).forEach { slot -> table.slotBytes(slot).copyInto(rows, slot * PALETTE_BYTES) }
    }
}

internal const val PALETTE_ROW_BYTES = PALETTE_TEXTURE_WIDTH * PALETTE_ENTRY_BYTES
private const val UNPACK_ALIGNMENT = 1
private const val BASE_LEVEL = 0
private const val NO_BORDER = 0
