package com.homm3.livewallpaper.core.map

import com.badlogic.gdx.math.Rectangle
import com.homm3.livewallpaper.core.GameConfig.TILE_SIZE
import org.junit.Test
import kotlin.test.assertEquals

class TerrainShareTest {
    private val snow = 3
    private val grass = 2

    private fun tiles(x: Int, y: Int, width: Int, height: Int) =
        Rectangle(x * TILE_SIZE, y * TILE_SIZE, width * TILE_SIZE - 1, height * TILE_SIZE - 1)

    @Test
    fun `counts matching tiles inside the view`() {
        val share = visibleTerrainShare(mapSize = 10, tiles(0, 0, 4, 5), setOf(snow)) { x, _ ->
            if (x == 0) snow else grass
        }
        assertEquals(0.25f, share)
    }

    @Test
    fun `ignores tiles outside the map`() {
        val share = visibleTerrainShare(mapSize = 4, tiles(-2, -2, 4, 4), setOf(snow)) { _, _ -> snow }
        assertEquals(1f, share)
    }

    @Test
    fun `a view that misses the map gives zero`() {
        val share = visibleTerrainShare(mapSize = 4, tiles(10, 10, 3, 3), setOf(snow)) { _, _ -> snow }
        assertEquals(0f, share)
    }

    @Test
    fun `matches any of the given terrains`() {
        val sand = 1
        val share = visibleTerrainShare(mapSize = 2, tiles(0, 0, 2, 2), setOf(snow, sand)) { x, y ->
            listOf(snow, sand, grass, grass)[y * 2 + x]
        }
        assertEquals(0.5f, share)
    }
}
