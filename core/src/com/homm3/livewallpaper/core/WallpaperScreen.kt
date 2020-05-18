package com.homm3.livewallpaper.core

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.Timer
import com.homm3.livewallpaper.core.Constants.Companion.TILE_SIZE
import com.homm3.livewallpaper.parser.formats.JsonMap
import ktx.app.KtxScreen
import kotlin.math.ceil
import kotlin.math.min
import kotlin.random.Random

class WallpaperScreen(private val engine: Engine) : KtxScreen {
    private val h3mMap = JsonMap().parse(Gdx.files.internal("maps/invasion.sml").read())
    private var terrainRenderer = TerrainRenderer(engine, h3mMap)
    private var objectsRenderer = ObjectsRenderer(engine, h3mMap)
    private var inputProcessor = InputProcessor(engine.camera).apply {
        onRandomizeCameraPosition = {
            randomizeCameraPosition(engine.camera)
        }
    }
    private var randomizeCameraTask: Timer.Task? = null
    private var updateInterval = Constants.Preferences.DEFAULT_MAP_UPDATE_INTERVAL.toFloat()

    init {
        applyPreferences()
        engine.camera.setToOrtho(true)

        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            Gdx.input.inputProcessor = inputProcessor
        }
    }

    private fun applyPreferences() {
        val prevUpdateInterval = updateInterval
        updateInterval = Gdx.app
            .getPreferences(Constants.Preferences.PREFERENCES_NAME)
            .getInteger(Constants.Preferences.MAP_UPDATE_INTERVAL, Constants.Preferences.DEFAULT_MAP_UPDATE_INTERVAL) * 60f

        if (updateInterval == 0f || prevUpdateInterval != updateInterval) {
            randomizeCameraTask?.cancel()
        }

        if (updateInterval > 0) {
            randomizeCameraTask = Timer.schedule(
                object : Timer.Task() {
                    override fun run() {
                        randomizeCameraPosition(engine.camera)
                    }
                },
                updateInterval,
                updateInterval
            )
        }

        val scale = Gdx.app
            .getPreferences(Constants.Preferences.PREFERENCES_NAME)
            .getString(Constants.Preferences.SCALE)
        engine.camera.zoom = when (scale) {
            "DPI" -> min(1 / Gdx.graphics.density, 1f)
            else -> 1 / (scale.toFloatOrNull() ?: 1f)
        }
    }

    private fun randomizeCameraPosition(camera: OrthographicCamera) {
        val cameraViewportWidthTiles = ceil(camera.viewportWidth * camera.zoom / TILE_SIZE)
        val halfWidth = ceil(cameraViewportWidthTiles / 2).toInt()
        val nextCameraX = Random.nextInt(halfWidth, h3mMap.size - halfWidth) * TILE_SIZE

        val cameraViewportHeightTiles = ceil(camera.viewportHeight * camera.zoom / TILE_SIZE)
        val halfHeight = ceil(cameraViewportHeightTiles / 2).toInt()
        val nextCameraY = Random.nextInt(halfHeight, h3mMap.size - halfHeight) * TILE_SIZE

        camera.position.set(nextCameraX, nextCameraY, 0f)
    }

    override fun show() {
        applyPreferences()

        if (updateInterval == 0f) {
            randomizeCameraPosition(engine.camera)
        }
    }

    override fun resize(width: Int, height: Int) {
        engine.viewport.update(width, height, false)
    }

    override fun render(delta: Float) {
        engine.camera.update()
        terrainRenderer.render()
        objectsRenderer.render(delta)
    }

    override fun dispose() {
        super.dispose()
        randomizeCameraTask?.cancel()
        terrainRenderer.dispose()
        objectsRenderer.dispose()
    }
}