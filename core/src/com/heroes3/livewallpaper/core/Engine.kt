package com.heroes3.livewallpaper.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.viewport.ScreenViewport

class Engine : ApplicationAdapter() {
    private lateinit var assets: Assets
    private lateinit var terrainRenderer: TerrainRenderer
    private lateinit var objectsRenderer: ObjectsRenderer
    private val camera = OrthographicCamera()
    private val viewport = ScreenViewport(camera)
    private val parser = JsonMapParser()

    override fun create() {
        assets = Assets()
        val h3mMap = parser.parse(Gdx.files.internal("maps/invasion.json").read())
        camera.setToOrtho(true)
        terrainRenderer = TerrainRenderer(assets, camera, h3mMap)
        objectsRenderer = ObjectsRenderer(assets, camera, h3mMap)
        Gdx.input.inputProcessor = InputProcessor(camera)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    override fun render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        camera.update()
        terrainRenderer.render()
        objectsRenderer.render()
    }
}