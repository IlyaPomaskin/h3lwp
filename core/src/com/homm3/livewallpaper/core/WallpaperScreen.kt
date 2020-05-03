package com.homm3.livewallpaper.core

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.homm3.livewallpaper.core.Constants.Companion.TILE_SIZE
import com.homm3.livewallpaper.parser.formats.JsonMap
import ktx.app.KtxScreen
import ktx.assets.load
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.random.Random

class WallpaperScreen(private val engine: Engine) : KtxScreen {
    private var terrainRenderer: TerrainRenderer? = null
    private var objectsRenderer: ObjectsRenderer? = null
    private var inputProcessor = InputProcessor(engine.camera).apply {
        onRandomizeCameraPosition = ::randomizeCameraPosition
    }
    private val h3mMap = JsonMap().parse(Gdx.files.internal("maps/invasion.sml").read())

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
        val filesExists = Gdx.files.local(Assets.atlasPath).exists()

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
        val cameraViewportWidthTiles = ceil(engine.camera.viewportWidth * engine.camera.zoom / TILE_SIZE)
        val cameraViewportHeightTiles = ceil(engine.camera.viewportHeight * engine.camera.zoom / TILE_SIZE)
        val nextCameraX = Random.nextInt(
           cameraViewportWidthTiles.toInt(),
            floor(h3mMap.size - cameraViewportWidthTiles).toInt()
        ) * TILE_SIZE
        val nextCameraY = Random.nextInt(
            cameraViewportHeightTiles.toInt(),
            floor(h3mMap.size - cameraViewportHeightTiles).toInt()
        ) * TILE_SIZE

        engine.camera.position.set(nextCameraX, nextCameraY, 0f)
    }

    override fun show() {
        super.show()
        engine.camera.zoom = min(1 / Gdx.graphics.density, 1f)
        engine.camera.setToOrtho(true)
        terrainRenderer = terrainRenderer ?: TerrainRenderer(engine, h3mMap)
        objectsRenderer = objectsRenderer ?: ObjectsRenderer(engine, h3mMap)
        randomizeCameraPosition()

        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            Gdx.input.inputProcessor = inputProcessor
        }
    }

    override fun resize(width: Int, height: Int) {
        engine.viewport.update(width, height, false)
    }

    override fun render(delta: Float) {
        engine.camera.update()

        if (isAssetsLoaded()) {
            terrainRenderer?.render()
            objectsRenderer?.render(delta)
        }
    }

    override fun dispose() {
        super.dispose()
        terrainRenderer?.dispose()
        objectsRenderer?.dispose()
    }
}