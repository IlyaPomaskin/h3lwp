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
    private val labelText = "To use this wallpaper you must provide files from \"Heroes of Might and Magic 3: Shadow of the Death\""
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
                        onClick {
                            engine.onSettingsButtonClick {
                                Gdx.app.postRunnable {
                                    engine.getScreen<WallpaperScreen>().tryLoadAssets()
                                }
                            }
                        }
                    }
                )
            }
        )
    }

    override fun show() {
        super.show()
        Gdx.input.inputProcessor = stage
        engine.camera.zoom = 1f
        engine.getScreen<WallpaperScreen>().tryLoadAssets()
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