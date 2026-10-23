package com.homm3.livewallpaper.core.assets.indexed

import com.badlogic.gdx.graphics.Color

/**
 * Encodes a palette slot into the SpriteBatch vertex colour: r = slot low byte, g = slot high byte,
 * b unused, a = alpha. The shader decodes it back (see PaletteShader).
 */
object PaletteColor {
    private const val BYTE_MAX = 255f
    private const val SLOT_BYTE = 256

    fun encode(slot: Int, alpha: Float = 1f): Float {
        require(slot in 0 until MAX_PALETTE_SLOTS) { "palette slot out of range: $slot" }
        return Color.toFloatBits((slot % SLOT_BYTE) / BYTE_MAX, (slot / SLOT_BYTE) / BYTE_MAX, 0f, alpha)
    }
}
