package com.homm3.livewallpaper.core

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import ktx.actors.stage
import ktx.app.KtxScreen

class LoadingScreen(private val engine: Engine) : KtxScreen {
    private val loadingStage = stage(viewport = engine.viewport)
        .apply {
            addActor(
                Label("Loading...", engine.skin)
                    .apply {
                        setFillParent(true)
                        setAlignment(Align.center)
                    }
            )
        }

    override fun render(delta: Float) {
        super.render(delta)
        loadingStage.draw()
        if (engine.assets.manager.update()) {
            engine.updateVisibleScreen()
        }
    }

    override fun show() {
        super.show()
        engine.camera.zoom = 1f
    }

    override fun resize(width: Int, height: Int) {
        engine.viewport.update(width, height, true)
    }

    override fun dispose() {
        loadingStage.dispose()
    }
}