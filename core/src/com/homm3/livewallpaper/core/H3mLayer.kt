package com.homm3.livewallpaper.core

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.MapGroupLayer
import com.homm3.livewallpaper.core.Constants.Companion.TILE_SIZE
import com.homm3.livewallpaper.parser.formats.H3m

class H3mLayer(engine: Engine, h3mMap: H3m) : MapGroupLayer() {
    val mapSize = h3mMap.header.size * TILE_SIZE

    fun updateVisibleObjects(camera: Camera) {
        layers
            .getByType(ObjectsLayer::class.java)
            .firstOrNull()
            ?.updateVisibleSprites(camera)
    }

    init {
        isVisible = false

        layers.add(TerrainGroupLayer(engine.assets, h3mMap, false))
        layers.add(ObjectsLayer(engine, h3mMap, false))
        layers.add(BorderLayer(engine.assets, h3mMap.header.size, Constants.BORDER_SIZE, Constants.BORDER_SIZE))
    }
}