package com.homm3.livewallpaper.core.map.layers

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class TileCacheTest {
    private data class Key(val defName: String, val frameIndex: Int)

    @Test
    fun `same key returns the same tile instance and creates it once`() {
        val createdKeys = mutableListOf<Key>()
        val cache = TileCache<Key> { key ->
            createdKeys += key
            StaticTiledMapTile(TextureRegion())
        }

        val first = cache[Key("grastl", 3)]
        val second = cache[Key("grastl", 3)]

        assertSame(first, second)
        assertEquals(listOf(Key("grastl", 3)), createdKeys)
    }

    @Test
    fun `different keys produce distinct tiles`() {
        val cache = TileCache<Key> { StaticTiledMapTile(TextureRegion()) }

        val grass = cache[Key("grastl", 3)]
        val otherFrame = cache[Key("grastl", 4)]
        val otherDef = cache[Key("dirttl", 3)]

        assertNotSame(grass, otherFrame)
        assertNotSame(grass, otherDef)
        assertEquals(3, cache.size)
    }
}
