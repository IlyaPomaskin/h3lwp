package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.graphics.glutils.ETC1.ETC1Data

/** One logical atlas page after ETC1 conversion. */
data class Etc1PageData(val color: ETC1Data, val alpha: ETC1Data)

/**
 * Output of the CPU phase: ETC1 page payloads + region descriptors + map
 * from packerName → page index (preserves what PixmapPacker assigned).
 */
data class Etc1Bundle(
    val pages: List<Etc1PageData>,
    val regionInfos: List<RegionInfo>,
    val pageOfPackerName: Map<String, Int>,
)
