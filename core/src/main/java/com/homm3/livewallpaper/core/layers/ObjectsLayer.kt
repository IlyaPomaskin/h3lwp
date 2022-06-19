package com.homm3.livewallpaper.core.layers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer
import com.badlogic.gdx.math.Rectangle
import com.homm3.livewallpaper.core.Assets
import com.homm3.livewallpaper.core.Constants.Companion.TILE_SIZE
import com.homm3.livewallpaper.core.Constants.Companion.VISIBLE_TILES_OFFSET
import com.homm3.livewallpaper.core.ObjectsRandomizer
import com.homm3.livewallpaper.core.Sprite
import com.homm3.livewallpaper.parser.formats.H3m

// TODO underground rendering

class ObjectsLayer(private val assets: Assets, h3m: H3m, isUnderground: Boolean) : MapLayer() {
    private val randomizer = ObjectsRandomizer()
    private var sprites = h3m
        .objects
        .sorted()
        // TODO underground rendering
        .filter { it.z == if (isUnderground) 1 else 0 }
        .map { Sprite(it, assets.getObjectFrames(randomizer.replaceRandomObject(it))) }

    private var spritesIndices: MutableMap<Int, MutableMap<Int, MutableList<Int>>> =
        mutableMapOf()

    init {
        name = "objects"

        h3m
            .objects
            .sorted()
            .filter { it.z == if (isUnderground) 1 else 0 }
            .forEachIndexed { index, it ->
                if (spritesIndices[it.y] == null) {
                    spritesIndices[it.y] = mutableMapOf()
                }

                if (spritesIndices[it.y]?.get(it.x) == null) {
                    spritesIndices[it.y]?.put(it.x, ArrayList())
                }

                spritesIndices[it.y]?.get(it.x)?.add(index)
            }
    }

    private var lastViewPort = ""
    private val visibleIndices = mutableListOf<Int>()
    private val visibleSprites = mutableListOf<Sprite>()

    private fun updateVisibleObjects(viewBounds: Rectangle) {
        if (lastViewPort == viewBounds.toString()) {
            return
        }

        val startY = (viewBounds.y / TILE_SIZE).toInt() - VISIBLE_TILES_OFFSET
        val endY = ((viewBounds.y + viewBounds.height) / TILE_SIZE).toInt() + VISIBLE_TILES_OFFSET
        val startX = (viewBounds.x / TILE_SIZE).toInt() - VISIBLE_TILES_OFFSET
        val endX = ((viewBounds.x + viewBounds.width) / TILE_SIZE).toInt() + VISIBLE_TILES_OFFSET

        visibleSprites.clear()
        visibleIndices.clear()
        for (y in startY..endY) {
            val column = this.spritesIndices[y] ?: continue

            for (x in startX..endX) {
                val indices = column[x] ?: continue

                visibleIndices.addAll(indices)
            }
        }
        visibleIndices.sort()
        visibleIndices.forEach { id -> visibleSprites.add(sprites[id]) }

        lastViewPort = viewBounds.toString()
    }

    fun render(batch: BatchTiledMapRenderer) {
        updateVisibleObjects(batch.viewBounds)

        visibleSprites.forEach { it.render(batch, Gdx.graphics.deltaTime) }
    }
}