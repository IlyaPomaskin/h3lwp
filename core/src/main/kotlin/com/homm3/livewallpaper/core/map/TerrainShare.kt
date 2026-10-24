package com.homm3.livewallpaper.core.map

import com.badlogic.gdx.math.Rectangle
import com.homm3.livewallpaper.core.GameConfig.TILE_SIZE
import kotlin.math.floor

/**
 * Share of the map tiles touched by [viewBounds] whose terrain is one of [terrains]; tiles outside the
 * [mapSize] square are not counted, and a view that touches no map tile gives 0.
 */
internal fun visibleTerrainShare(
    mapSize: Int,
    viewBounds: Rectangle,
    terrains: Set<Int>,
    terrainAt: (x: Int, y: Int) -> Int,
): Float {
    val lastTile = mapSize - 1
    val xs = tileAt(viewBounds.x).coerceAtLeast(0)..tileAt(viewBounds.x + viewBounds.width).coerceAtMost(lastTile)
    val ys = tileAt(viewBounds.y).coerceAtLeast(0)..tileAt(viewBounds.y + viewBounds.height).coerceAtMost(lastTile)
    if (xs.isEmpty() || ys.isEmpty()) return 0f

    val matching = xs.sumOf { x -> ys.count { y -> terrainAt(x, y) in terrains } }
    val total = (xs.last - xs.first + 1) * (ys.last - ys.first + 1)
    return matching.toFloat() / total
}

private fun tileAt(world: Float): Int = floor(world / TILE_SIZE).toInt()
