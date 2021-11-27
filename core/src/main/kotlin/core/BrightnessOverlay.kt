package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import ktx.graphics.use

class BrightnessOverlay(private val camera: OrthographicCamera) {
    private val brightnessOverlay = ShapeRenderer()
    var brightness = Constants.Preferences.BRIGHTNESS_DEFAULT

    fun render() {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        brightnessOverlay.projectionMatrix = camera.view
        brightnessOverlay.use(ShapeRenderer.ShapeType.Filled) {
            it.color = Color(0f, 0f, 0f, (100 - brightness) / 100f)
            it.rect(
                camera.position.x - camera.viewportWidth / 2,
                camera.position.y - camera.viewportHeight / 2,
                camera.viewportWidth,
                camera.viewportHeight
            )
        }
    }
}