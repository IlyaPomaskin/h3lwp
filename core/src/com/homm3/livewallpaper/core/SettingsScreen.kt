package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Align
import ktx.app.KtxScreen
import ktx.actors.*

class SettingsScreen(private val engine: Engine) : KtxScreen {
    private val stage = stage(viewport = engine.viewport)
    private val labelText = "To use this app you must provide files from Heroes 3 SoD or Complete (HD Edition will not work).\n" +
        "Open the game folder on PC, find folder named  \"Data\", and upload file named \"h3sprite.lod\" to your phone. " +
        "Click the button below and select the uploaded file."
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
                    Label(labelText, engine.skin).apply {
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