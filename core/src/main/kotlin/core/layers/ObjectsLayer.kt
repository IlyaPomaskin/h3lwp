package core.layers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.maps.MapLayer
import com.homm3.livewallpaper.core.Assets
import com.homm3.livewallpaper.core.ObjectsRandomizer
import com.homm3.livewallpaper.core.Sprite
import com.homm3.livewallpaper.parser.formats.H3m

class ObjectsLayer(private val assets: Assets, h3m: H3m, isUnderground: Boolean) : MapLayer() {
    private val randomizer = ObjectsRandomizer()
    private var visibleSprites: List<Sprite> = mutableListOf()
    private var sprites = h3m
        .objects
        .sorted()
        // TODO underground rendering
        .filter { it.z == if (isUnderground) 1 else 0 }
        .map { Sprite(it, assets.getObjectFrames(randomizer.replaceRandomObject(it))) }

    init {
        name = "objects"
    }

    fun updateVisibleSprites(camera: Camera) {
        visibleSprites = sprites
            .filter { it.inViewport(camera) }

        println("visibleSprites ${visibleSprites.size}")
    }

    fun render(batch: Batch) {
        visibleSprites.forEach { it.render(batch, Gdx.graphics.deltaTime) }
    }
}