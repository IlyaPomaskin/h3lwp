package com.homm3.livewallpaper.core.map.layers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer
import com.badlogic.gdx.math.Rectangle
import com.homm3.livewallpaper.core.GameConfig.FRUSTUM_PADDING_TILES
import com.homm3.livewallpaper.core.GameConfig.TILE_SIZE
import com.homm3.livewallpaper.core.assets.GameAssets
import com.homm3.livewallpaper.core.map.MapSprite
import com.homm3.livewallpaper.core.map.ObjectsRandomizer
import com.homm3.livewallpaper.parser.h3m.H3mMap

class ObjectsLayer(assets: GameAssets, h3m: H3mMap, isUnderground: Boolean) : MapLayer() {
    private val randomizer = ObjectsRandomizer(hasHotaAssets = assets.isHotaAvailable())
    private val sprites = h3m
        .objects
        .sorted()
        .filter { it.z == if (isUnderground) 1 else 0 }
        .map { MapSprite(it, assets.getObjectFrames(randomizer.resolveSpriteName(it))) }

    private val spritesIndices: Map<Int, Map<Int, List<Int>>> = buildSpatialIndex(h3m, isUnderground)

    private var lastStartX = Int.MIN_VALUE
    private var lastStartY = Int.MIN_VALUE
    private var lastEndX = Int.MIN_VALUE
    private var lastEndY = Int.MIN_VALUE
    private val visibleSprites = mutableListOf<MapSprite>()

    init {
        name = "objects"
    }

    fun render(renderer: BatchTiledMapRenderer) {
        updateVisibleObjects(renderer.viewBounds)
        visibleSprites.forEach { it.render(renderer, Gdx.graphics.deltaTime) }
    }

    fun findSpriteAt(worldX: Float, worldY: Float): MapSprite? {
        return visibleSprites.lastOrNull { it.contains(worldX, worldY) }
    }

    fun findAllSpritesAt(worldX: Float, worldY: Float): List<MapSprite> {
        return visibleSprites.filter { it.contains(worldX, worldY) }
    }

    fun objectCoverage(viewBounds: Rectangle): Float {
        updateVisibleObjects(viewBounds)

        val tileSize = TILE_SIZE.toInt()
        val cols = (viewBounds.width / tileSize).toInt() + 1
        val rows = (viewBounds.height / tileSize).toInt() + 1
        if (cols <= 0 || rows <= 0) return 0f
        val grid = Array(cols) { BooleanArray(rows) }

        for (sprite in visibleSprites) {
            val rect = sprite.worldBounds() ?: continue
            val rx0 = maxOf(rect.x, viewBounds.x)
            val ry0 = maxOf(rect.y, viewBounds.y)
            val rx1 = minOf(rect.x + rect.width, viewBounds.x + viewBounds.width)
            val ry1 = minOf(rect.y + rect.height, viewBounds.y + viewBounds.height)
            if (rx0 >= rx1 || ry0 >= ry1) continue
            val gx0 = ((rx0 - viewBounds.x) / tileSize).toInt().coerceIn(0, cols - 1)
            val gy0 = ((ry0 - viewBounds.y) / tileSize).toInt().coerceIn(0, rows - 1)
            val gx1 = ((rx1 - viewBounds.x) / tileSize).toInt().coerceIn(0, cols - 1)
            val gy1 = ((ry1 - viewBounds.y) / tileSize).toInt().coerceIn(0, rows - 1)
            for (gx in gx0..gx1) for (gy in gy0..gy1) grid[gx][gy] = true
        }

        var filled = 0
        for (col in grid) for (b in col) if (b) filled++
        return filled.toFloat() / (cols * rows)
    }

    private fun updateVisibleObjects(viewBounds: Rectangle) {
        val startY = (viewBounds.y / TILE_SIZE).toInt() - FRUSTUM_PADDING_TILES
        val endY = ((viewBounds.y + viewBounds.height) / TILE_SIZE).toInt() + FRUSTUM_PADDING_TILES
        val startX = (viewBounds.x / TILE_SIZE).toInt() - FRUSTUM_PADDING_TILES
        val endX = ((viewBounds.x + viewBounds.width) / TILE_SIZE).toInt() + FRUSTUM_PADDING_TILES

        if (startX == lastStartX && startY == lastStartY && endX == lastEndX && endY == lastEndY) {
            return
        }

        visibleSprites.clear()
        val visibleIndices = mutableListOf<Int>()
        for (y in startY..endY) {
            val column = spritesIndices[y] ?: continue
            for (x in startX..endX) {
                val indices = column[x] ?: continue
                visibleIndices.addAll(indices)
            }
        }
        visibleIndices.sort()
        visibleIndices.forEach { id -> visibleSprites.add(sprites[id]) }

        lastStartX = startX
        lastStartY = startY
        lastEndX = endX
        lastEndY = endY
    }

    private fun buildSpatialIndex(h3m: H3mMap, isUnderground: Boolean): Map<Int, Map<Int, List<Int>>> {
        val index = mutableMapOf<Int, MutableMap<Int, MutableList<Int>>>()
        h3m.objects
            .sorted()
            .filter { it.z == if (isUnderground) 1 else 0 }
            .forEachIndexed { i, obj ->
                index
                    .getOrPut(obj.y) { mutableMapOf() }
                    .getOrPut(obj.x) { mutableListOf() }
                    .add(i)
            }
        return index
    }
}
