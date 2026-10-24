package com.homm3.livewallpaper.core.library

import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertEquals

class MapLibraryTest {
    private val files = listOf("a.h3m", "b.h3m", "c.h3m", "d.h3m")

    @Test
    fun `random load order tries every other file before the current one`() {
        repeat(20) { seed ->
            val order = randomLoadOrder(files, currentFile = "b.h3m", random = Random(seed))

            assertEquals(files.toSet(), order.toSet())
            assertEquals("b.h3m", order.last())
        }
    }

    @Test
    fun `random load order varies the first pick`() {
        val firstPicks = (0 until 50).map { seed -> randomLoadOrder(files, currentFile = null, random = Random(seed)).first() }

        assertEquals(files.toSet(), firstPicks.toSet())
    }

    @Test
    fun `random load order of a single file is that file`() {
        assertEquals(listOf("a.h3m"), randomLoadOrder(listOf("a.h3m"), currentFile = "a.h3m", random = Random(0)))
        assertEquals(emptyList(), randomLoadOrder(emptyList(), currentFile = null, random = Random(0)))
    }

    @Test
    fun `folder change is unchanged only when the set is equal and nothing loaded was removed`() {
        val previous = setOf("a.h3m", "b.h3m")
        assertEquals(FolderChange.UNCHANGED, classifyFolderChange(previous, previous, emptySet()))
        assertEquals(FolderChange.CHANGED, classifyFolderChange(previous, previous + "c.h3m", emptySet()))
        assertEquals(FolderChange.CHANGED, classifyFolderChange(previous, setOf("a.h3m"), setOf("b.h3m")))
        assertEquals(FolderChange.EMPTY, classifyFolderChange(previous, emptySet(), setOf("a.h3m")))
    }
}
