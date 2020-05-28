package com.homm3.livewallpaper.core

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import com.badlogic.gdx.math.Rectangle
import kotlin.random.Random

class BorderLayer(private val assets: Assets, private val mapSize: Int, borderSize: Int = 10) :
    TiledMapTileLayer(
        mapSize + borderSize * 2, mapSize + borderSize * 2,
        Constants.TILE_SIZE.toInt(), Constants.TILE_SIZE.toInt()
    ) {

    private fun createBorderCell(from: Int, to: Int? = null): Cell {
        val index = if (to == null) from else Random.nextInt(from, to)
        val cell = Cell()
        cell.tile = StaticTiledMapTile(assets.getTerrainFrames("edg", index)[0])
        return cell
    }

    init {
        val mapSizeWithBorder = mapSize + borderSize * 2
        offsetX = -(borderSize * Constants.TILE_SIZE)
        offsetY = (borderSize * Constants.TILE_SIZE)

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

                setCell(x, y, borderCell)
            })
        })

        setCell(mapStart, mapStart, createBorderCell(16))
        setCell(mapEnd, mapStart, createBorderCell(17))
        setCell(mapEnd, mapEnd, createBorderCell(18))
        setCell(mapStart, mapEnd, createBorderCell(19))
    }
}