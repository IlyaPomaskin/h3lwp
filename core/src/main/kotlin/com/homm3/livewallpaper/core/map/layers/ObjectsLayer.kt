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
    private val randomizer = ObjectsRandomizer()
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
