package com.homm3.livewallpaper.core.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.homm3.livewallpaper.core.assets.GameAssets
import ktx.actors.stage
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.scene2d.actors
import ktx.scene2d.label
import kotlin.math.min

class LoadingScreen(private val assets: GameAssets) : KtxScreen {

    private val viewport = ScreenViewport().apply {
        unitsPerPixel = min(1 / Gdx.graphics.density, 1f)
    }
    private val stage = stage(viewport = viewport).apply {
        actors {
            label(assets.i18n.get("loading"), skin = assets.skin) {
                setFillParent(true)
                setAlignment(Align.center)
                setWrap(true)
            }
        }
    }

    override fun render(delta: Float) {
        clearScreen(0f, 0f, 0f, 1f)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
    }
}
