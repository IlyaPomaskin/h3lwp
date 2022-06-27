package com.homm3.livewallpaper.core.layers

import com.badlogic.gdx.maps.MapGroupLayer
import com.homm3.livewallpaper.core.Assets
import com.homm3.livewallpaper.parser.formats.H3m

class H3mLayersGroup(assets: Assets, h3m: H3m) : MapGroupLayer() {
    val mapSize = h3m.header.size

    val terrain = TerrainGroupLayer(assets, h3m, h3m.header.hasUnderground)

    init {
        layers.add(terrain)
        layers.add(ObjectsLayer(assets, h3m, h3m.header.hasUnderground))
    }
}