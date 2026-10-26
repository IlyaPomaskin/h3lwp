package com.homm3.livewallpaper.core.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch

/**
 * SpriteBatch that keeps the palette texture bound on [PALETTE_UNIT] while index pages go through unit 0.
 * Rebinding happens on begin and before every flush (a texture switch flushes first), so a palette upload
 * (which binds the palette on whatever unit is active) between draws cannot leave a stale binding.
 */
class PaletteSpriteBatch(
    size: Int = DEFAULT_SIZE,
    private val paletteTexture: () -> Texture?,
) : SpriteBatch(size) {

    override fun begin() {
        super.begin()
        bindPalette()
    }

    override fun flush() {
        bindPalette()
        super.flush()
    }

    private fun bindPalette() {
        val palette = paletteTexture() ?: return
        palette.bind(PALETTE_UNIT)
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0)
    }

    companion object {
        const val PALETTE_UNIT = 1
        private const val DEFAULT_SIZE = 1000
    }
}
