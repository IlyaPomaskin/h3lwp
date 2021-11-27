package com.homm3.livewallpaper.core

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import com.badlogic.gdx.math.Rectangle
import kotlin.random.Random

class BorderLayer(
    private val manager: AssetManager,
    private val mapSize: Int,
    borderWidth: Int,
    borderHeight: Int
) : TiledMapTileLayer(
    mapSize + borderWidth * 2,
    mapSize + borderHeight * 2,
    Constants.TILE_SIZE.toInt(),
    Constants.TILE_SIZE.toInt()
) {
    private fun getBorderFrame(index: Int): TextureAtlas.AtlasRegion {
         manager
            .get<TextureAtlas>(Constants.Assets.ATLAS_PATH)
            .findRegions("edg/$index")
            .run {
                return if (isEmpty) {
                    println("Can't find def edg/$index")
                    Constants.Assets.emptyPixmap
                } else {
                    this.first()
                }
            }
    }

    private fun createBorderCell(from: Int, to: Int? = null): Cell {
        val index = if (to == null) from else Random.nextInt(from, to)
        val cell = Cell()
        cell.tile = StaticTiledMapTile(getBorderFrame(index))
        return cell
    }

    init {
        offsetX = -(borderWidth * Constants.TILE_SIZE)
        offsetY = (borderHeight * Constants.TILE_SIZE)

        val xStart = borderWidth - 1
        val xEnd = borderWidth + mapSize
        val yStart = borderHeight - 1
        val yEnd = borderHeight + mapSize
        val mapRect = Rectangle(borderWidth.toFloat(), borderHeight.toFloat(), mapSize - 1f, mapSize - 1f)
        (0..mapSize + borderWidth * 2).forEach(fun(x) {
            (0..mapSize + borderHeight * 2).forEach(fun(y) {
                if (mapRect.contains(x.toFloat(), y.toFloat())) {
                    return
                }

                val borderCell = when {
                    x in xStart until xEnd && y == yStart -> createBorderCell(21, 24)
                    y in yStart until yEnd && x == xEnd -> createBorderCell(25, 28)
                    x in xStart until xEnd && y == yEnd -> createBorderCell(29, 32)
                    y in yStart until yEnd && x == xStart -> createBorderCell(33, 36)
                    else -> createBorderCell(0, 16)
                }

                setCell(x, y, borderCell)
            })
        })

        setCell(xStart, yStart, createBorderCell(16))
        setCell(xEnd, yStart, createBorderCell(17))
        setCell(xEnd, yEnd, createBorderCell(18))
        setCell(xStart, yEnd, createBorderCell(19))

        name = "border"
    }
}