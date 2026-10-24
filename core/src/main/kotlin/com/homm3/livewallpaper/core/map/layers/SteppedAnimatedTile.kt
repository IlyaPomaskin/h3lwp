package com.homm3.livewallpaper.core.map.layers

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import com.homm3.livewallpaper.core.render.AnimationClock

/** Animated tile driven by [AnimationClock] instead of [com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile]'s wall clock. */
internal class SteppedAnimatedTile(private val frames: List<TextureRegion>) : StaticTiledMapTile(frames.first()) {
    override fun getTextureRegion(): TextureRegion = frames[(AnimationClock.frameStep % frames.size).toInt()]
}
