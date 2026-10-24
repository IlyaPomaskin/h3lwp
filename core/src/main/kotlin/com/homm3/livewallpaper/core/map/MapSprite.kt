package com.homm3.livewallpaper.core.map

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer
import com.badlogic.gdx.utils.Array
import com.homm3.livewallpaper.core.GameConfig.TILE_SIZE
import com.homm3.livewallpaper.core.assets.indexed.PaletteColor
import com.homm3.livewallpaper.core.assets.indexed.PaletteRegion
import com.homm3.livewallpaper.core.render.AnimationClock
import com.homm3.livewallpaper.parser.h3m.H3mObject
import kotlin.random.Random

/** [shiftTiles] moves the sprite right of the object's anchor tile, e.g. [MonsterAnchor.shiftTiles]. */
class MapSprite(mapObject: H3mObject, private val frames: Array<TextureAtlas.AtlasRegion>, shiftTiles: Int = 0) {
    private val x = (mapObject.x + 1 + shiftTiles) * TILE_SIZE
    private val y = (mapObject.y + 1) * TILE_SIZE
    private val framePhase = Random.nextInt(frames.size)

    fun render(renderer: BatchTiledMapRenderer) {
        val frameIndex = ((AnimationClock.frameStep + framePhase) % frames.size).toInt()
        val frame = frames.get(frameIndex)
        renderer.batch.packedColor = PaletteColor.encode((frame as? PaletteRegion)?.paletteSlot ?: 0)
        renderer.batch.draw(frame, getFrameX(frame), getFrameY(frame))
    }

    fun worldBounds(): com.badlogic.gdx.math.Rectangle? {
        if (frames.isEmpty) return null
        val frame = frames.first()
        val fx = getFrameX(frame)
        val fy = getFrameY(frame)
        return com.badlogic.gdx.math.Rectangle(
            fx, fy, frame.packedWidth.toFloat(), frame.packedHeight.toFloat()
        )
    }

    private fun getFrameX(frame: TextureAtlas.AtlasRegion): Float {
        return x + frame.offsetX - frame.originalWidth
    }

    private fun getFrameY(frame: TextureAtlas.AtlasRegion): Float {
        return y - frame.offsetY - frame.packedHeight
    }
}
