package com.homm3.livewallpaper.core.render

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.homm3.livewallpaper.core.map.layers.ObjectsLayer

class Etc1TiledMapRenderer(
    map: TiledMap,
    batch: Etc1SpriteBatch,
) : OrthogonalTiledMapRenderer(map, batch as SpriteBatch) {

    val dualShader = Etc1DualShader.compile()

    init {
        batch.shader = dualShader
        Etc1DualShader.setUseDualSample(dualShader, true)
    }

    fun setDualSampleEnabled(on: Boolean) {
        Etc1DualShader.setUseDualSample(dualShader, on)
    }

    override fun renderObjects(layer: MapLayer?) {
        if (layer is ObjectsLayer) layer.render(this)
    }

    override fun dispose() {
        super.dispose()
        dualShader.dispose()
    }
}
