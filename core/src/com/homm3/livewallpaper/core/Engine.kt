package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxGame

open class Engine : KtxGame<Screen>(null, true) {
    open var onSettingsButtonClick: () -> Unit = { }

    lateinit var assets: Assets
    val camera = OrthographicCamera()
    var randomPoint = Vector2()
    val viewport = ScreenViewport(camera)

    override fun create() {
        assets = Assets()
        assets.tryLoadWallpaperAssets()
        addScreen(LoadingScreen(this))
        addScreen(SettingsScreen(this))
        Gdx.app.postRunnable(::updateVisibleScreen)
    }

    fun updateVisibleScreen() {
        if (assets.isWallpaperAssetsLoaded()) {
            if (!screens.containsKey(WallpaperScreen::class.java)) {
                addScreen(WallpaperScreen(this))
            }
            setScreen<WallpaperScreen>()
        } else {
            assets.tryLoadWallpaperAssets()

            if (!assets.manager.isFinished) {
                setScreen<LoadingScreen>()
                return
            }

            setScreen<SettingsScreen>()
        }
    }

    override fun resume() {
        super.resume()
        updateVisibleScreen()
    }
}