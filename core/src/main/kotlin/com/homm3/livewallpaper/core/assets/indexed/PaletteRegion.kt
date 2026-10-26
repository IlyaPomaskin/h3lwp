package com.homm3.livewallpaper.core.assets.indexed

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion

/** AtlasRegion over an index page; [paletteSlots] are its palette slots in the palette texture. */
class PaletteRegion(texture: Texture, x: Int, y: Int, width: Int, height: Int, val paletteSlots: PaletteSlots) :
    AtlasRegion(texture, x, y, width, height)
