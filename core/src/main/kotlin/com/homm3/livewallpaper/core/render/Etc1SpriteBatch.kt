package com.homm3.livewallpaper.core.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * SpriteBatch that keeps GL_TEXTURE1 bound to the alpha companion of the
 * currently-bound color texture on GL_TEXTURE0. If no companion is registered
 * (legacy path), the same color texture is bound on TEXTURE1 — the shader's
 * u_useDualSample uniform controls whether it's actually sampled.
 */
class Etc1SpriteBatch(
    size: Int = 1000,
    private val companionLookup: () -> ((Texture) -> Texture?)
) : SpriteBatch(size) {

    private var lastColor: Texture? = null

    override fun switchTexture(texture: Texture) {
        super.switchTexture(texture)
        bindCompanion(texture)
        lastColor = texture
    }

    fun rebindCompanion() {
        val tex = lastColor ?: return
        bindCompanion(tex)
    }

    private fun bindCompanion(color: Texture) {
        val companion = companionLookup().invoke(color) ?: color
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE1)
        companion.bind(1)
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0)
    }
}
