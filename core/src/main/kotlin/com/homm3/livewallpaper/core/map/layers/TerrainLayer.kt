package com.homm3.livewallpaper.core.map.layers

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.maps.MapGroupLayer
import com.badlogic.gdx.maps.tiled.TiledMapTile
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import com.badlogic.gdx.utils.Array
import com.homm3.livewallpaper.core.GameConfig.FRAME_TIME
import com.homm3.livewallpaper.core.GameConfig.TILE_SIZE
import com.homm3.livewallpaper.core.assets.GameAssets
import com.homm3.livewallpaper.parser.h3m.H3mMap
import com.homm3.livewallpaper.parser.h3m.H3mTile
import ktx.collections.map

class TerrainLayer(private val assets: GameAssets, h3m: H3mMap, isUnderground: Boolean) :
    MapGroupLayer() {

    enum class TerrainDef(val id: Int) {
        dirttl(0), sandtl(1), grastl(2), snowtl(3), swmptl(4),
        rougtl(5), subbtl(6), lavatl(7), watrtl(8), rocktl(9),
        highlnd(10), wastlnd(11);

        companion object {
            fun byId(id: Int): String = entries.find { it.id == id }?.toString() ?: "dirttl"
        }
    }

    enum class RiverDef(val id: Int) {
        clrrvr(1), icyrvr(2), mudrvr(3), lavrvr(4);

        companion object {
            fun byId(id: Int): String = entries.find { it.id == id }?.toString() ?: "clrrvr"
        }
    }

    enum class RoadDef(val id: Int) {
        dirtrd(1), gravrd(2), cobbrd(3);

        companion object {
            fun byId(id: Int): String = entries.find { it.id == id }?.toString() ?: "dirtrd"
        }
    }

    private enum class TileType { TERRAIN, RIVER, ROAD }

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

    private fun createMapTile(frames: Array<AtlasRegion>): TiledMapTile {
        return if (frames.size > 1) {
            AnimatedTiledMapTile(FRAME_TIME, frames.map { StaticTiledMapTile(it) })
        } else {
            StaticTiledMapTile(frames.first())
        }
    }

    private fun createCell(tile: H3mTile, type: TileType): TiledMapTileLayer.Cell {
        return when (type) {
            TileType.TERRAIN -> TiledMapTileLayer.Cell().apply {
                this.tile = createMapTile(
                    assets.getTerrainFrames(TerrainDef.byId(tile.terrain), tile.terrainIndex)
                )
                this.flipHorizontally = tile.mirrorConfig.get(0)
                this.flipVertically = tile.mirrorConfig.get(1)
            }

            TileType.RIVER -> TiledMapTileLayer.Cell().apply {
                this.tile = createMapTile(
                    assets.getTerrainFrames(RiverDef.byId(tile.river), tile.riverIndex)
                )
                this.flipHorizontally = tile.mirrorConfig.get(2)
                this.flipVertically = tile.mirrorConfig.get(3)
            }

            TileType.ROAD -> TiledMapTileLayer.Cell().apply {
                this.tile = createMapTile(
                    assets.getTerrainFrames(RoadDef.byId(tile.road), tile.roadIndex)
                )
                this.flipHorizontally = tile.mirrorConfig.get(4)
                this.flipVertically = tile.mirrorConfig.get(5)
            }
        }
    }
}
