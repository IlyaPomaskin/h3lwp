package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.homm3.livewallpaper.core.assets.indexed.IndexedSpriteLoader
import com.homm3.livewallpaper.core.assets.indexed.IndexedSpriteRegistry
import com.homm3.livewallpaper.core.assets.indexed.LodSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ktx.collections.gdxArrayOf
import ktx.log.logger

/**
 * Sprites (index pages + palette texture) of every DEF the maps folder uses; one set lives as long as the map
 * library's data. Create, fill and dispose it on the GL thread.
 */
class SpriteSet(
    private val sources: List<LodSource>,
    val hasHotaAssets: Boolean,
) : Disposable {
    private val registry = IndexedSpriteRegistry()
    private val notFoundSprites = mutableSetOf<String>()
    private var isDisposed = false

    private val lazyEmptyTexture = lazy {
        val pixmap = Pixmap(EMPTY_TEXTURE_SIZE, EMPTY_TEXTURE_SIZE, Pixmap.Format.RGBA8888)
        Texture(pixmap).also { pixmap.dispose() }
    }
    private val emptyRegion: TextureAtlas.AtlasRegion by lazy {
        TextureAtlas.AtlasRegion(TextureRegion(lazyEmptyTexture.value))
    }

    /** Loads the DEFs of [defNames] (lowercase) not in this set yet: decodes on IO, uploads on the caller's (GL) thread. */
    suspend fun loadDefs(defNames: Set<String>) {
        val toLoad = defNames.filterNot { registry.isDefLoaded(it) || it in notFoundSprites }.toSet()
        if (toLoad.isEmpty()) return

        val decodeStart = System.currentTimeMillis()
        val bundle = withContext(Dispatchers.IO) { IndexedSpriteLoader().load(sources, toLoad) }
        val uploadStart = System.currentTimeMillis()
        val pageCount = bundle.pages.size
        registry.add(bundle)
        log.info {
            "Loaded ${toLoad.size} DEFs (${bundle.regions.size} regions, $pageCount pages, ${bundle.palettes.size} " +
                "palettes): decode+pack ${uploadStart - decodeStart}ms, upload ${System.currentTimeMillis() - uploadStart}ms"
        }

        val notFound = toLoad.filterNot { registry.isDefLoaded(it) }
        if (notFound.isEmpty()) return
        notFoundSprites.addAll(notFound)
        log.error { "${notFound.size} sprites not found in HotA.lod or H3sprite.lod: $notFound" }
    }

    fun getTerrainFrames(defName: String, index: Int): Array<TextureAtlas.AtlasRegion> =
        registry.getTerrainFrames(defName, index).orEmptyRegion()

    fun getObjectFrames(defName: String): Array<TextureAtlas.AtlasRegion> {
        if (defName.isEmpty()) return gdxArrayOf(emptyRegion)
        return registry.getObjectFrames(defName).orEmptyRegion()
    }

    internal fun findAtlasRegions(name: String): Array<TextureAtlas.AtlasRegion> = registry.findRegions(name)

    fun paletteTexture(): Texture = registry.paletteTexture

    fun updatePalettes(step: Long) = registry.update(step)

    private fun Array<TextureAtlas.AtlasRegion>.orEmptyRegion(): Array<TextureAtlas.AtlasRegion> {
        if (isEmpty) return gdxArrayOf(emptyRegion)
        return this
    }

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true
        registry.dispose()
        if (lazyEmptyTexture.isInitialized()) lazyEmptyTexture.value.dispose()
    }

    companion object {
        private val log = logger<SpriteSet>()
        private const val EMPTY_TEXTURE_SIZE = 1
    }
}
