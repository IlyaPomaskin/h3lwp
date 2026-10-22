package com.homm3.livewallpaper.core.assets.indexed

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.utils.Disposable
import com.homm3.livewallpaper.core.assets.PackedRect
import com.homm3.livewallpaper.parser.lod.LodDataSource

/*
 * Shared contract for the indexed-palette sprite pipeline.
 *
 * CPU side  : IndexedSpriteLoader  -> IndexedBundle   (8-bit index pages + palettes, no GL)
 * GPU side  : IndexedSpriteRegistry.add(bundle)       (uploads pages as GL_ALPHA textures, palettes into PaletteAtlas)
 * Rendering : PaletteShader + PaletteSpriteBatch + PaletteTiledMapRenderer
 *             fragment colour = palette[row][index]; row comes from the vertex colour (PaletteColor).
 */

const val PALETTE_SIZE = 256
const val PALETTE_ENTRY_BYTES = 4
const val PALETTE_BYTES = PALETTE_SIZE * PALETTE_ENTRY_BYTES

/** Palette texture is PALETTE_SIZE x MAX_PALETTE_ROWS RGBA8888; row = one palette. */
const val MAX_PALETTE_ROWS = 2048

/**
 * One 256-colour palette, RGBA8888 (1024 bytes), special indices already applied
 * (index 0 transparent, shadows as black with alpha, player colour neutralised).
 *
 * [rotations]: palette-cycling ranges as in LodSpriteLoader.paletteRotations, e.g. 229..241 means
 * entries [229, 241) rotate left by one entry per animation step. Empty for static palettes.
 */
class PaletteData(val rgba: ByteArray, val rotations: List<Pair<Int, Int>> = emptyList()) {
    init {
        require(rgba.size == PALETTE_BYTES) { "palette must be $PALETTE_BYTES bytes, got ${rgba.size}" }
    }
}

/**
 * Same meaning as RegionInfo, plus [paletteIndex] into [IndexedBundle.palettes].
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

/** Output of IndexedSpriteLoader. [pages] are Pixmap.Format.Alpha, each byte = palette index. Owns the pixmaps. */
class IndexedBundle(
    val pages: List<Pixmap>,
    val regions: List<IndexedRegionInfo>,
    val packerRects: Map<String, PackedRect>,
    val palettes: List<PaletteData>,
) : Disposable {
    override fun dispose() = pages.forEach { it.dispose() }
}

/** A LOD archive to read sprites from; sources are searched in list order (HotA.lod first). */
class LodSource(val name: String, val open: () -> LodDataSource, val aliases: Map<String, String>)

/** AtlasRegion over an index page; [paletteRow] is its row in the palette texture. */
class PaletteRegion(texture: Texture, x: Int, y: Int, width: Int, height: Int, val paletteRow: Int) :
    AtlasRegion(texture, x, y, width, height)

/**
 * Encodes a palette row into the SpriteBatch vertex colour: r = row low byte, g = row high byte,
 * b unused, a = alpha. The shader decodes it back (see PaletteShader).
 */
object PaletteColor {
    private const val BYTE_MAX = 255f
    private const val ROW_BYTE = 256

    fun encode(row: Int, alpha: Float = 1f): Float {
        require(row in 0 until MAX_PALETTE_ROWS) { "palette row out of range: $row" }
        return Color.toFloatBits((row % ROW_BYTE) / BYTE_MAX, (row / ROW_BYTE) / BYTE_MAX, 0f, alpha)
    }
}
