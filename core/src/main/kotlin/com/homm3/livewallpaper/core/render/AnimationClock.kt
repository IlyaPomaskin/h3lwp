package com.homm3.livewallpaper.core.render

import java.util.concurrent.atomic.AtomicLong

/**
 * Discrete animation step shared by sprites, tiles and palette cycling. [AnimationTicker] advances it right before
 * requesting a frame, so a ticked frame shows the latest step: the next one, or further ahead when it renders more
 * than a tick late. Frames requested for other reasons (input, prefs) repeat the current step.
 */
object AnimationClock {
    private val advancedSteps = AtomicLong()

    /** Step of the frame being rendered; read on the rendering thread only. */
    var frameStep = 0L
        private set

    /** Called from the ticker thread. */
    fun advance() {
        advancedSteps.incrementAndGet()
    }

    /** Snapshots the step once per frame, so a frame never mixes two steps. */
    fun beginFrame() {
        frameStep = advancedSteps.get()
    }
}
