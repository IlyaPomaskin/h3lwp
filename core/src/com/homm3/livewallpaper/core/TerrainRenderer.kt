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
import java.util.*

class TerrainRenderer(private val engine: Engine, private val h3mMap: ParsedMap) : Disposable {
    private var tiledMap = TiledMap()
    private val renderer = OrthogonalTiledMapRenderer(tiledMap)

    init {
        createLayers()
    }

    class TileSettings(
        val frames: Array<AtlasRegion>,
        val flipX: Boolean,
        val flipY: Boolean
    )

    object Bits {
        fun convert(input: Long): BitSet {
            var value = input
            val bits = BitSet()
            var index = 0
            while (value != 0L) {
                if (value % 2L != 0L) {
                    bits.set(index)
                }
                ++index
                value = value ushr 1
            }
            return bits
        }
    }

    private fun getTileSettingsByType(tile: TerrainTile, type: String): TileSettings {
        return when (type) {
            "terrain" -> TileSettings(
                engine.assets.getTerrainFrames(Constants.TerrainDefs.byInt(tile.terrain), tile.terrainImageIndex),
                Bits.convert(tile.mirrorConfig).get(0),
                Bits.convert(tile.mirrorConfig).get(1)
            )
            "river" -> TileSettings(
                engine.assets.getTerrainFrames(Constants.RiverDefs.byInt(tile.river), tile.riverImageIndex),
                Bits.convert(tile.mirrorConfig).get(2),
                Bits.convert(tile.mirrorConfig).get(3)
            )
            "road" -> TileSettings(
                engine.assets.getTerrainFrames(Constants.RoadDefs.byInt(tile.road), tile.roadImageIndex),
                Bits.convert(tile.mirrorConfig).get(4),
                Bits.convert(tile.mirrorConfig).get(5)
            )
            else -> throw Exception("Incorrect tile settings")
        }
    }

    private fun createMapTile(frames: Array<AtlasRegion>): TiledMapTile {
        return if (frames.size > 1) {
            AnimatedTiledMapTile(FRAME_TIME, frames.map { StaticTiledMapTile(it) })
        } else {
            StaticTiledMapTile(frames.first())
        }
    }

    private fun createTile(tile: TerrainTile, type: String): TiledMapTileLayer.Cell {
        val settings = getTileSettingsByType(tile, type)
        val cell = TiledMapTileLayer.Cell()
        cell.tile = createMapTile(settings.frames)
        cell.flipHorizontally = settings.flipX
        cell.flipVertically = settings.flipY
        return cell
    }

    private fun createLayers() {
        val terrainLayer = TiledMapTileLayer(h3mMap.size, h3mMap.size, TILE_SIZE.toInt(), TILE_SIZE.toInt())
        val riverLayer = TiledMapTileLayer(h3mMap.size, h3mMap.size, TILE_SIZE.toInt(), TILE_SIZE.toInt())
        val roadLayer = TiledMapTileLayer(h3mMap.size, h3mMap.size, TILE_SIZE.toInt(), TILE_SIZE.toInt())

        roadLayer.offsetY = -16f

        for (x in 0 until h3mMap.size) {
            for (y in 0 until h3mMap.size) {
                val index = h3mMap.size * y + x
                val tile = h3mMap.terrain[index]
                terrainLayer.setCell(x, y, createTile(tile, "terrain"))
                if (tile.river > 0) {
                    riverLayer.setCell(x, y, createTile(tile, "river"))
                }
                if (tile.road > 0) {
                    roadLayer.setCell(x, y, createTile(tile, "road"))
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