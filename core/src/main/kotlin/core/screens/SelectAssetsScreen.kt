package core.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.Align
import com.homm3.livewallpaper.core.Assets
import ktx.app.KtxScreen
import ktx.actors.*
import ktx.app.clearScreen

class SelectAssetsScreen(private val assets: Assets, private val onSettingsButtonClick: () -> Unit) : KtxScreen {
    private val stage = stage()

    init {
        stage.addActor(
            VerticalGroup().apply {
                center()
                grow()
                setFillParent(true)
                space(10.toFloat())
                pad(50.toFloat())
                addActor(
                    Label(assets.i18n.get("instructions"), assets.skin).apply {
                        setWrap(true)
                        setAlignment(Align.center)
                    }
                )
                addActor(
                    TextButton(assets.i18n.get("openSettings"), assets.skin).apply {
                        onClick { onSettingsButtonClick() }
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