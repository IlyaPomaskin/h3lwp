package com.homm3.livewallpaper.core.assets.indexed

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion

/** AtlasRegion over an index page; [paletteSlot] is its palette slot in the palette texture. */
class PaletteRegion(texture: Texture, x: Int, y: Int, width: Int, height: Int, val paletteSlot: Int) :
    AtlasRegion(texture, x, y, width, height)
