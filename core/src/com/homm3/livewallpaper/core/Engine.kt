package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxGame
import kotlin.math.min

class Engine : KtxGame<Screen>(null, true) {
    companion object {
        val PREFERENCES_NAME = Engine::class.java.`package`.name + ".PREFERENCES"
        const val IS_ASSETS_READY_KEY = "isAssetsReady"
    }

    lateinit var assets: Assets
    lateinit var skin: Skin
    val camera = OrthographicCamera()
    val viewport = ScreenViewport(camera)
    var onSettingsButtonClick: (onDone: () -> Unit) -> Unit = { }

    override fun create() {
        assets = Assets()
        skin = assets.loadSkin()
        addScreen(LoadingScreen(this))
        addScreen(WallpaperScreen(this))
        addScreen(SettingsScreen(this))
        Gdx.app.postRunnable(::updateVisibleScreen)
    }

    fun updateVisibleScreen() {
        if (!assets.manager.isFinished) {
            setScreen<LoadingScreen>()
            return
        }

        if (getScreen<WallpaperScreen>().isAssetsLoaded()) {
            setScreen<WallpaperScreen>()
        } else {
            setScreen<SettingsScreen>()
        }
    }

    override fun resume() {
        super.resume()
        updateVisibleScreen()
    }
}