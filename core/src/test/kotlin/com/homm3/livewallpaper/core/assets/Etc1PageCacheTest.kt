package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.graphics.Pixmap
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class Etc1PageCacheTest {
    companion object {
        private lateinit var app: HeadlessApplication

        @JvmStatic
        @BeforeClass
        fun setUp() {
            app = HeadlessApplication(object : ApplicationAdapter() {}, HeadlessApplicationConfiguration())
        }

        @JvmStatic
        @AfterClass
        fun tearDown() { app.exit() }
    }

    @Test
    fun `round trips a bundle through disk`() {
        val px = Pixmap(64, 64, Pixmap.Format.RGBA4444)
        px.setColor(0f, 0.5f, 1f, 0.8f); px.fill()
        val page = Etc1AtlasEncoder().encodePage(px)
        px.dispose()

        val bundle = Etc1Bundle(
            pages = listOf(page),
            regionInfos = listOf(RegionInfo("foo/0", "foo", 0, 32, 32, 32, 32, 0, 0, false)),
            packerRects = mapOf("foo/0" to PackedRect(0, 0, 0, 32, 32)),
        )

        val cache = Etc1PageCache(dir = "cache/atlas-test")
        val key = cache.cacheKey(setOf("foo"), "test-fingerprint")
        cache.write(key, bundle)

        val read = cache.read(key)
        assertNotNull(read)
        assertEquals(1, read!!.pages.size)
        assertEquals(page.color.compressedData.capacity(), read.pages[0].color.compressedData.capacity())
        assertEquals(bundle.regionInfos, read.regionInfos)
        assertEquals(bundle.packerRects, read.packerRects)
    }
}
