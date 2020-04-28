package com.heroes3.livewallpaper.core

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.heroes3.livewallpaper.parser.JsonMapParser
import ktx.app.KtxScreen
import ktx.assets.load
import ktx.async.interval
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.random.Random

class WallpaperScreen(private val engine: Engine) : KtxScreen {
    private var terrainRenderer: TerrainRenderer? = null
    private var objectsRenderer: ObjectsRenderer? = null
    private var inputProcessor = InputProcessor(engine.camera).apply {
        onRandomizeCameraPosition = ::randomizeCameraPosition
    }
    private val cameraRandomizerTask = interval(
        60f * 10f,
        0f,
        -1,
        ::randomizeCameraPosition
    )
    private val h3mMap = JsonMapParser().parse(Gdx.files.internal("maps/invasion.json").read())

    init {
        tryLoadAssets()
    }

    fun isAssetsLoaded(): Boolean {
        return engine.assets.manager.isLoaded(Assets.atlasPath)
    }

    private fun canLoadAssets(): Boolean {
        val isAssetsReady = Gdx.app
            .getPreferences(Engine.PREFERENCES_NAME)
            .getBoolean(Engine.IS_ASSETS_READY_KEY)
        val filesExists = Gdx.files.internal(Assets.atlasPath).exists()

        return isAssetsReady && filesExists && !isAssetsLoaded()
    }

    fun tryLoadAssets() {
        if (canLoadAssets()) {
            engine.assets.manager.load<TextureAtlas>(
                Assets.atlasPath,
                TextureAtlasLoader.TextureAtlasParameter(true)
            )
            engine.updateVisibleScreen()
        }
    }

    private fun randomizeCameraPosition() {
        val cameraViewportWidthTiles = ceil(engine.camera.viewportWidth * engine.camera.zoom / 32)
        val cameraViewportHeightTiles = ceil(engine.camera.viewportHeight * engine.camera.zoom / 32)
        val nextCameraX = Random.nextInt(
           cameraViewportWidthTiles.toInt(),
            floor(h3mMap.size - cameraViewportWidthTiles).toInt()
        ) * 32f
        val nextCameraY = Random.nextInt(
            cameraViewportHeightTiles.toInt(),
            floor(h3mMap.size - cameraViewportHeightTiles).toInt()
        ) * 32f

        engine.camera.position.set(nextCameraX, nextCameraY, 0f)
        objectsRenderer?.updateVisibleSprites()
    }

    override fun show() {
        super.show()
        engine.camera.setToOrtho(true)
        randomizeCameraPosition()
        terrainRenderer = terrainRenderer ?: TerrainRenderer(engine, h3mMap)
        objectsRenderer = objectsRenderer ?: ObjectsRenderer(engine, h3mMap)

        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            Gdx.input.inputProcessor = inputProcessor
        }
    }

    override fun resize(width: Int, height: Int) {
        engine.viewport.update(width, height)
    }

    override fun render(delta: Float) {
        engine.camera.update()

        if (engine.assets.manager.update()) {
            terrainRenderer?.render()
            objectsRenderer?.render(delta)
        }
    }

    override fun dispose() {
        super.dispose()
        terrainRenderer?.dispose()
        objectsRenderer?.dispose()
        cameraRandomizerTask.cancel()
    }
}