package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.PixmapPacker
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

    fun addFromPacker(packer: PixmapPacker, regionInfos: List<RegionInfo>) {
        if (regionInfos.isEmpty()) return

        data class PackedRect(val pageIndex: Int, val rect: PixmapPacker.PixmapPackerRectangle)

        val pageTextures = mutableListOf<Texture>()
        for (page in packer.pages) {
            val texture = Texture(page.pixmap)
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            pageTextures.add(texture)
            textures.add(texture)
        }

        val packerRects = mutableMapOf<String, PackedRect>()
        packer.pages.forEachIndexed { pageIndex, page ->
            page.rects.forEach { entry ->
                packerRects[entry.key] = PackedRect(pageIndex, entry.value)
            }
        }

        // Snapshot existing keys so we skip sprites from previous loads
        // but still add all frames of new sprites in this batch
        val existingRegionNames = regions.keys.toSet()
        val newRegionNames = mutableSetOf<String>()
        for (info in regionInfos) {
            if (info.regionName in existingRegionNames) continue

            val packed = packerRects[info.packerName] ?: continue
            val texture = pageTextures[packed.pageIndex]

            val region = AtlasRegion(
                texture,
                packed.rect.x.toInt(), packed.rect.y.toInt(),
                info.width, info.height
            )
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
            val existingWithSameIndex = (0 until arr.size).any { arr.get(it).index == info.regionIndex }
            if (existingWithSameIndex) {
                log.info { "addFromPacker duplicate: ${info.regionName}[${info.regionIndex}] from packer=${info.packerName} (already have ${arr.size} frames)" }
            }
            arr.add(region)
            newRegionNames.add(info.regionName)
            loadedDefNames.add(info.packerName.substringBefore("/"))
        }

        // Sort newly added region arrays
        for (name in newRegionNames) {
            val arr = regions[name] ?: continue
            if (arr.size > 1) {
                arr.sort { a, b -> a.index.compareTo(b.index) }
            }
        }

        // Handle edg border frames if newly added
        if ("edg" in newRegionNames) {
            val edgRegions = regions["edg"]
            if (edgRegions != null) {
                for (i in 0 until edgRegions.size) {
                    val region = edgRegions.get(i)
                    regions["edg/${region.index}"] = gdxArrayOf(region)
                }
            }
        }

        log.info { "Added ${newRegionNames.size} new sprite regions from ${regionInfos.map { it.packerName.substringBefore("/") }.toSet().size} DEFs" }
    }

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

        fun fromPacker(
            packer: PixmapPacker,
            regionInfos: List<RegionInfo>
        ): SpriteRegistry {
            val registry = SpriteRegistry()

            // Create textures from packer pages
            val pageTextures = mutableListOf<Texture>()
            for ((pageIndex, page) in packer.pages.withIndex()) {
                val texture = Texture(page.pixmap)
                texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
                pageTextures.add(texture)
                registry.textures.add(texture)
            }

            // Build a lookup: packerName → (pageIndex, rect)
            data class PackedRect(
                val pageIndex: Int,
                val rect: PixmapPacker.PixmapPackerRectangle
            )

            val packerRects = mutableMapOf<String, PackedRect>()
            packer.pages.forEachIndexed { pageIndex, page ->
                page.rects.forEach { entry ->
                    packerRects[entry.key] = PackedRect(pageIndex, entry.value)
                }
            }

            // Create AtlasRegions from RegionInfos
            // Match the old TextureAtlas(file, flip=true) pipeline exactly:
            // 1. Set ALL properties first (raw offset values)
            // 2. THEN call flip(false, true) which both swaps UV AND transforms offsetY
            // AtlasRegion.flip() overrides TextureRegion.flip() and computes:
            //   offsetY = originalHeight - offsetY - packedHeight
            for (info in regionInfos) {
                val packed = packerRects[info.packerName] ?: continue
                val texture = pageTextures[packed.pageIndex]

                val region = AtlasRegion(
                    texture,
                    packed.rect.x.toInt(), packed.rect.y.toInt(),
                    info.width, info.height
                )
                region.name = info.regionName
                region.index = info.regionIndex
                region.offsetX = info.x.toFloat()
                region.offsetY = info.y.toFloat()
                region.originalWidth = info.fullWidth
                region.originalHeight = info.fullHeight
                region.packedWidth = info.width
                region.packedHeight = info.height
                // flip AFTER all properties are set, so offsetY transform uses correct values
                region.flip(false, true)

                log.debug { "  region: ${info.regionName}[${info.regionIndex}] offsetX=${region.offsetX} offsetY=${region.offsetY} origW=${region.originalWidth} origH=${region.originalHeight} packedW=${region.packedWidth} packedH=${region.packedHeight} regW=${region.regionWidth} regH=${region.regionHeight}" }

                val arr = registry.regions.getOrPut(info.regionName) { Array() }
                val existingWithSameIndex = (0 until arr.size).any { arr.get(it).index == info.regionIndex }
                if (existingWithSameIndex) {
                    log.info { "Duplicate region: ${info.regionName}[${info.regionIndex}] from packer=${info.packerName} (already have ${arr.size} frames)" }
                }
                arr.add(region)
                registry.loadedDefNames.add(info.packerName.substringBefore("/"))
            }

            // Sort each region array by index so animations play in order
            for ((_, arr) in registry.regions) {
                if (arr.size > 1) {
                    arr.sort { a, b -> a.index.compareTo(b.index) }
                }
            }

            // Border frames: edg.def is loaded as object frames with regionName="edg"
            // and regionIndex=position in group. BorderLayer looks them up as "edg/$index".
            val edgRegions = registry.regions["edg"]
            if (edgRegions != null) {
                for (i in 0 until edgRegions.size) {
                    val region = edgRegions.get(i)
                    registry.regions["edg/${region.index}"] = gdxArrayOf(region)
                }
            }

            log.info { "Registry: ${registry.regions.size} sprite regions from ${registry.loadedDefNames.size} DEFs" }
            return registry
        }
    }
}
