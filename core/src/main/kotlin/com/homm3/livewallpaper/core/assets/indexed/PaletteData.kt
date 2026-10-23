package com.homm3.livewallpaper.core.assets.indexed

/**
 * One 256-colour palette, RGBA8888 (1024 bytes), special indices already applied
 * (index 0 transparent, shadows as black with alpha, player colour neutralised).
 *
 * [rotations]: palette-cycling ranges as in DefPalette.paletteRotations, e.g. 229..241 means
 * entries [229, 241) rotate left by one entry per animation step. Empty for static palettes.
 */
class PaletteData(val rgba: ByteArray, val rotations: List<Pair<Int, Int>> = emptyList()) {
    init {
        require(rgba.size == PALETTE_BYTES) { "palette must be $PALETTE_BYTES bytes, got ${rgba.size}" }
    }
}
