package com.homm3.livewallpaper.core.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.homm3.livewallpaper.core.assets.GameAssets
import ktx.actors.onClick
import ktx.actors.stage
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.scene2d.actors
import ktx.scene2d.label
import ktx.scene2d.textButton
import ktx.scene2d.verticalGroup
import kotlin.math.min

class AssetSetupScreen(
    private val assets: GameAssets,
    private val onSettingsButtonClick: (onProgress: (String) -> Unit) -> Unit
) : KtxScreen {

    private val viewport = ScreenViewport().apply {
        unitsPerPixel = min(1 / Gdx.graphics.density, 1f)
    }
    private lateinit var statusLabel: Label
    private val stage = stage(viewport = viewport).apply {
        actors {
            verticalGroup {
                center()
                grow()
                setFillParent(true)
                space(10f)
                pad(50f)
                label(assets.i18n.get("instructions"), skin = assets.skin) {
                    setWrap(true)
                    setAlignment(Align.center)
                }
                textButton(assets.i18n.get("openSettings"), skin = assets.skin) {
                    onClick {
                        onSettingsButtonClick { status ->
                            Gdx.app.postRunnable { statusLabel.setText(status) }
                        }
                    }
                }
                this@AssetSetupScreen.statusLabel = label("", skin = assets.skin) {
                    setWrap(true)
                    setAlignment(Align.center)
                }
            }
        }
    }

    override fun show() {
        Gdx.input.inputProcessor = stage
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        clearScreen(0f, 0f, 0f, 1f)
        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        stage.dispose()
    }
}
