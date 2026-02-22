package com.homm3.livewallpaper.core

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
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

open class Engine(
    private val prefs: Flow<WallpaperPreferences>,
    private val onSettingsButtonClick: (onProgress: (String) -> Unit, onDone: () -> Unit) -> Unit = { _, _ -> },
    private val onHotaButtonClick: (onProgress: (String) -> Unit, onDone: () -> Unit) -> Unit = { _, _ -> }
) : KtxGame<Screen>(null, false) {
    private var assets: GameAssets? = null
    private lateinit var camera: MapCamera

    fun moveCameraByOffset(offset: Float) {
        camera.moveByScrollOffset(offset)
    }

    override fun create() {
        KtxAsync.initiate()

        camera = MapCamera()
        val gameAssets = GameAssets()
        assets = gameAssets
        gameAssets.loadUiAssets()

        addScreen(LoadingScreen(gameAssets))
        addScreen(AssetSetupScreen(gameAssets, onSettingsButtonClick, onHotaButtonClick, onConversionDone = ::loadAndStart))
        addScreen(GameScreen(camera, prefs))

        loadAndStart()
    }

    override fun resume() {
        super.resume()

        val a = assets ?: return
        if (a.isGameAssetsLoaded()) {
            setScreen<GameScreen>()
        } else {
            loadAndStart()
        }
    }

    fun onVisibilityChanged(visible: Boolean) {
        val a = assets ?: return
        if (visible && a.isGameAssetsLoaded()) {
            Gdx.app.postRunnable { getScreen<GameScreen>().randomizeVisibleMapPart() }
        }
    }

    override fun render() {
        if (Gdx.app.type == Application.ApplicationType.Desktop && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit()
            return
        }
        super.render()
    }

    override fun dispose() {
        super.dispose()
        assets?.dispose()
    }

    private fun loadAndStart() {
        val a = assets ?: return
        if (!a.isGameAssetsAvailable()) {
            setScreen<AssetSetupScreen>()
            return
        }
        setScreen<LoadingScreen>()
        KtxAsync.launch {
            val maps = a.loadGameAssets()
            maps.forEach { getScreen<GameScreen>().addMap(GameMap(a, it)) }
            setScreen<GameScreen>()
        }
    }
}
