package com.homm3.livewallpaper.core.assets.indexed

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import ktx.collections.gdxArrayOf
import ktx.log.logger
import java.util.Locale

class IndexedSpriteRegistry : Disposable {
    private val paletteAtlas = PaletteAtlas()
    private val regions = mutableMapOf<String, Array<AtlasRegion>>()
    private val pageTextures = mutableListOf<Texture>()
    private val loadedDefNames = mutableSetOf<String>()

    val paletteTexture: Texture get() = paletteAtlas.texture

    fun getTerrainFrames(defName: String, index: Int): Array<AtlasRegion> =
        regions["$defName/$index"] ?: gdxArrayOf()

    fun getObjectFrames(defName: String): Array<AtlasRegion> {
        val key = defName.lowercase(Locale.ROOT).removeSuffix(DEF_EXTENSION)
        return regions[key] ?: gdxArrayOf()
    }

    fun findRegions(name: String): Array<AtlasRegion> = regions[name] ?: gdxArrayOf()

    fun isDefLoaded(defName: String): Boolean = defName in loadedDefNames

    fun update(delta: Float) = paletteAtlas.update(delta)

    fun add(bundle: IndexedBundle) {
        try {
            addRegions(bundle)
        } finally {
            bundle.dispose()
        }
    }

    private fun addRegions(bundle: IndexedBundle) {
        val existingRegionNames = regions.keys.toSet()
        val newRegions = bundle.regions.mapNotNull { info ->
            if (info.regionName in existingRegionNames) return@mapNotNull null
            bundle.packerRects[info.packerName]?.let { rect -> info to rect }
        }
        if (newRegions.isEmpty()) return

        val textureByPage = newRegions.map { (_, rect) -> rect.pageIndex }.distinct()
            .associateWith { pageIndex -> uploadPage(bundle, pageIndex) }
        val slotByPalette = newRegions.map { (info, _) -> info.paletteIndex }.distinct()
            .associateWith { paletteIndex -> paletteAtlas.add(paletteOf(bundle, paletteIndex)) }

        newRegions.forEach { (info, rect) ->
            val region = PaletteRegion(
                texture = textureByPage.getValue(rect.pageIndex),
                x = rect.x,
                y = rect.y,
                width = info.width,
                height = info.height,
                paletteSlot = slotByPalette.getValue(info.paletteIndex),
            ).apply {
                name = info.regionName
                index = info.regionIndex
                offsetX = info.x.toFloat()
                offsetY = info.y.toFloat()
                originalWidth = info.fullWidth
                originalHeight = info.fullHeight
                packedWidth = info.width
                packedHeight = info.height
                flip(false, true)
            }
            regions.getOrPut(info.regionName) { Array() }.add(region)
            loadedDefNames.add(info.packerName.substringBefore("/"))
        }

        val newRegionNames = newRegions.map { (info, _) -> info.regionName }.toSet()
        newRegionNames.forEach { name -> regions[name]?.sort { a, b -> a.index.compareTo(b.index) } }
        if (EDGE_DEF in newRegionNames) registerEdgeFrames()

        log.info {
            "Added ${newRegionNames.size} indexed regions, ${textureByPage.size} pages, ${slotByPalette.size} palettes"
        }
    }

    private fun uploadPage(bundle: IndexedBundle, pageIndex: Int): Texture {
        val page = bundle.pages.getOrNull(pageIndex)
        requireNotNull(page) { "page $pageIndex missing, bundle has ${bundle.pages.size}" }
        return Texture(IndexPageTextureData.of(page)).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            pageTextures.add(this)
        }
    }

    private fun paletteOf(bundle: IndexedBundle, paletteIndex: Int): PaletteData {
        val palette = bundle.palettes.getOrNull(paletteIndex)
        return requireNotNull(palette) { "palette $paletteIndex missing, bundle has ${bundle.palettes.size}" }
    }

    private fun registerEdgeFrames() {
        val edges = regions[EDGE_DEF] ?: return
        edges.forEach { edge -> regions["$EDGE_DEF/${edge.index}"] = gdxArrayOf(edge) }
    }

    override fun dispose() {
        pageTextures.forEach { it.dispose() }
        pageTextures.clear()
        paletteAtlas.dispose()
        regions.clear()
        loadedDefNames.clear()
    }

    private companion object {
        const val DEF_EXTENSION = ".def"
        const val EDGE_DEF = "edg"
        val log = logger<IndexedSpriteRegistry>()
    }
}
