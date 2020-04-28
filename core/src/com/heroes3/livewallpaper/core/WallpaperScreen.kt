package com.heroes3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.loaders.TextureAtlasLoader
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.heroes3.livewallpaper.parser.JsonMapParser
import ktx.app.KtxScreen
import ktx.assets.load

class WallpaperScreen(private val engine: Engine) : KtxScreen {
    private lateinit var terrainRenderer: TerrainRenderer
    private lateinit var objectsRenderer: ObjectsRenderer
    private val parser = JsonMapParser()

    init {
        tryLoadAssets()
    }

    fun isAssetsLoaded() : Boolean {
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

    override fun show() {
        super.show()
        engine.camera.setToOrtho(true)
        val h3mMap = parser.parse(Gdx.files.internal("maps/invasion.json").read())
        terrainRenderer = TerrainRenderer(engine, h3mMap)
        objectsRenderer = ObjectsRenderer(engine, h3mMap)
        Gdx.input.inputProcessor = InputProcessor(engine.camera)
    }

    override fun resize(width: Int, height: Int) {
        engine.viewport.update(width, height)
    }

    override fun render(delta: Float) {
        engine.camera.update()

        if (engine.assets.manager.update()) {
            terrainRenderer.render()
            objectsRenderer.render(delta)
        }
    }
}