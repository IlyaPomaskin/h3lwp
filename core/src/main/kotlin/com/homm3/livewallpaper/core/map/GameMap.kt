package com.homm3.livewallpaper.core.map

import com.badlogic.gdx.maps.MapGroupLayer
import com.homm3.livewallpaper.core.GameConfig
import com.homm3.livewallpaper.core.assets.GameAssets
import com.homm3.livewallpaper.core.map.layers.BorderLayer
import com.homm3.livewallpaper.core.map.layers.ObjectsLayer
import com.homm3.livewallpaper.core.map.layers.TerrainLayer
import com.homm3.livewallpaper.parser.h3m.H3mMap

class GameMap(assets: GameAssets, h3m: H3mMap, val fileName: String = "") : MapGroupLayer() {
    val mapSize = h3m.header.size

    init {
        val terrainLayer = TerrainLayer(assets, h3m, false)
        val objectsLayer = ObjectsLayer(assets, h3m, false)
        val borderLayer = BorderLayer(
            assets, h3m.header.size,
            GameConfig.BORDER_TILE_COUNT, GameConfig.BORDER_TILE_COUNT
        )

        layers.add(terrainLayer)
        layers.add(objectsLayer)
        layers.add(borderLayer)
    }
}
