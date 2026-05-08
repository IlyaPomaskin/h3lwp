package com.homm3.livewallpaper.core.map

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer
import com.badlogic.gdx.utils.Array
import com.homm3.livewallpaper.core.GameConfig.FRAME_TIME
import com.homm3.livewallpaper.core.GameConfig.TILE_SIZE
import com.homm3.livewallpaper.parser.h3m.H3mObject
import kotlin.random.Random

class MapSprite(mapObject: H3mObject, private val frames: Array<TextureAtlas.AtlasRegion>) {
    val defName: String = mapObject.def.spriteName
    val mapX: Int = mapObject.x
    val mapY: Int = mapObject.y
    val mapZ: Int = mapObject.z
    val objectType: String = mapObject.objectType.name
    private val x = (mapObject.x + 1) * TILE_SIZE
    private val y = (mapObject.y + 1) * TILE_SIZE
    private val animationLength = frames.size * FRAME_TIME
    private var stateTime = animationLength * Random.nextFloat()

    fun render(renderer: BatchTiledMapRenderer, delta: Float) {
        stateTime = (stateTime % animationLength) + delta
        val frameIndex = ((stateTime / FRAME_TIME) % frames.size).toInt()
        val frame = frames.get(frameIndex)
        renderer.batch.draw(frame, getFrameX(frame), getFrameY(frame))
    }

    fun contains(worldX: Float, worldY: Float): Boolean {
        if (frames.isEmpty) return false
        val frame = frames.first()
        val fx = getFrameX(frame)
        val fy = getFrameY(frame)
        return worldX >= fx && worldX <= fx + frame.originalWidth &&
                worldY >= fy && worldY <= fy + frame.originalHeight
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

    fun frameNames(): List<String> {
        return (0 until frames.size).map { "${frames.get(it).name}/${frames.get(it).index}" }
    }

    fun debugInfo(): String {
        if (frames.isEmpty) return "no frames"
        val f = frames.first()
        return "drawPos=(${getFrameX(f)}, ${getFrameY(f)}) packedWH=${f.packedWidth}x${f.packedHeight} " +
            "origWH=${f.originalWidth}x${f.originalHeight} offset=(${f.offsetX}, ${f.offsetY}) " +
            "texRegion=(${f.regionX},${f.regionY} ${f.regionWidth}x${f.regionHeight}) " +
            "texU=${f.u}-${f.u2} texV=${f.v}-${f.v2}"
    }

    private fun getFrameX(frame: TextureAtlas.AtlasRegion): Float {
        return x + frame.offsetX - frame.originalWidth
    }

    private fun getFrameY(frame: TextureAtlas.AtlasRegion): Float {
        return y - frame.offsetY - frame.packedHeight
    }
}
