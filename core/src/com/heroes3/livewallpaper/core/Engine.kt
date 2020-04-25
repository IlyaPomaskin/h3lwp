package com.heroes3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxGame

class Engine : KtxGame<Screen>(null, true) {
    companion object {
        val PREFERENCES_NAME = Engine::class.java.`package`.name + ".PREFERENCES"
        val IS_ASSETS_READY_KEY = "isAssetsReady"
    }

    lateinit var assets: Assets
    val camera = OrthographicCamera()
    val viewport = ScreenViewport(camera)
    var onSettingButtonClick: (onDone: () -> Unit) -> Unit = { }

    override fun create() {
        assets = Assets()
        addScreen(WallpaperScreen(this))
        addScreen(SettingsScreen(this))
        updateVisibleScreen()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        viewport.unitsPerPixel = 1 / Gdx.graphics.density
        println("units per pixel: ${viewport.unitsPerPixel} width: $width height: $height")
    }

    fun updateVisibleScreen() {
        val isAssetsReady = Gdx.app
            .getPreferences(PREFERENCES_NAME)
            .getBoolean(IS_ASSETS_READY_KEY)
        if (isAssetsReady) {
            setScreen<WallpaperScreen>()
        } else {
            setScreen<SettingsScreen>()
        }
    }

    override fun resume() {
        super.resume()
        println("engine resume!!")
        updateVisibleScreen()
    }
}