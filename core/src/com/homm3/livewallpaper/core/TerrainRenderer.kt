package com.homm3.livewallpaper.core

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTile
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.homm3.livewallpaper.core.Constants.Companion.FRAME_TIME
import com.homm3.livewallpaper.core.Constants.Companion.TILE_SIZE
import com.homm3.livewallpaper.parser.formats.H3m
import ktx.collections.map
import kotlin.random.Random

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

    private fun createBorderCell(from: Int, to: Int? = null): TiledMapTileLayer.Cell {
        val index = if (to == null) from else Random.nextInt(from, to)

        return TiledMapTileLayer.Cell().apply {
            this.tile = StaticTiledMapTile(
                engine.assets.getTerrainFrames("edg", index)[0]
            )
        }
    }

    private fun createBorderLayer(mapSize: Int): MapLayer {
        val borderSize = 10
        val mapSizeWithBorder = mapSize + borderSize * 2
        val borderLayer = TiledMapTileLayer(mapSizeWithBorder, mapSizeWithBorder, TILE_SIZE.toInt(), TILE_SIZE.toInt())
        borderLayer.offsetX = -(borderSize * TILE_SIZE)
        borderLayer.offsetY = (borderSize * TILE_SIZE)

        val mapStart = borderSize - 1
        val mapEnd = borderSize + mapSize
        val rect = Rectangle(borderSize.toFloat(), borderSize.toFloat(), mapSize - 1f, mapSize - 1f)
        val borderRange = (0..mapSizeWithBorder)
        borderRange.forEach(fun(x) {
            borderRange.forEach(fun(y) {
                if (rect.contains(x.toFloat(), y.toFloat())) {
                    return
                }

                val borderCell = when {
                    x in mapStart until mapEnd && y == mapStart -> createBorderCell(21, 24)
                    y in mapStart until mapEnd && x == mapEnd -> createBorderCell(25, 28)
                    x in mapStart until mapEnd && y == mapEnd -> createBorderCell(29, 32)
                    y in mapStart until mapEnd && x == mapStart -> createBorderCell(33, 36)
                    else -> createBorderCell(0, 16)
                }

                borderLayer.setCell(x, y, borderCell)
            })
        })

        borderLayer.setCell(mapStart, mapStart, createBorderCell(16))
        borderLayer.setCell(mapEnd, mapStart, createBorderCell(17))
        borderLayer.setCell(mapEnd, mapEnd, createBorderCell(18))
        borderLayer.setCell(mapStart, mapEnd, createBorderCell(19))

        return borderLayer
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
//        tiledMap.layers.add(createBorderLayer(mapSize))
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