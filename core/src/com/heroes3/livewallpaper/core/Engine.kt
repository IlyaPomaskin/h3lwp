package com.heroes3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxGame

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
        skin = Skin(Gdx.files.internal("skin/uiskin.json"))
//        viewport.unitsPerPixel = 1 / Gdx.graphics.density
        camera.zoom = 1 / Gdx.graphics.density
        viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        addScreen(LoadingScreen(this))
        addScreen(WallpaperScreen(this))
        addScreen(SettingsScreen(this))
        updateVisibleScreen()
    }

    fun updateVisibleScreen() {
        val isAssetsReady = Gdx.app
            .getPreferences(PREFERENCES_NAME)
            .getBoolean(IS_ASSETS_READY_KEY)
        val filesExists = Gdx.files.internal(Assets.atlasPath).exists()
        if (isAssetsReady && filesExists) {
            if (assets.manager.isLoaded(Assets.atlasPath)) {
                setScreen<WallpaperScreen>()
            } else {
                assets.loadAtlas()
                setScreen<LoadingScreen>()
            }
        } else {
            setScreen<SettingsScreen>()
        }
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        viewport.update(width, height, true)
    }

    override fun resume() {
        super.resume()
        println("engine resume!!")
        updateVisibleScreen()
    }
}