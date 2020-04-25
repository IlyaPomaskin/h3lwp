package com.heroes3.livewallpaper.core

import com.badlogic.gdx.Gdx
import ktx.app.KtxScreen

class WallpaperScreen(private val engine: Engine) : KtxScreen {
    private lateinit var terrainRenderer: TerrainRenderer
    private lateinit var objectsRenderer: ObjectsRenderer
    private val parser = JsonMapParser()

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