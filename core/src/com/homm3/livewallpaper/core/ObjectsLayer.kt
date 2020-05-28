package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.MapObject
import com.badlogic.gdx.utils.Disposable
import com.homm3.livewallpaper.parser.formats.H3m
import com.homm3.livewallpaper.parser.formats.H3mObjects
import ktx.collections.gdxArrayOf
import ktx.graphics.use

class ObjectsLayer(private val assets: Assets, private val camera: Camera, h3mMap: H3m) : MapLayer() {
    private val randomizer = ObjectsRandomizer()
    private var prevCameraPos = Pair(Float.NaN, Float.NaN)
    private var visibleSprites: List<Sprite> = mutableListOf()
    private var sprites: List<Sprite> = h3mMap
        .objects
        .sorted()
        // TODO underground rendering
        .filter { it.z == 0 }
        .map { Sprite(it, assets.getObjectFrames(randomizer.replaceRandomObject(it))) }

    fun render(batch: Batch) {
        if (prevCameraPos.first != camera.position.x || prevCameraPos.second != camera.position.y) {
            visibleSprites = sprites.filter { it.inViewport(camera) }
            prevCameraPos = Pair(camera.position.x, camera.position.y)
        }

        visibleSprites.forEach { it.render(batch, Gdx.graphics.deltaTime) }
    }
}