package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.badlogic.gdx.graphics.glutils.ETC1
import com.badlogic.gdx.graphics.glutils.ETC1.ETC1Data
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure CPU converter: RGBA4444 packer page → (ETC1 color, ETC1 alpha-replicated).
 * No GL calls; safe on any thread.
 */
class Etc1AtlasEncoder {
    fun encodePages(packer: PixmapPacker): List<Etc1PageData> {
        return packer.pages.map { page -> encodePage(page.pixmap) }
    }

    fun encodePage(rgba4444: Pixmap): Etc1PageData {
        require(rgba4444.format == Pixmap.Format.RGBA4444) {
            "expected RGBA4444 packer page, got ${rgba4444.format}"
        }
        val w = rgba4444.width
        val h = rgba4444.height

        val colorPx = Pixmap(w, h, Pixmap.Format.RGB565)
        val alphaPx = Pixmap(w, h, Pixmap.Format.RGB565)
        try {
            splitChannels(rgba4444, colorPx, alphaPx)
            val color = ETC1.encodeImage(colorPx)
            val alpha = ETC1.encodeImage(alphaPx)
            return Etc1PageData(color, alpha)
        } finally {
            colorPx.dispose()
            alphaPx.dispose()
        }
    }

    private fun splitChannels(src: Pixmap, colorOut: Pixmap, alphaOut: Pixmap) {
        // libGDX stores RGBA4444/RGB565 pixels low-byte-first in the backing buffer.
        // Using LITTLE_ENDIAN for both reads and writes matches that layout.
        val srcBuf = src.pixels.order(ByteOrder.LITTLE_ENDIAN)
        val colorBuf = colorOut.pixels.order(ByteOrder.LITTLE_ENDIAN)
        val alphaBuf = alphaOut.pixels.order(ByteOrder.LITTLE_ENDIAN)
        srcBuf.position(0); colorBuf.position(0); alphaBuf.position(0)

        val pixels = src.width * src.height
        for (i in 0 until pixels) {
            val s = srcBuf.short.toInt() and 0xFFFF
            val r4 = (s ushr 12) and 0xF
            val g4 = (s ushr 8) and 0xF
            val b4 = (s ushr 4) and 0xF
            val a4 = s and 0xF
            val r8 = (r4 shl 4) or r4
            val g8 = (g4 shl 4) or g4
            val b8 = (b4 shl 4) or b4
            val a8 = (a4 shl 4) or a4

            putRgb565(colorBuf, r8, g8, b8)
            putRgb565(alphaBuf, a8, a8, a8)
        }
    }

    private fun putRgb565(buf: ByteBuffer, r: Int, g: Int, b: Int) {
        val v = ((r and 0xF8) shl 8) or ((g and 0xFC) shl 3) or ((b and 0xF8) ushr 3)
        // Little-endian: low byte first, matching libGDX's in-memory RGB565 layout.
        buf.put((v and 0xFF).toByte())
        buf.put((v ushr 8).toByte())
    }
}
