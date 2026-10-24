package com.homm3.livewallpaper.core.screen

import org.junit.Test
import kotlin.test.assertEquals

class MapSwitchCountTest {
    @Test
    fun `grows with the square root of the screens that fit on the map`() {
        assertEquals(17, switchesBeforeMapChange(mapSize = 72, hasUnderground = false, tilesAcross = 10f, tilesDown = 22.3f))
    }

    @Test
    fun `an underground adds half of the surface screens`() {
        assertEquals(18, switchesBeforeMapChange(mapSize = 72, hasUnderground = true, tilesAcross = 10f, tilesDown = 22.3f))
        assertEquals(29, switchesBeforeMapChange(mapSize = 144, hasUnderground = true, tilesAcross = 10f, tilesDown = 22.3f))
    }

    @Test
    fun `a map that fits on one screen gets 10 switches`() {
        assertEquals(10, switchesBeforeMapChange(mapSize = 36, hasUnderground = false, tilesAcross = 40f, tilesDown = 89f))
    }

    @Test
    fun `144 screens get 30 switches and more screens stay at 30`() {
        assertEquals(30, switchesBeforeMapChange(mapSize = 144, hasUnderground = false, tilesAcross = 8f, tilesDown = 17.85f))
        assertEquals(30, switchesBeforeMapChange(mapSize = 144, hasUnderground = true, tilesAcross = 8f, tilesDown = 17.85f))
    }
}
