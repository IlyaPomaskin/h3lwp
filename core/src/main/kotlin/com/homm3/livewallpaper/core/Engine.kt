package com.homm3.livewallpaper.core

import com.badlogic.gdx.Screen
import com.homm3.livewallpaper.core.assets.GameAssets
import com.homm3.livewallpaper.core.map.GameMap
import com.homm3.livewallpaper.core.render.MapCamera
import com.homm3.livewallpaper.core.screen.AssetSetupScreen
import com.homm3.livewallpaper.core.screen.GameScreen
import com.homm3.livewallpaper.core.screen.LoadingScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ktx.app.KtxGame
import ktx.async.KtxAsync

class Engine(
    private val prefs: Flow<WallpaperPreferences>,
    private val onSettingsButtonClick: (onProgress: (String) -> Unit) -> Unit = {}
) : KtxGame<Screen>(null, false) {
    private lateinit var assets: GameAssets
    private lateinit var camera: MapCamera

    fun moveCameraByOffset(offset: Float) {
        camera.moveByScrollOffset(offset)
    }

    override fun create() {
        KtxAsync.initiate()

        camera = MapCamera()
        assets = GameAssets()
        assets.loadUiAssets()

        addScreen(LoadingScreen(assets))
        addScreen(AssetSetupScreen(assets, onSettingsButtonClick))
        addScreen(GameScreen(camera, prefs))

        loadAndStart()
    }

    override fun resume() {
        super.resume()

        if (assets.isGameAssetsLoaded()) {
            setScreen<GameScreen>()
        } else {
            loadAndStart()
        }
    }

    override fun dispose() {
        super.dispose()
        assets.dispose()
    }

    private fun loadAndStart() {
        if (!assets.isGameAssetsAvailable()) {
            setScreen<AssetSetupScreen>()
            return
        }
        setScreen<LoadingScreen>()
        KtxAsync.launch {
            val maps = assets.loadGameAssets()
            maps.forEach { getScreen<GameScreen>().addMap(GameMap(assets, it)) }
            setScreen<GameScreen>()
        }
    }
}
