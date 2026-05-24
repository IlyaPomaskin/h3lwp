package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.glutils.ETC1TextureData
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import ktx.collections.gdxArrayOf
import ktx.log.logger
import java.util.Locale

class SpriteRegistry : Disposable {
    private val regions = mutableMapOf<String, Array<AtlasRegion>>()
    private val textures = mutableListOf<Texture>()
    private val loadedDefNames = mutableSetOf<String>()
    private val colorToAlpha = mutableMapOf<Texture, Texture>()
    private val alphaTextures = mutableListOf<Texture>()

    /** Returns companion alpha texture for color, or null if none registered (legacy path). */
    fun companionFor(color: Texture): Texture? = colorToAlpha[color]

    fun getTerrainFrames(defName: String, index: Int): Array<AtlasRegion> {
        val key = "$defName/$index"
        return regions[key] ?: gdxArrayOf()
    }

    fun getObjectFrames(defName: String): Array<AtlasRegion> {
        val key = defName.lowercase(Locale.ROOT).removeSuffix(".def")
        val frames = regions[key] ?: gdxArrayOf()
        if (frames.size > 1) {
            val indices = (0 until frames.size).map { frames.get(it).index }
            val duplicateIndices = indices.groupBy { it }.filter { it.value.size > 1 }.keys
            if (duplicateIndices.isNotEmpty()) {
                log.info { "DEF $defName ($key): ${frames.size} frames with duplicate indices $duplicateIndices (all indices: $indices)" }
            }
        }
        return frames
    }

    fun findRegions(name: String): Array<AtlasRegion> {
        return regions[name] ?: gdxArrayOf()
    }

    fun isDefLoaded(defName: String): Boolean = defName in loadedDefNames

    fun addFromEtc1(bundle: Etc1Bundle) {
        if (bundle.regionInfos.isEmpty()) return

        val colorTextures = bundle.pages.map { page ->
            Texture(ETC1TextureData(page.color, false)).also { tex ->
                tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
                textures.add(tex)
            }
        }
        val alphaTexs = bundle.pages.map { page ->
            Texture(ETC1TextureData(page.alpha, false)).also { tex ->
                tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
                alphaTextures.add(tex)
            }
        }
        colorTextures.zip(alphaTexs).forEach { (c, a) -> colorToAlpha[c] = a }

        val existingRegionNames = regions.keys.toSet()
        val newRegionNames = mutableSetOf<String>()
        for (info in bundle.regionInfos) {
            if (info.regionName in existingRegionNames) continue
            val rect = bundle.packerRects[info.packerName] ?: continue
            val texture = colorTextures[rect.pageIndex]

            val region = AtlasRegion(texture, rect.x, rect.y, info.width, info.height)
            region.name = info.regionName
            region.index = info.regionIndex
            region.offsetX = info.x.toFloat()
            region.offsetY = info.y.toFloat()
            region.originalWidth = info.fullWidth
            region.originalHeight = info.fullHeight
            region.packedWidth = info.width
            region.packedHeight = info.height
            region.flip(false, true)

            val arr = regions.getOrPut(info.regionName) { Array() }
            arr.add(region)
            newRegionNames.add(info.regionName)
            loadedDefNames.add(info.packerName.substringBefore("/"))
        }

        for (name in newRegionNames) {
            regions[name]?.let { if (it.size > 1) it.sort { a, b -> a.index.compareTo(b.index) } }
        }

        if ("edg" in newRegionNames) {
            regions["edg"]?.let { edgs ->
                for (i in 0 until edgs.size) regions["edg/${edgs.get(i).index}"] = gdxArrayOf(edgs.get(i))
            }
        }

        log.info { "Added ${newRegionNames.size} new sprite regions from ETC1 bundle" }
    }

    override fun dispose() {
        textures.forEach { it.dispose() }
        alphaTextures.forEach { it.dispose() }
        textures.clear()
        alphaTextures.clear()
        colorToAlpha.clear()
        regions.clear()
    }

    companion object {
        private val log = logger<SpriteRegistry>()

        fun fromEtc1(bundle: Etc1Bundle): SpriteRegistry {
            val r = SpriteRegistry()
            r.addFromEtc1(bundle)
            return r
        }
    }
}
