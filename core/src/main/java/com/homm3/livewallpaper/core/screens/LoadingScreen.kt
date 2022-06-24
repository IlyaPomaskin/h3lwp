package com.homm3.livewallpaper.core.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.homm3.livewallpaper.core.Assets
import ktx.actors.stage
import ktx.app.KtxScreen
import ktx.app.clearScreen
import kotlin.math.min

class LoadingScreen(private val assets: Assets, val onLoadDone: () -> Unit) : KtxScreen {
    private val label =
        Label(assets.i18n.get("loading"), assets.skin)
            .apply {
                setFillParent(true)
                setAlignment(Align.center)
                setWrap(true)
            }
    private val viewport = ScreenViewport()
        .apply {
            unitsPerPixel = min(1 / Gdx.graphics.density, 1f)
        }
    private val stage = stage(viewport = viewport)
        .apply { addActor(label) }

    override fun render(delta: Float) {
        clearScreen(0f, 0f, 0f, 1f)
        stage.draw()

        if (assets.manager.update()) {
            onLoadDone()
        }
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
    }
}