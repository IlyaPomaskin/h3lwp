package com.homm3.livewallpaper.core

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.Timer
import com.homm3.livewallpaper.core.Constants.Companion.RANDOMIZE_CAMERA_INTERVAL
import com.homm3.livewallpaper.core.Constants.Companion.TILE_SIZE
import com.homm3.livewallpaper.parser.formats.JsonMap
import ktx.app.KtxScreen
import kotlin.math.ceil
import kotlin.math.floor
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
    private val randomizeCameraTask = Timer.schedule(
        object : Timer.Task() {
            override fun run() {
                randomizeCameraPosition(engine.camera)
            }
        },
        0f,
        RANDOMIZE_CAMERA_INTERVAL
    )

    init {
        engine.camera.zoom = min(1 / Gdx.graphics.density, 1f)
        engine.camera.setToOrtho(true)

        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            Gdx.input.inputProcessor = inputProcessor
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
        randomizeCameraTask.cancel()
        terrainRenderer.dispose()
        objectsRenderer.dispose()
    }
}