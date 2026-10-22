package com.homm3.livewallpaper.core.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.utils.ScreenUtils
import ktx.log.logger

/**
 * Desktop-only prototype aid: with -Dh3lwp.capture=<png path> the screen is captured
 * N frames (-Dh3lwp.captureFrame, default 30) after the first map is shown, then the app exits.
 * -Dh3lwp.camera=<x>,<y> pins the camera so captures from different builds are comparable.
 */
class DebugCapture {
    private val capturePath: String? = System.getProperty(CAPTURE_PROPERTY)
    val pinnedCamera: Pair<Float, Float>? = System.getProperty(CAMERA_PROPERTY)
        ?.split(",")
        ?.mapNotNull { it.trim().toFloatOrNull() }
        ?.takeIf { it.size == 2 }
        ?.let { it[0] to it[1] }
    private val captureAfterFrames = System.getProperty(CAPTURE_FRAME_PROPERTY)?.toIntOrNull() ?: DEFAULT_CAPTURE_AFTER_FRAMES
    private var framesWithMap = 0

    val isEnabled: Boolean get() = capturePath != null

    fun onFrameRendered(hasMap: Boolean) {
        val path = capturePath ?: return
        if (!hasMap) return
        framesWithMap++
        if (framesWithMap != captureAfterFrames) return

        val pixmap = Pixmap.fromFrameBuffer()
        PixmapIO.writePNG(Gdx.files.absolute(path), pixmap)
        pixmap.dispose()
        log.info { "captured ${Gdx.graphics.backBufferWidth}x${Gdx.graphics.backBufferHeight} to $path" }
        Gdx.app.exit()
    }

    private object Pixmap {
        fun fromFrameBuffer() = ScreenUtils.getFrameBufferPixmap(
            0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight
        )
    }

    companion object {
        private const val CAPTURE_PROPERTY = "h3lwp.capture"
        private const val CAMERA_PROPERTY = "h3lwp.camera"
        private const val CAPTURE_FRAME_PROPERTY = "h3lwp.captureFrame"
        private const val DEFAULT_CAPTURE_AFTER_FRAMES = 30
        private val log = logger<DebugCapture>()
    }
}
