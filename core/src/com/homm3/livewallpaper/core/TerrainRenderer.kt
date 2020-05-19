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
import com.homm3.livewallpaper.parser.formats.JsonMap.ParsedMap
import com.homm3.livewallpaper.parser.formats.JsonMap.TerrainTile
import ktx.collections.map

class TerrainRenderer(private val engine: Engine, private val h3mMap: ParsedMap) : Disposable {
    private val tiledMap = TiledMap()
    private val renderer = OrthogonalTiledMapRenderer(tiledMap)

    init {
        createLayers()
    }

    private fun createCell(tile: TerrainTile, type: String): TiledMapTileLayer.Cell {
        val mirrorConfig = tile.mirror()

        return when (type) {
            "terrain" -> TiledMapTileLayer.Cell().apply {
                this.tile = createMapTile(
                    engine.assets.getTerrainFrames(Constants.TerrainDefs.byInt(tile.terrain), tile.terrainImageIndex)
                )
                this.flipHorizontally = mirrorConfig.get(0)
                this.flipVertically = mirrorConfig.get(1)
            }
            "river" -> TiledMapTileLayer.Cell().apply {
                this.tile = createMapTile(
                    engine.assets.getTerrainFrames(Constants.RiverDefs.byInt(tile.river), tile.riverImageIndex)
                )
                this.flipHorizontally = mirrorConfig.get(2)
                this.flipVertically = mirrorConfig.get(3)
            }
            "road" -> TiledMapTileLayer.Cell().apply {
                this.tile = createMapTile(
                    engine.assets.getTerrainFrames(Constants.RoadDefs.byInt(tile.road), tile.roadImageIndex)
                )
                this.flipHorizontally = mirrorConfig.get(4)
                this.flipVertically = mirrorConfig.get(5)
            }
            else -> throw Exception("Incorrect tile")
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
        val terrainLayer = TiledMapTileLayer(h3mMap.size, h3mMap.size, TILE_SIZE.toInt(), TILE_SIZE.toInt())
        val riverLayer = TiledMapTileLayer(h3mMap.size, h3mMap.size, TILE_SIZE.toInt(), TILE_SIZE.toInt())
        val roadLayer = TiledMapTileLayer(h3mMap.size, h3mMap.size, TILE_SIZE.toInt(), TILE_SIZE.toInt())

        roadLayer.offsetY = -(TILE_SIZE / 2)

        for (x in 0 until h3mMap.size) {
            for (y in 0 until h3mMap.size) {
                val index = h3mMap.size * y + x
                val tile = h3mMap.terrain[index]
                terrainLayer.setCell(x, y, createCell(tile, "terrain"))
                if (tile.river > 0) {
                    riverLayer.setCell(x, y, createCell(tile, "river"))
                }
                if (tile.road > 0) {
                    roadLayer.setCell(x, y, createCell(tile, "road"))
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