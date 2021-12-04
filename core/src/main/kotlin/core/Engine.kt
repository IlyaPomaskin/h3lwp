package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.homm3.livewallpaper.parser.formats.H3m
import core.Camera
import core.layers.H3mLayersGroup
import core.screens.GameScreen
import core.screens.LoadingScreen
import core.screens.SelectAssetsScreen
import ktx.app.KtxGame

open class Engine : KtxGame<Screen>(null, false) {
    private lateinit var assets: Assets
    private lateinit var camera: Camera

    open fun onSettingsButtonClick() {}

    private val mapsList: MutableList<H3m> = mutableListOf()

    fun moveCameraByOffset(offset: Float) {
        camera.moveCameraByOffset(offset)
    }

    override fun create() {
        camera = Camera()
        assets = Assets()
        assets.loadUiAssets()

        addScreen(LoadingScreen(assets))
        addScreen(SelectAssetsScreen(assets, ::onSettingsButtonClick))
        addScreen(GameScreen(camera))

        loadAndStart()
        loadMaps()
    }

    private fun loadAndStart() {
        setScreen<LoadingScreen>()

        if (assets.isGameAssetsAvailable()) {
            assets.loadGameAssets()
            setScreen<GameScreen>()
        } else {
            setScreen<SelectAssetsScreen>()
        }
    }

    private fun loadMaps() {
        Gdx.files
            .internal("maps")
            .list(".h3m")
            .filter { it.length() > 0L }
            .sortedBy { it.length() }
            .forEach { fileHandle ->
                Gdx.app.log("h3mLayer", "start loading ${fileHandle.file()}")
                assets
                    .manager
                    .load(
                        fileHandle.file().toString(),
                        H3m::class.java,
                        H3mLoaderParams().apply {
                            loadedCallback =
                                AssetLoaderParameters.LoadedCallback { aManager, fileName, _ ->
                                    mapsList.add(aManager.get(fileName))
                                }
                        })
            }

        assets.manager.finishLoading()

        mapsList.forEach {
            getScreen<GameScreen>().addMap(
                H3mLayersGroup(assets, it)
            )
        }
    }

    override fun resume() {
        super.resume()

        if (assets.isGameAssetsLoaded()) {
            setScreen<GameScreen>()
        } else {
            loadAndStart()
        }
    }

    override fun render() {
        assets.manager.update()

        super.render()
    }
}