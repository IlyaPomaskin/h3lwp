package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Align
import com.homm3.livewallpaper.core.Constants.Companion.INSTRUCTIONS
import ktx.app.KtxScreen
import ktx.actors.*

class SettingsScreen(private val engine: Engine) : KtxScreen {
    private val stage = stage(viewport = engine.viewport)
    private val buttonText = "Open settings"

    init {
        stage.addActor(
            VerticalGroup().apply {
                center()
                grow()
                setFillParent(true)
                space(10.toFloat())
                pad(50.toFloat())
                addActor(
                    Label(INSTRUCTIONS, engine.skin).apply {
                        setWrap(true)
                        setAlignment(Align.center)
                    }
                )
                addActor(
                    TextButton(buttonText, engine.skin).apply {
                        onClick { engine.onSettingsButtonClick() }
                    }
                )
            }
        )
    }

    override fun show() {
        super.show()
        Gdx.input.inputProcessor = stage
    }

    override fun resize(width: Int, height: Int) {
        engine.viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        stage.dispose()
    }
}