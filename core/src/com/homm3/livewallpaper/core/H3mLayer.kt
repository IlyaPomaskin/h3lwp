package com.homm3.livewallpaper.core

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.maps.MapGroupLayer
import com.homm3.livewallpaper.parser.formats.H3m

class H3mLayer(engine: Engine, h3mMap: H3m, isUnderground: Boolean) : MapGroupLayer() {
    private val objectsLayer = ObjectsLayer(engine, h3mMap, isUnderground)
    val mapSize = h3mMap.header.size

    fun updateVisibleObjects(camera: Camera) {
        objectsLayer.updateVisibleSprites(camera)
    }

    init {
        isVisible = false;
        layers.add(TerrainGroupLayer(engine.assets, h3mMap, isUnderground))
        layers.add(objectsLayer)
        layers.add(BorderLayer(engine.assets, mapSize, Constants.BORDER_SIZE, Constants.BORDER_SIZE))
    }
}