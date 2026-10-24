package com.homm3.livewallpaper.core.render

import com.badlogic.gdx.Gdx
import com.homm3.livewallpaper.core.GameConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Advances [AnimationClock] and requests a frame every [interval] while started, so animations advance with
 * non-continuous rendering.
 * Ticks run on [Dispatchers.Default]: KtxAsync/libGDX timers are driven by frames and would never fire.
 * [start] and [stop] are meant for the rendering thread.
 */
class AnimationTicker(private val interval: Duration = GameConfig.FRAME_TIME.toDouble().seconds) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    init {
        require(interval.isPositive()) { "Tick interval must be positive, was $interval" }
    }

    fun start() {
        if (tickJob?.isActive == true) return
        tickJob = scope.launch {
            while (isActive) {
                AnimationClock.advance()
                Gdx.graphics.requestRendering()
                delay(interval)
            }
        }
    }

    fun stop() {
        tickJob?.cancel()
        tickJob = null
    }

    fun dispose() = scope.cancel()
}
