package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.glutils.ETC1
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Etc1AtlasEncoderTest {
    companion object {
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
    fun `encodes a solid red opaque page`() {
        val px = Pixmap(64, 64, Pixmap.Format.RGBA4444)
        px.setColor(1f, 0f, 0f, 1f)
        px.fill()

        val encoder = Etc1AtlasEncoder()
        val out = encoder.encodePage(px)

        assertEquals(2048, out.color.compressedData.capacity())
        assertEquals(2048, out.alpha.compressedData.capacity())

        val decoded = ETC1.decodeImage(out.color, Pixmap.Format.RGB565)
        val center = decoded.getPixel(32, 32)
        val r = (center ushr 24) and 0xFF
        val g = (center ushr 16) and 0xFF
        val b = (center ushr 8) and 0xFF
        assertTrue(r > 200, "expected near-red R, got r=$r g=$g b=$b")
        assertTrue(g < 60)
        assertTrue(b < 60)
        decoded.dispose()
        px.dispose()
    }

    @Test
    fun `encodes alpha as gray replicated channels`() {
        val px = Pixmap(64, 64, Pixmap.Format.RGBA4444)
        px.setColor(0f, 1f, 0f, 0.5f)
        px.fill()

        val out = Etc1AtlasEncoder().encodePage(px)
        val decoded = ETC1.decodeImage(out.alpha, Pixmap.Format.RGB565)
        val center = decoded.getPixel(32, 32)
        val r = (center ushr 24) and 0xFF
        val g = (center ushr 16) and 0xFF
        val b = (center ushr 8) and 0xFF
        assertTrue(r in 100..160, "r=$r")
        assertTrue(g in 100..160, "g=$g")
        assertTrue(b in 100..160, "b=$b")
        decoded.dispose()
        px.dispose()
    }
}
