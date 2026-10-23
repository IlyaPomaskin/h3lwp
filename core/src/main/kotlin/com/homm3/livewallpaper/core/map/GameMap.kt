package com.homm3.livewallpaper.core.map

import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.math.Rectangle
import com.homm3.livewallpaper.core.GameConfig
import com.homm3.livewallpaper.core.assets.SpriteSet
import com.homm3.livewallpaper.core.map.layers.BorderLayer
import com.homm3.livewallpaper.core.map.layers.ObjectsLayer
import com.homm3.livewallpaper.core.map.layers.TerrainLayer
import com.homm3.livewallpaper.parser.h3m.H3mMap
import ktx.log.logger

/**
 * One level (surface or underground) of a loaded map. The objects layer is built on the first coverage check
 * and the full [tiledMap] (terrain, objects, border) on first display; both are kept until the map is dropped.
 * Use it on the rendering thread only: building resolves sprites from the [SpriteSet].
 */
class GameMap(
    val spriteSet: SpriteSet,
    private val h3m: H3mMap,
    val fileName: String,
    val isUnderground: Boolean,
) {
    val mapSize = h3m.header.size
    private val objectsLayer by lazy(LazyThreadSafetyMode.NONE) { ObjectsLayer(spriteSet, h3m, isUnderground) }

    val tiledMap: TiledMap by lazy(LazyThreadSafetyMode.NONE) { buildTiledMap() }

    fun objectCoverage(cameraX: Float, cameraY: Float, viewportW: Float, viewportH: Float): Float {
        val viewBounds = Rectangle(
            cameraX - viewportW / 2, cameraY - viewportH / 2,
            viewportW, viewportH
        )
        return objectsLayer.objectCoverage(viewBounds)
    }

    private fun buildTiledMap(): TiledMap {
        val buildStart = System.currentTimeMillis()
        val map = TiledMap().apply {
            layers.add(TerrainLayer(spriteSet, h3m, isUnderground))
            layers.add(objectsLayer)
            layers.add(BorderLayer(spriteSet, mapSize, GameConfig.BORDER_TILE_COUNT, GameConfig.BORDER_TILE_COUNT))
        }
        log.info { "Built '$fileName' (underground=$isUnderground) in ${System.currentTimeMillis() - buildStart}ms" }
        return map
    }

    companion object {
        private val log = logger<GameMap>()
    }
}
