package com.homm3.livewallpaper.core

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.homm3.livewallpaper.core.Constants.Companion.TILE_SIZE
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.DEFAULT_MAP_UPDATE_INTERVAL
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.DEFAULT_SCALE
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.BRIGHTNESS
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.BRIGHTNESS_DEFAULT
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.MAP_UPDATE_INTERVAL
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.MINIMAL_MAP_UPDATE_INTERVAL
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.PREFERENCES_NAME
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.SCALE
import com.homm3.livewallpaper.parser.formats.H3mReader
import ktx.app.KtxScreen
import ktx.graphics.use
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class WallpaperScreen(private val engine: Engine) : KtxScreen {
    val camera = OrthographicCamera().also {
        it.setToOrtho(true)
    }
    private val viewport = ScreenViewport(camera)
    private val tiledMap = TiledMap()
    private val renderer = object : OrthogonalTiledMapRenderer(tiledMap) {
        override fun renderObjects(layer: MapLayer?) {
            if (layer is ObjectsLayer) {
                layer.render(batch)
            }
        }
    }
    private var mapUpdateInterval = DEFAULT_MAP_UPDATE_INTERVAL
    private var lastMapUpdateTime = System.currentTimeMillis()
    private val inputProcessor = InputProcessor(camera).also {
        it.onEnter = { randomizeVisibleMapPart() }
        it.onSpace = { tiledMap.layers.forEach { if (it is H3mLayer) it.updateVisibleObjects(camera) } }
    }
    private val prefs = Gdx.app.getPreferences(PREFERENCES_NAME)
    private val brightnessOverlay = BrightnessOverlay(camera)
    private val maps = readMaps()

    init {
        maps.firstOrNull()?.finishLoading()
        applyPreferences()
        randomizeVisibleMapPart()

        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            Gdx.input.inputProcessor = inputProcessor
        }
    }

    private fun readMaps() {
        val filesListBySize = Gdx.files
            .internal("maps")
            .list(".h3m")
            .filter { it.length() > 0L }
            .sortedBy { it.length() }

        if (filesListBySize.isEmpty()) {
            throw Exception("Maps not found")
        }

        readMap(filesListBySize.first()).forEach { tiledMap.layers.add(it) }
        filesListBySize.drop(1).forEach { fileHandle -> thread { readMap(fileHandle).forEach { tiledMap.layers.add(it) } } }
    }

    private fun readMap(file: FileHandle): List<H3mLayer> {
        val h3mMap = H3mReader(file.read()).read()
        val groundLayer = H3mLayer(engine, h3mMap)

        return listOf(groundLayer)
    }

    private fun applyPreferences() {
        brightnessOverlay.brightness = kotlin
            .runCatching { prefs.getInteger(BRIGHTNESS, BRIGHTNESS_DEFAULT) }
            .getOrDefault(BRIGHTNESS_DEFAULT)

        mapUpdateInterval = kotlin
            .runCatching { prefs.getString(MAP_UPDATE_INTERVAL).toFloat() }
            .getOrDefault(DEFAULT_MAP_UPDATE_INTERVAL)

        val scale = kotlin
            .runCatching { prefs.getString(SCALE).toInt() }
            .getOrDefault(DEFAULT_SCALE)

        viewport.unitsPerPixel = when (scale) {
            0 -> min(1 / Gdx.graphics.density, 1f)
            else -> 1 / scale.toFloat()
        }
        viewport.update(Gdx.graphics.width, Gdx.graphics.height)

        brightness = prefs.getInteger(BRIGHTNESS, BRIGHTNESS_DEFAULT)
    }

    private fun randomizeCameraPosition(mapSize: Float) {
        val halfWidth = camera.viewportWidth / 2
        val xStart = halfWidth
        val xEnd = mapSize - xStart - halfWidth
        val nextCameraX = xStart + Random.nextFloat() * xEnd

        val halfHeight = camera.viewportHeight / 2
        val yStart = halfHeight
        val yEnd = mapSize - yStart - halfHeight
        val nextCameraY = yStart + Random.nextFloat() * yEnd

        engine.cameraPoint.set(nextCameraX, nextCameraY)
        camera.position.set(engine.cameraPoint, 0f)
        camera.update()
    }

    private fun randomizeVisibleMapPart() {
        tiledMap.layers.toList().run {
            forEach { it.isVisible = false }
            filterIsInstance(H3mLayer::class.java)
                .random()
                .run {
                    randomizeCameraPosition(mapSize)
                    isVisible = true
                    updateVisibleObjects(camera)
                }
        }
    }

    private fun shouldUpdateVisibleMapPart(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastMapUpdateTime
        val updateInterval = max(mapUpdateInterval, MINIMAL_MAP_UPDATE_INTERVAL)
        if (timeSinceLastUpdate >= updateInterval) {
            lastMapUpdateTime = currentTime
            return true
        }

        return false
    }

    override fun show() {
        applyPreferences()

        if (shouldUpdateVisibleMapPart()) {
            randomizeVisibleMapPart()
        }
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