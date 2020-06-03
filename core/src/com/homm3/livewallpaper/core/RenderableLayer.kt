package com.homm3.livewallpaper.core

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.maps.MapLayer

abstract class RenderableLayer : MapLayer() {
    abstract fun render(batch: Batch)
}