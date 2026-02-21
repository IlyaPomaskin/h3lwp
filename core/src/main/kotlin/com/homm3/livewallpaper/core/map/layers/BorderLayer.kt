package com.homm3.livewallpaper.core.map.layers

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import com.badlogic.gdx.math.Rectangle
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.core.GameConfig
import com.homm3.livewallpaper.core.assets.GameAssets
import ktx.log.logger
import kotlin.random.Random

class BorderLayer(
    private val assets: GameAssets,
    private val mapSize: Int,
    borderWidth: Int,
    borderHeight: Int
) : TiledMapTileLayer(
    mapSize + borderWidth * 2,
    mapSize + borderHeight * 2,
    GameConfig.TILE_SIZE.toInt(),
    GameConfig.TILE_SIZE.toInt()
) {
    init {
        offsetX = -(borderWidth * GameConfig.TILE_SIZE)
        offsetY = (borderHeight * GameConfig.TILE_SIZE)

        val xStart = borderWidth - 1
        val xEnd = borderWidth + mapSize
        val yStart = borderHeight - 1
        val yEnd = borderHeight + mapSize
        val mapRect = Rectangle(
            borderWidth.toFloat(), borderHeight.toFloat(),
            mapSize - 1f, mapSize - 1f
        )

        (0..mapSize + borderWidth * 2).forEach { x ->
            (0..mapSize + borderHeight * 2).forEach { y ->
                if (mapRect.contains(x.toFloat(), y.toFloat())) {
                    return@forEach
                }

                val borderCell = when {
                    x in xStart until xEnd && y == yStart -> createBorderCell(TOP_EDGE_START, TOP_EDGE_END)
                    y in yStart until yEnd && x == xEnd -> createBorderCell(RIGHT_EDGE_START, RIGHT_EDGE_END)
                    x in xStart until xEnd && y == yEnd -> createBorderCell(BOTTOM_EDGE_START, BOTTOM_EDGE_END)
                    y in yStart until yEnd && x == xStart -> createBorderCell(LEFT_EDGE_START, LEFT_EDGE_END)
                    else -> createBorderCell(INTERIOR_START, INTERIOR_END)
                }

                setCell(x, y, borderCell)
            }
        }

        setCell(xStart, yStart, createBorderCell(CORNER_TL))
        setCell(xEnd, yStart, createBorderCell(CORNER_TR))
        setCell(xEnd, yEnd, createBorderCell(CORNER_BR))
        setCell(xStart, yEnd, createBorderCell(CORNER_BL))

        name = "border"
    }

    private fun getBorderFrame(index: Int): TextureAtlas.AtlasRegion {
        val regions = assets.manager
            .get<TextureAtlas>(AssetPaths.ATLAS_PATH)
            .findRegions("edg/$index")

        return if (regions.isEmpty) {
            log.error { "Can't find border frame edg/$index" }
            assets.manager.get<TextureAtlas>(AssetPaths.ATLAS_PATH)
                .findRegions("edg/0").first()
        } else {
            regions.first()
        }
    }

    private fun createBorderCell(from: Int, to: Int? = null): Cell {
        val index = if (to == null) from else Random.nextInt(from, to)
        val cell = Cell()
        cell.tile = StaticTiledMapTile(getBorderFrame(index))
        return cell
    }

    companion object {
        private val log = logger<BorderLayer>()

        const val CORNER_TL = 16
        const val CORNER_TR = 17
        const val CORNER_BR = 18
        const val CORNER_BL = 19
        const val TOP_EDGE_START = 21
        const val TOP_EDGE_END = 24
        const val RIGHT_EDGE_START = 25
        const val RIGHT_EDGE_END = 28
        const val BOTTOM_EDGE_START = 29
        const val BOTTOM_EDGE_END = 32
        const val LEFT_EDGE_START = 33
        const val LEFT_EDGE_END = 36
        const val INTERIOR_START = 0
        const val INTERIOR_END = 16
    }
}
