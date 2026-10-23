package com.homm3.livewallpaper.core.assets.indexed

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.utils.Disposable
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

/**
 * Palette texture: PALETTE_TEXTURE_WIDTH x PALETTE_TEXTURE_HEIGHT RGBA8888 holding PALETTES_PER_ROW palettes per row.
 * Palette slot s lives in row s / PALETTES_PER_ROW, column s % PALETTES_PER_ROW.
 */
const val PALETTES_PER_ROW = 4
const val PALETTE_TEXTURE_WIDTH = PALETTE_SIZE * PALETTES_PER_ROW
const val PALETTE_TEXTURE_HEIGHT = 2048
const val MAX_PALETTE_SLOTS = PALETTES_PER_ROW * PALETTE_TEXTURE_HEIGHT

/**
 * One 256-colour palette, RGBA8888 (1024 bytes), special indices already applied
 * (index 0 transparent, shadows as black with alpha, player colour neutralised).
 *
 * [rotations]: palette-cycling ranges as in DefPalette.paletteRotations, e.g. 229..241 means
 * entries [229, 241) rotate left by one entry per animation step. Empty for static palettes.
 */
class PaletteData(val rgba: ByteArray, val rotations: List<Pair<Int, Int>> = emptyList()) {
    init {
        require(rgba.size == PALETTE_BYTES) { "palette must be $PALETTE_BYTES bytes, got ${rgba.size}" }
    }
}

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

/** A LOD archive to read sprites from; sources are searched in list order (HotA.lod first). */
class LodSource(val name: String, val open: () -> LodDataSource, val aliases: Map<String, String>)

/** AtlasRegion over an index page; [paletteSlot] is its palette slot in the palette texture. */
class PaletteRegion(texture: Texture, x: Int, y: Int, width: Int, height: Int, val paletteSlot: Int) :
    AtlasRegion(texture, x, y, width, height)

/**
 * Encodes a palette slot into the SpriteBatch vertex colour: r = slot low byte, g = slot high byte,
 * b unused, a = alpha. The shader decodes it back (see PaletteShader).
 */
object PaletteColor {
    private const val BYTE_MAX = 255f
    private const val SLOT_BYTE = 256

    fun encode(slot: Int, alpha: Float = 1f): Float {
        require(slot in 0 until MAX_PALETTE_SLOTS) { "palette slot out of range: $slot" }
        return Color.toFloatBits((slot % SLOT_BYTE) / BYTE_MAX, (slot / SLOT_BYTE) / BYTE_MAX, 0f, alpha)
    }
}
