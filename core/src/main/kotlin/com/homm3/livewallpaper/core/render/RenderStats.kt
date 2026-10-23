package com.homm3.livewallpaper.core.render

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import ktx.log.logger

/** Logs average draw calls and map render CPU time every [REPORT_INTERVAL_NS]; does nothing below debug log level. */
class RenderStats {
    private var frames = 0
    private var drawCalls = 0L
    private var renderNanos = 0L
    private var windowStart = System.nanoTime()

    fun record(frameDrawCalls: Int, frameRenderNanos: Long) {
        val isDebugLogging = Gdx.app.logLevel >= Application.LOG_DEBUG
        if (!isDebugLogging) return
        frames++
        drawCalls += frameDrawCalls
        renderNanos += frameRenderNanos

        val now = System.nanoTime()
        if (now - windowStart < REPORT_INTERVAL_NS) return

        log.debug {
            "render stats: fps=${Gdx.graphics.framesPerSecond} drawCalls/frame=${drawCalls / frames} " +
                "mapRender=${"%.2f".format(renderNanos / frames / NANOS_PER_MS)}ms over $frames frames"
        }
        frames = 0
        drawCalls = 0
        renderNanos = 0
        windowStart = now
    }

    companion object {
        private const val REPORT_INTERVAL_NS = 5_000_000_000L
        private const val NANOS_PER_MS = 1_000_000.0
        private val log = logger<RenderStats>()
    }
}
