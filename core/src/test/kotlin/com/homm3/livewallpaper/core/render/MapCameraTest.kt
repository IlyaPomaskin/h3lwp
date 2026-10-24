package com.homm3.livewallpaper.core.render

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.headless.HeadlessApplication
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals

class MapCameraTest {
    @Test
    fun `randomized position keeps the current scroll offset`() {
        val camera = MapCamera().apply {
            viewportWidth = VIEWPORT_SIZE
            viewportHeight = VIEWPORT_SIZE
        }
        camera.moveByScrollOffset(HALF_SCROLLED)

        camera.randomizePosition(MAP_SIZE)
        val randomizedX = camera.position.x
        camera.moveByScrollOffset(HALF_SCROLLED)

        assertEquals(randomizedX, camera.position.x)
    }

    @Test
    fun `launcher zoom zooms out up to the max as the shade opens`() {
        val camera = MapCamera()

        camera.setLauncherZoom(0f)
        assertEquals(1f, camera.zoom)
        camera.setLauncherZoom(0.5f)
        assertEquals(1f + MapCamera.MAX_LAUNCHER_ZOOM_OUT / 2, camera.zoom)
        camera.setLauncherZoom(1f)
        assertEquals(1f + MapCamera.MAX_LAUNCHER_ZOOM_OUT, camera.zoom)
    }

    @Test
    fun `launcher zoom outside 0 to 1 is clamped`() {
        val camera = MapCamera()

        camera.setLauncherZoom(-1f)
        assertEquals(1f, camera.zoom)
        camera.setLauncherZoom(2f)
        assertEquals(1f + MapCamera.MAX_LAUNCHER_ZOOM_OUT, camera.zoom)
    }

    companion object {
        private const val VIEWPORT_SIZE = 320f
        private const val MAP_SIZE = 72
        private const val HALF_SCROLLED = 0.5f
        private lateinit var app: HeadlessApplication

        @JvmStatic
        @BeforeClass
        fun startGdx() {
            app = HeadlessApplication(object : ApplicationAdapter() {})
        }

        @JvmStatic
        @AfterClass
        fun stopGdx() {
            app.exit()
        }
    }
}
