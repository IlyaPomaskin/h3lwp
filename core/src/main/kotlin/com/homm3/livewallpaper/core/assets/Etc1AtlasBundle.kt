package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.graphics.glutils.ETC1.ETC1Data

/** One logical atlas page after ETC1 conversion. */
data class Etc1PageData(val color: ETC1Data, val alpha: ETC1Data)

data class PackedRect(val pageIndex: Int, val x: Int, val y: Int, val w: Int, val h: Int)

/**
 * Output of the CPU phase: ETC1 page payloads + region descriptors + packer
 * rectangles keyed by packerName (carries page index, sub-rect within the page).
 */
data class Etc1Bundle(
    val pages: List<Etc1PageData>,
    val regionInfos: List<RegionInfo>,
    val packerRects: Map<String, PackedRect>,
)
