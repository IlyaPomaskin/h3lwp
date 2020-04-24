package com.heroes3.livewallpaper.core

import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxGame

class Engine : KtxGame<Screen>(null, true) {
    lateinit var assets: Assets
    val camera = OrthographicCamera()
    val viewport = ScreenViewport(camera)
    var onSettingButtonClick: () -> Unit = { }

    override fun create() {
        assets = Assets()
        addScreen(WallpaperScreen(this))
        addScreen(SettingsScreen(this))
        start()
    }

    fun start() {
        if (assets.isReady()) {
            setScreen<WallpaperScreen>()
        } else {
            setScreen<SettingsScreen>()
        }
    }

    override fun resume() {
        super.resume()
        println("resume")
    }
}