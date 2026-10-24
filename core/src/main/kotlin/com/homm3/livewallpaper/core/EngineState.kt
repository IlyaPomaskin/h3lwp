package com.homm3.livewallpaper.core

/** What the [Engine] shows right now; [screen] is null before any screen is set. */
data class EngineState(
    val screen: String?,
    val mapName: String?,
    val isUnderground: Boolean,
    val zoom: Float,
    val cameraX: Float,
    val cameraY: Float,
)
