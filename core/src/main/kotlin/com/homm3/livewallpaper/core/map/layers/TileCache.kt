package com.homm3.livewallpaper.core.map.layers

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.maps.tiled.TiledMapTile
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import com.badlogic.gdx.utils.Array
import com.homm3.livewallpaper.core.GameConfig.FRAME_TIME
import ktx.collections.map

internal class TileCache<K>(private val createTile: (K) -> TiledMapTile) {
    private val tiles = HashMap<K, TiledMapTile>()

    val size: Int get() = tiles.size

    operator fun get(key: K): TiledMapTile = tiles.getOrPut(key) { createTile(key) }
}

internal fun createMapTile(frames: Array<AtlasRegion>): TiledMapTile {
    val isAnimated = frames.size > 1
    if (isAnimated) {
        return AnimatedTiledMapTile(FRAME_TIME, frames.map { StaticTiledMapTile(it) })
    }
    return StaticTiledMapTile(frames.first())
}
