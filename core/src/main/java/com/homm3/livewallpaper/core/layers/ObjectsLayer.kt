package com.homm3.livewallpaper.core.layers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer
import com.homm3.livewallpaper.core.Assets
import com.homm3.livewallpaper.core.Constants.Companion.TILE_SIZE
import com.homm3.livewallpaper.core.Constants.Companion.VISIBLE_TILES_OFFSET
import com.homm3.livewallpaper.core.ObjectsRandomizer
import com.homm3.livewallpaper.core.Sprite
import com.homm3.livewallpaper.parser.formats.H3m
import kotlin.math.nextDown
import kotlin.math.nextUp

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

    fun render(batch: BatchTiledMapRenderer) {
        val startY = (batch.viewBounds.y / TILE_SIZE).nextDown()
            .toInt() - VISIBLE_TILES_OFFSET
        val endY = ((batch.viewBounds.y + batch.viewBounds.height) / TILE_SIZE).nextUp()
            .toInt() + VISIBLE_TILES_OFFSET
        val startX = (batch.viewBounds.x / TILE_SIZE).nextDown()
            .toInt() - VISIBLE_TILES_OFFSET
        val endX = ((batch.viewBounds.x + batch.viewBounds.width) / TILE_SIZE).nextUp()
            .toInt() + VISIBLE_TILES_OFFSET

        val visibleIndices = mutableListOf<Int>()
        for (y in startY..endY) {
            val column = this.spritesIndices[y] ?: continue

            for (x in startX..endX) {
                val indices = column[x] ?: continue

                visibleIndices.addAll(indices)
            }
        }
        visibleIndices.sort()
        visibleIndices.forEach { id ->
            sprites[id].render(batch, Gdx.graphics.deltaTime)
        }
    }
}