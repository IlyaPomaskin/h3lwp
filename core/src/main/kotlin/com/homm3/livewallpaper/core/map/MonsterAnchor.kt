package com.homm3.livewallpaper.core.map

import com.homm3.livewallpaper.core.GameConfig.TILE_SIZE

/**
 * Lines a monster sprite up with the tile its map template makes visitable. Objects are drawn from their
 * bottom-right tile, but monster sprites differ in where the creature stands: the original 64-px-wide sprites put it
 * in the rightmost tile, HotA's 96-px-wide ones in the middle tile. HotA's templates expect the middle tile, the
 * original game's (in its maps, and for random monsters there) the rightmost one, so a HotA sprite on an original
 * template would stand one tile left of its tile.
 */
object MonsterAnchor {
    private const val CREATURE_WIDTH_TILES = 2
    private const val MASK_COLUMNS = 8
    private const val BOTTOM_ROW_SHIFT = 8
    private const val ROW_BITS = 0xFF
    private const val MASK_BOTTOM_ROWS_INDEX = 1

    /** Tiles to move the sprite right: where the sprite's creature stands minus where the template's visit tile is. */
    fun shiftTiles(canvasWidth: Int, activeCells: List<Int>): Int {
        val visitColumnFromRight = visitColumnFromRight(activeCells) ?: return 0
        val creatureColumnFromRight = canvasWidth / TILE_SIZE.toInt() - CREATURE_WIDTH_TILES
        return creatureColumnFromRight - visitColumnFromRight
    }

    /** The template's bottom row is the high byte of the mask's last value; its highest set bit is the rightmost column. */
    private fun visitColumnFromRight(activeCells: List<Int>): Int? {
        val bottomRow = (activeCells.getOrNull(MASK_BOTTOM_ROWS_INDEX) ?: return null) ushr BOTTOM_ROW_SHIFT and ROW_BITS
        if (bottomRow == 0) return null
        val rightmostColumn = Integer.SIZE - 1 - Integer.numberOfLeadingZeros(bottomRow)
        return MASK_COLUMNS - 1 - rightmostColumn
    }
}
