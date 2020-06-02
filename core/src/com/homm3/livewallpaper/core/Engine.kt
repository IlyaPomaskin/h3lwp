package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxGame
import kotlin.math.min

open class Engine : KtxGame<Screen>(null, true) {
    lateinit var assets: Assets
    val camera = OrthographicCamera()
    var cameraPoint = Vector2()
    val viewport = ScreenViewport(camera)

    open fun onSettingsButtonClick() {}

    override fun create() {
        camera.zoom = min(1 / Gdx.graphics.density, 1f)
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