package com.homm3.livewallpaper.core.assets

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapDefListTest {
    private val castle = MapFileStamp("castle.h3m", length = 100, lastModified = 1, hasHotaAssets = true)
    private val swamp = MapFileStamp("swamp.h3m", length = 200, lastModified = 2, hasHotaAssets = true)
    private val list = MapDefList(setOf("avccast0.def", "edg.def"), setOf(castle))

    @Test
    fun `stamp line parses back to the same stamp`() {
        assertEquals(castle, MapFileStamp.parse(castle.serialize()))
        assertNull(MapFileStamp.parse("castle.h3m\t100"))
        assertNull(MapFileStamp.parse("castle.h3m\tlong\t1\ttrue"))
    }

    @Test
    fun `additions hold only DEFs not listed yet`() = runBlocking {
        val additions = list.additionsFor(listOf(castle, swamp)) { setOf("swmptl.def", "edg.def") }

        assertEquals(setOf("swmptl.def"), additions.defNames)
        assertEquals(setOf(swamp), additions.scannedMaps)
    }

    @Test
    fun `DEFs shared by several new maps are added once`() = runBlocking {
        val lake = swamp.copy(fileName = "lake.h3m")

        val additions = MapDefList.EMPTY.additionsFor(listOf(swamp, lake)) { setOf("edg.def", "${it.fileName}.def") }

        assertEquals(setOf("edg.def", "swamp.h3m.def", "lake.h3m.def"), additions.defNames)
    }

    @Test
    fun `scanned maps are not scanned again`() = runBlocking {
        val additions = list.additionsFor(listOf(castle)) { error("must not scan $it") }

        assertTrue(additions.isEmpty)
    }

    @Test
    fun `a changed or HotA-toggled map is scanned again`() = runBlocking {
        val scanned = mutableListOf<MapFileStamp>()
        val changed = castle.copy(lastModified = 5)
        val withoutHota = castle.copy(hasHotaAssets = false)

        list.additionsFor(listOf(changed, withoutHota)) { stamp -> emptySet<String>().also { scanned += stamp } }

        assertEquals(listOf(changed, withoutHota), scanned)
    }

    @Test
    fun `DEFs of removed maps stay listed`() = runBlocking {
        val updated = list + list.additionsFor(listOf(swamp)) { setOf("swmptl.def") }

        assertEquals(setOf("avccast0.def", "edg.def", "swmptl.def"), updated.defNames)
    }

    @Test
    fun `unreadable maps add nothing and are scanned again next time`() = runBlocking {
        val additions = list.additionsFor(listOf(swamp)) { null }

        assertTrue(additions.isEmpty)
    }
}
