package com.homm3.livewallpaper.core

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTile
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.homm3.livewallpaper.core.Constants.Companion.FRAME_TIME
import com.homm3.livewallpaper.core.Constants.Companion.TILE_SIZE
import com.homm3.livewallpaper.parser.formats.H3m
import ktx.collections.map

class TerrainRenderer(private val engine: Engine, private val h3mMap: H3m) : Disposable {
    private val tiledMap = TiledMap()
    private val renderer = OrthogonalTiledMapRenderer(tiledMap)

    init {
        createLayers()
    }

    enum class TileType {
        TERRAIN,
        RIVER,
        ROAD
    }

    private fun createCell(tile: H3m.Tile, type: TileType): TiledMapTileLayer.Cell {
        return when (type) {
            TileType.TERRAIN -> TiledMapTileLayer.Cell().apply {
                this.tile = createMapTile(
                    engine.assets.getTerrainFrames(Constants.TerrainDefs.byInt(tile.terrain), tile.terrainImageIndex)
                )
                this.flipHorizontally = tile.mirrorConfig.get(0)
                this.flipVertically = tile.mirrorConfig.get(1)
            }
            TileType.RIVER -> TiledMapTileLayer.Cell().apply {
                this.tile = createMapTile(
                    engine.assets.getTerrainFrames(Constants.RiverDefs.byInt(tile.river), tile.riverImageIndex)
                )
                this.flipHorizontally = tile.mirrorConfig.get(2)
                this.flipVertically = tile.mirrorConfig.get(3)
            }
            TileType.ROAD -> TiledMapTileLayer.Cell().apply {
                this.tile = createMapTile(
                    engine.assets.getTerrainFrames(Constants.RoadDefs.byInt(tile.road), tile.roadImageIndex)
                )
                this.flipHorizontally = tile.mirrorConfig.get(4)
                this.flipVertically = tile.mirrorConfig.get(5)
            }
        }
    }

    private fun createMapTile(frames: Array<AtlasRegion>): TiledMapTile {
        return if (frames.size > 1) {
            AnimatedTiledMapTile(FRAME_TIME, frames.map { StaticTiledMapTile(it) })
        } else {
            StaticTiledMapTile(frames.first())
        }
    }

    private fun createLayers() {
        val mapSize = h3mMap.header.size
        val terrainLayer = TiledMapTileLayer(mapSize, mapSize, TILE_SIZE.toInt(), TILE_SIZE.toInt())
        val riverLayer = TiledMapTileLayer(mapSize, mapSize, TILE_SIZE.toInt(), TILE_SIZE.toInt())
        val roadLayer = TiledMapTileLayer(mapSize, mapSize, TILE_SIZE.toInt(), TILE_SIZE.toInt())

        roadLayer.offsetY = -(TILE_SIZE / 2)

        // TODO underground rendering
        for (x in 0 until mapSize) {
            for (y in 0 until mapSize) {
                val index = mapSize * y + x
                val tile = h3mMap.terrainTiles[index]
                terrainLayer.setCell(x, y, createCell(tile, TileType.TERRAIN))
                if (tile.river > 0) {
                    riverLayer.setCell(x, y, createCell(tile, TileType.RIVER))
                }
                if (tile.road > 0) {
                    roadLayer.setCell(x, y, createCell(tile, TileType.ROAD))
                }
            }
        }

        tiledMap.layers.add(terrainLayer)
        tiledMap.layers.add(riverLayer)
        tiledMap.layers.add(roadLayer)
    }

    fun render() {
        renderer.setView(engine.camera)
        renderer.render()
    }

    override fun dispose() {
        renderer.dispose()
        tiledMap.dispose()
    }
}