package com.homm3.livewallpaper.core.map

import org.junit.Test
import kotlin.test.assertEquals

class MonsterAnchorTest {
    private val classicTemplate = listOf(0, 0x8000)
    private val hotaTemplate = listOf(0, 0x4000)
    private val hotaCanvasWidth = 96
    private val classicCanvasWidth = 64

    @Test
    fun `HotA sprite on a classic template moves one tile right`() {
        assertEquals(1, MonsterAnchor.shiftTiles(hotaCanvasWidth, classicTemplate))
    }

    @Test
    fun `sprites on their own template stay`() {
        assertEquals(0, MonsterAnchor.shiftTiles(hotaCanvasWidth, hotaTemplate))
        assertEquals(0, MonsterAnchor.shiftTiles(classicCanvasWidth, classicTemplate))
    }

    @Test
    fun `classic sprite on a HotA template moves one tile left`() {
        assertEquals(-1, MonsterAnchor.shiftTiles(classicCanvasWidth, hotaTemplate))
    }

    @Test
    fun `templates without a visit tile are left alone`() {
        assertEquals(0, MonsterAnchor.shiftTiles(hotaCanvasWidth, listOf(0, 0)))
    }
}
