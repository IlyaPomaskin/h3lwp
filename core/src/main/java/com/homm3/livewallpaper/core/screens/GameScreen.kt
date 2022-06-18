package com.homm3.livewallpaper.core.screens

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.homm3.livewallpaper.core.*
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.MINIMAL_MAP_UPDATE_INTERVAL
import com.homm3.livewallpaper.core.layers.H3mLayersGroup
import com.homm3.livewallpaper.core.layers.ObjectsLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ktx.app.KtxScreen
import kotlin.math.max
import kotlin.math.min

class GameScreen(private val camera: Camera, private val prefs: Flow<WallpaperPreferences>) :
    KtxScreen {
    private val viewport = ScreenViewport(camera)
    private val tiledMap = TiledMap()
    private val renderer = object : OrthogonalTiledMapRenderer(tiledMap) {
        override fun renderObjects(layer: MapLayer?) {
            if (layer is ObjectsLayer) {
                layer.render(batch)
            }
        }
    }
    private val inputProcessor = InputProcessor(viewport).apply {
        onEnter = { randomizeVisibleMapPart(true) }
    }
    private val brightnessOverlay = BrightnessOverlay(camera)
    private var mapUpdateInterval = 0f
    private var lastMapUpdateTime = 0L

    init {
        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            Gdx.input.inputProcessor = inputProcessor
        }

        subscribeToPreferences()
    }

    private fun setUnitsPerPixelByScale(scale: Scale) {
        // FIXME switching from x4 to x1 doesn't correctly update visible sprites
        // FIXME add borders for x1 scale

        val nextUnitsPerPixel = when (scale) {
            Scale.DPI -> min(1 / Gdx.graphics.density, 1f)
            Scale.X1 -> 1f
            Scale.X2 -> 0.5f
            Scale.X3 -> 0.33f
            Scale.X4 -> 0.25f
        }

        if (viewport.unitsPerPixel != nextUnitsPerPixel) {
            viewport.unitsPerPixel = nextUnitsPerPixel
            randomizeVisibleMapPart(true)
        }

    }

    private fun setMapUpdateInterval(interval: MapUpdateInterval) {
        mapUpdateInterval = when (interval) {
            MapUpdateInterval.EVERY_SWITCH -> 0f
            MapUpdateInterval.HOURS_2 -> 2f * 60f * 60f * 1000f
            MapUpdateInterval.HOURS_24 -> 24f * 60f * 60f * 1000f
            MapUpdateInterval.MINUTES_10 -> 10f * 60f * 1000f
            MapUpdateInterval.MINUTES_30 -> 30f * 60f * 1000f
        }
    }

    private fun subscribeToPreferences() {
        CoroutineScope(Dispatchers.Default).launch {
            prefs.collect {
                brightnessOverlay.brightness = it.brightness
                setUnitsPerPixelByScale(it.scale)
                setMapUpdateInterval(it.mapUpdateInterval)
            }
        }
    }

    fun addMap(map: H3mLayersGroup) {
        tiledMap.layers.add(map)

        if (tiledMap.layers.size() == 1) {
            Gdx.app.postRunnable(::randomizeVisibleMapPart)
        }
    }

    private fun shouldUpdateVisibleMapPart(force: Boolean): Boolean {
        val isInitialRender = lastMapUpdateTime == 0L

        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastMapUpdateTime
        val updateInterval = max(mapUpdateInterval, MINIMAL_MAP_UPDATE_INTERVAL)
        val isTimeToUpdate = timeSinceLastUpdate >= updateInterval

        if (force || isInitialRender || isTimeToUpdate) {
            lastMapUpdateTime = currentTime
            return true
        }

        return false
    }

    private fun randomizeVisibleMapPart(force: Boolean = false) {
        if (!shouldUpdateVisibleMapPart(force)) {
            return
        }

        val h3mLayer = tiledMap
            .layers
            .onEach { it.isVisible = false }
            .toList()
            .filterIsInstance(H3mLayersGroup::class.java)
            .randomOrNull()

        if (h3mLayer != null) {
            camera.randomizeCameraPosition(h3mLayer.mapSize)
            h3mLayer.updateVisibleSprites(camera)
            h3mLayer.isVisible = true
        }
    }

    override fun show() {
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

    override fun dispose() {
        super.dispose()
        renderer.dispose()
    }
}
