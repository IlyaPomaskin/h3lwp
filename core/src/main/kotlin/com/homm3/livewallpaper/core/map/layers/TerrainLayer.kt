package com.homm3.livewallpaper.core.map.layers

import com.badlogic.gdx.maps.MapGroupLayer
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.homm3.livewallpaper.core.GameConfig.TILE_SIZE
import com.homm3.livewallpaper.core.assets.SpriteSet
import com.homm3.livewallpaper.core.defs.RiverDef
import com.homm3.livewallpaper.core.defs.RoadDef
import com.homm3.livewallpaper.core.defs.TerrainDef
import com.homm3.livewallpaper.parser.h3m.H3mMap
import com.homm3.livewallpaper.parser.h3m.H3mTile

class TerrainLayer(private val assets: SpriteSet, h3m: H3mMap, isUnderground: Boolean) :
    MapGroupLayer() {

    private enum class TileType { TERRAIN, RIVER, ROAD }

    private data class TileKey(val defName: String, val frameIndex: Int)

    private val tileCache = TileCache<TileKey> { key ->
        createMapTile(assets.getTerrainFrames(key.defName, key.frameIndex))
    }

    init {
        val mapSize = h3m.header.size
        val tileSize = TILE_SIZE.toInt()
        val terrainTileLayer = TiledMapTileLayer(mapSize, mapSize, tileSize, tileSize)
        val riverTileLayer = TiledMapTileLayer(mapSize, mapSize, tileSize, tileSize)
        val roadTileLayer = TiledMapTileLayer(mapSize, mapSize, tileSize, tileSize)
        roadTileLayer.offsetY = -(TILE_SIZE / 2)

        val tileOffset = if (isUnderground) mapSize * mapSize else 0
        for (x in 0 until mapSize) {
            for (y in 0 until mapSize) {
                val index = tileOffset + mapSize * y + x
                val tile = h3m.tiles[index]

                terrainTileLayer.setCell(x, y, createCell(tile, TileType.TERRAIN))

                if (tile.river > 0) {
                    riverTileLayer.setCell(x, y, createCell(tile, TileType.RIVER))
                }

                if (tile.road > 0) {
                    roadTileLayer.setCell(x, y, createCell(tile, TileType.ROAD))
                }
            }
        }

        layers.add(terrainTileLayer)
        layers.add(riverTileLayer)
        layers.add(roadTileLayer)
        name = "terrain"
    }

    private fun createCell(tile: H3mTile, type: TileType): TiledMapTileLayer.Cell {
        return when (type) {
            TileType.TERRAIN -> TiledMapTileLayer.Cell().apply {
                this.tile = tileCache[TileKey(TerrainDef.byId(tile.terrain), tile.terrainIndex)]
                this.flipHorizontally = tile.mirrorConfig.get(0)
                this.flipVertically = tile.mirrorConfig.get(1)
            }

            TileType.RIVER -> TiledMapTileLayer.Cell().apply {
                this.tile = tileCache[TileKey(RiverDef.byId(tile.river), tile.riverIndex)]
                this.flipHorizontally = tile.mirrorConfig.get(2)
                this.flipVertically = tile.mirrorConfig.get(3)
            }

            TileType.ROAD -> TiledMapTileLayer.Cell().apply {
                this.tile = tileCache[TileKey(RoadDef.byId(tile.road), tile.roadIndex)]
                this.flipHorizontally = tile.mirrorConfig.get(4)
                this.flipVertically = tile.mirrorConfig.get(5)
            }
        }
    }
}
