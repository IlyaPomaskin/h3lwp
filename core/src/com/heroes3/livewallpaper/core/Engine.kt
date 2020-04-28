package com.heroes3.livewallpaper.core

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
    var onSettingButtonClick: (onDone: () -> Unit) -> Unit = { }

    override fun create() {
        assets = Assets()
        skin = assets.loadSkin()
        camera.zoom = min(1 / Gdx.graphics.density, 1f)
        viewport.update(Gdx.graphics.width, Gdx.graphics.height)
        addScreen(LoadingScreen(this))
        addScreen(WallpaperScreen(this))
        addScreen(SettingsScreen(this))
        updateVisibleScreen()
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

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        viewport.update(width, height)
    }

    override fun resume() {
        super.resume()
        updateVisibleScreen()
    }
}