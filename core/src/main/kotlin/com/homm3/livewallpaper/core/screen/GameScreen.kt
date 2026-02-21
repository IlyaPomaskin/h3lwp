package com.homm3.livewallpaper.core.screen

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.homm3.livewallpaper.core.GameConfig
import com.homm3.livewallpaper.core.MapUpdateInterval
import com.homm3.livewallpaper.core.Scale
import com.homm3.livewallpaper.core.WallpaperPreferences
import com.homm3.livewallpaper.core.input.CameraInputProcessor
import com.homm3.livewallpaper.core.map.GameMap
import com.homm3.livewallpaper.core.map.layers.ObjectsLayer
import com.homm3.livewallpaper.core.render.BrightnessOverlay
import com.homm3.livewallpaper.core.render.MapCamera
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ktx.app.KtxScreen
import ktx.async.KtxAsync
import kotlin.math.max
import kotlin.math.min

class GameScreen(
    private val camera: MapCamera,
    private val prefs: Flow<WallpaperPreferences>
) : KtxScreen {

    private val viewport = ScreenViewport(camera)
    private val tiledMap = TiledMap()
    private val renderer = object : OrthogonalTiledMapRenderer(tiledMap) {
        override fun renderObjects(layer: MapLayer?) {
            if (layer is ObjectsLayer) {
                layer.render(this)
            }
        }
    }
    private val inputProcessor = CameraInputProcessor(viewport).apply {
        onEnter = { randomizeVisibleMapPart(force = true) }
    }
    private val brightnessOverlay = BrightnessOverlay(camera)
    private var mapUpdateInterval = 0f
    private var lastMapUpdateTime = 0L
    private var prefsJob: Job? = null

    fun addMap(map: GameMap) {
        tiledMap.layers.add(map)

        if (tiledMap.layers.size() == 1) {
            Gdx.app.postRunnable(::randomizeVisibleMapPart)
        }
    }

    override fun show() {
        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            Gdx.input.inputProcessor = inputProcessor
        }

        var isFirst = true
        prefsJob = KtxAsync.launch {
            prefs.collect {
                brightnessOverlay.brightness = it.brightness
                setUnitsPerPixelByScale(it.scale, isFirst)
                setMapUpdateInterval(it.mapUpdateInterval)
                isFirst = false
            }
        }

        randomizeVisibleMapPart()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    override fun render(delta: Float) {
        inputProcessor.handlePressedKeys()
        camera.update()
        renderer.setView(camera)
        renderer.render()
        brightnessOverlay.render()
    }

    override fun hide() {
        prefsJob?.cancel()
        Gdx.input.inputProcessor = null
    }

    override fun dispose() {
        prefsJob?.cancel()
        renderer.dispose()
        brightnessOverlay.dispose()
    }

    private fun setUnitsPerPixelByScale(scale: Scale, isFirst: Boolean) {
        val nextUnitsPerPixel = when (scale) {
            Scale.DPI -> min(1 / Gdx.graphics.density, 1f)
            Scale.X1 -> 1f
            Scale.X2 -> 0.5f
            Scale.X3 -> 0.33f
            Scale.X4 -> 0.25f
        }

        if (viewport.unitsPerPixel != nextUnitsPerPixel) {
            viewport.unitsPerPixel = nextUnitsPerPixel

            if (!isFirst) {
                randomizeVisibleMapPart(force = true)
            }
        }
    }

    private fun setMapUpdateInterval(interval: MapUpdateInterval) {
        mapUpdateInterval = when (interval) {
            MapUpdateInterval.EVERY_SWITCH -> 1000f
            MapUpdateInterval.HOURS_2 -> 2f * 60f * 60f * 1000f
            MapUpdateInterval.HOURS_24 -> 24f * 60f * 60f * 1000f
            MapUpdateInterval.MINUTES_10 -> 10f * 60f * 1000f
            MapUpdateInterval.MINUTES_30 -> 30f * 60f * 1000f
        }
    }

    private fun shouldUpdateVisibleMapPart(force: Boolean): Boolean {
        val isInitialRender = lastMapUpdateTime == 0L

        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastMapUpdateTime
        val updateInterval = max(mapUpdateInterval, GameConfig.MIN_MAP_UPDATE_INTERVAL_MS)
        val isTimeToUpdate = timeSinceLastUpdate >= updateInterval

        if (force || isInitialRender || isTimeToUpdate) {
            lastMapUpdateTime = currentTime
            return true
        }

        return false
    }

    fun randomizeVisibleMapPart(force: Boolean = false) {
        if (!shouldUpdateVisibleMapPart(force)) {
            return
        }

        val mapLayer = tiledMap
            .layers
            .onEach { it.isVisible = false }
            .toList()
            .filterIsInstance(GameMap::class.java)
            .randomOrNull()

        if (mapLayer != null) {
            camera.randomizePosition(mapLayer.mapSize)
            mapLayer.isVisible = true
        }
    }
}
