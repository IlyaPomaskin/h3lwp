package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import ktx.collections.gdxArrayOf
import ktx.log.logger
import java.util.Locale

class SpriteRegistry : Disposable {
    private val regions = mutableMapOf<String, Array<AtlasRegion>>()
    private val textures = mutableListOf<Texture>()
    private val loadedDefNames = mutableSetOf<String>()

    fun getTerrainFrames(defName: String, index: Int): Array<AtlasRegion> {
        val key = "$defName/$index"
        return regions[key] ?: gdxArrayOf()
    }

    fun getObjectFrames(defName: String): Array<AtlasRegion> {
        val key = defName.lowercase(Locale.ROOT).removeSuffix(".def")
        return regions[key] ?: gdxArrayOf()
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

            regions.getOrPut(info.regionName) { Array() }.add(region)
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

    override fun dispose() {
        textures.forEach { it.dispose() }
        textures.clear()
        regions.clear()
    }

    companion object {
        private val log = logger<SpriteRegistry>()

        fun fromPacker(
            packer: PixmapPacker,
            regionInfos: List<RegionInfo>
        ): SpriteRegistry {
            val registry = SpriteRegistry()

            // Create textures from packer pages
            val pageTextures = mutableListOf<Texture>()
            for (page in packer.pages) {
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

                registry.regions.getOrPut(info.regionName) { Array() }.add(region)
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

            return registry
        }
    }
}
