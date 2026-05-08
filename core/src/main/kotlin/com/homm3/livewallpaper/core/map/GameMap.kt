package com.homm3.livewallpaper.core.map

import com.badlogic.gdx.maps.MapGroupLayer
import com.homm3.livewallpaper.core.GameConfig
import com.homm3.livewallpaper.core.GameConfig.TILE_SIZE
import com.homm3.livewallpaper.core.assets.GameAssets
import com.homm3.livewallpaper.core.map.layers.BorderLayer
import com.homm3.livewallpaper.core.map.layers.ObjectsLayer
import com.homm3.livewallpaper.core.map.layers.TerrainLayer
import com.homm3.livewallpaper.parser.h3m.H3mMap

class GameMap(private val assets: GameAssets, private val h3m: H3mMap, val fileName: String = "", val isUnderground: Boolean = false) : MapGroupLayer() {
    val mapSize = h3m.header.size

    init {
        val terrainLayer = TerrainLayer(assets, h3m, isUnderground)
        val objectsLayer = ObjectsLayer(assets, h3m, isUnderground)
        val borderLayer = BorderLayer(
            assets, h3m.header.size,
            GameConfig.BORDER_TILE_COUNT, GameConfig.BORDER_TILE_COUNT
        )

        layers.add(terrainLayer)
        layers.add(objectsLayer)
        layers.add(borderLayer)
    }

    fun terrainCoverage(cameraX: Float, cameraY: Float, viewportW: Float, viewportH: Float): Float {
        val startX = ((cameraX - viewportW / 2) / TILE_SIZE).toInt().coerceIn(0, mapSize - 1)
        val endX = ((cameraX + viewportW / 2) / TILE_SIZE).toInt().coerceIn(0, mapSize - 1)
        val startY = ((cameraY - viewportH / 2) / TILE_SIZE).toInt().coerceIn(0, mapSize - 1)
        val endY = ((cameraY + viewportH / 2) / TILE_SIZE).toInt().coerceIn(0, mapSize - 1)

        val tileOffset = if (isUnderground) mapSize * mapSize else 0
        var total = 0
        var filled = 0
        for (x in startX..endX) {
            for (y in startY..endY) {
                val tile = h3m.tiles[tileOffset + mapSize * y + x]
                val defName = TerrainLayer.TerrainDef.byId(tile.terrain)
                if (assets.hasTerrainFrames(defName, tile.terrainIndex)) filled++
                total++
            }
        }
        return if (total > 0) filled.toFloat() / total else 0f
    }
}
