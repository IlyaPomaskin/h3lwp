package com.homm3.livewallpaper.android.wallpaper

import java.util.concurrent.atomic.AtomicReference

/** The engine of the live wallpaper running in this process, if any; read by debug-only tooling. */
object RunningEngine {
    private val engine = AtomicReference<AndroidEngine?>(null)

    val current: AndroidEngine? get() = engine.get()

    fun register(androidEngine: AndroidEngine) = engine.set(androidEngine)

    fun unregister(androidEngine: AndroidEngine) {
        engine.compareAndSet(androidEngine, null)
    }
}
