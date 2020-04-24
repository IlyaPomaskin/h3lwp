package com.heroes3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxScreen
import ktx.actors.*

class SettingsScreen(private val engine: Engine) : KtxScreen {
    private val stage = stage(viewport = engine.viewport)
    private val skin = Skin(Gdx.files.local("sprites/skin/uiskin.json"))

    init {
        val labelGroup = VerticalGroup().apply {
            this.grow()
            this.padBottom(30.toFloat())
            this.addActor(
                Label("Some text", skin).apply {
                    this.setWrap(true)
                    this.setAlignment(Align.center)
                }
            )
        }
        val group = VerticalGroup().apply {
            this.center()
            this.grow()
            this.setFillParent(true)
            this.space(10.toFloat())
            this.pad(50.toFloat())
            this.addActor(labelGroup)
            this.addActor(
                TextButton("Open settings", skin).apply {
                    this.onChange {
                        engine.onSettingButtonClick()
                        engine.start()
                    }
                }
            )
        }

        stage.addActor(group)
    }

    override fun show() {
        super.show()
        engine.viewport.unitsPerPixel = 0.5f
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