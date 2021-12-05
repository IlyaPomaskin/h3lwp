package com.homm3.livewallpaper.core.layers

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.MapGroupLayer
import com.homm3.livewallpaper.core.Assets
import com.homm3.livewallpaper.parser.formats.H3m

class H3mLayersGroup(assets: Assets, h3m: H3m) : MapGroupLayer() {
    val mapSize = h3m.header.size
    private val terrainLayer = TerrainGroupLayer(assets, h3m, false)
    private val objectsLayer = ObjectsLayer(assets, h3m, false)

    init {
        layers.add(terrainLayer)
        layers.add(objectsLayer)
    }

    fun updateVisibleSprites(camera: Camera) {
        objectsLayer.updateVisibleSprites(camera)
    }
}