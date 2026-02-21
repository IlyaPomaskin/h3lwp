package com.homm3.livewallpaper.core.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import com.homm3.livewallpaper.core.WallpaperPreferences
import ktx.graphics.use

class BrightnessOverlay(private val camera: OrthographicCamera) : Disposable {
    private val shapeRenderer = ShapeRenderer()
    var brightness = WallpaperPreferences.defaultBrightness

    fun render() {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapeRenderer.projectionMatrix = camera.view
        shapeRenderer.use(ShapeRenderer.ShapeType.Filled) {
            it.color = Color(0f, 0f, 0f, 1f - brightness)
            it.rect(
                camera.position.x - camera.viewportWidth / 2,
                camera.position.y - camera.viewportHeight / 2,
                camera.viewportWidth,
                camera.viewportHeight
            )
        }
    }

    override fun dispose() {
        shapeRenderer.dispose()
    }
}
