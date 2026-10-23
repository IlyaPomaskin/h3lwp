package com.homm3.livewallpaper.core.assets

import com.homm3.livewallpaper.parser.h3m.H3mMap

data class LoadedMap(val fileName: String, val h3m: H3mMap)

/** Maps of one batch plus the [spriteSet] built for them; the receiver owns and must dispose the set. */
class MapBatch(val maps: List<LoadedMap>, val spriteSet: SpriteSet)
