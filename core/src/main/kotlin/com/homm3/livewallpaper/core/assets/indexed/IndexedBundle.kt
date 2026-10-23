package com.homm3.livewallpaper.core.assets.indexed

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.utils.Disposable

/**
 * One sprite region: packer rectangle plus placement inside the full DEF frame, and [paletteIndex]
 * into [IndexedBundle.palettes].
 * Naming rules (consumers depend on them):
 *  - objects: regionName = def name lowercase without ".def", regionIndex = frame position in its group
 *  - terrain: regionName = "<def without .def>/<tile index>", regionIndex = 0 (no rotation copies any more)
 *  - packerName = "<def name>/<frame name>"; the def name before "/" marks the def as loaded
 */
data class IndexedRegionInfo(
    val packerName: String,
    val regionName: String,
    val regionIndex: Int,
    val width: Int,
    val height: Int,
    val fullWidth: Int,
    val fullHeight: Int,
    val x: Int,
    val y: Int,
    val isTerrain: Boolean,
    val paletteIndex: Int,
)

/** Position of a packed sprite: page and rectangle inside it. */
data class PackedRect(val pageIndex: Int, val x: Int, val y: Int, val w: Int, val h: Int)

/** Output of IndexedSpriteLoader. [pages] are Pixmap.Format.Alpha, each byte = palette index. Owns the pixmaps. */
class IndexedBundle(
    val pages: List<Pixmap>,
    val regions: List<IndexedRegionInfo>,
    val packerRects: Map<String, PackedRect>,
    val palettes: List<PaletteData>,
) : Disposable {
    override fun dispose() = pages.forEach { it.dispose() }
}
