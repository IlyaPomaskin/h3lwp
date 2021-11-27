package com.homm3.livewallpaper.core

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import ktx.actors.stage
import ktx.app.KtxScreen
import ktx.app.clearScreen
import kotlin.Exception

class LoadingScreen(private val engine: Engine) : KtxScreen {
    private val label = Label(engine.assets.i18n.get("loading"), engine.assets.skin)
        .apply {
            setFillParent(true)
            setAlignment(Align.center)
            setWrap(true)
        }
    private val loadingStage = stage().apply { addActor(label) }
    private var isError = false

    private fun showError(ex: Exception) {
        isError = true
        label.setText(engine.assets.i18n.get("loadingError"))
        ex.printStackTrace()
    }

    override fun render(delta: Float) {
        clearScreen(0f, 0f, 0f, 1f)
        loadingStage.draw()

        try {
            if (!isError && engine.assets.manager.update()) {
                engine.updateVisibleScreen()
            }
        } catch (ex: Exception) {
            showError(ex)
        }
    }

    override fun resize(width: Int, height: Int) {
        loadingStage.viewport.update(width, height, true)
    }

    override fun dispose() {
        loadingStage.dispose()
    }
}