package com.homm3.livewallpaper.core.library

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapLibraryTest {
    private val files = listOf("a.h3m", "b.h3m", "c.h3m", "d.h3m")

    @Test
    fun `next unloaded file is searched forward with wrap-around`() {
        val loaded = setOf("b.h3m", "c.h3m")
        assertEquals(3, findNextUnloadedIndex(files, fromIndex = 0, step = 1) { it in loaded })
        assertEquals(0, findNextUnloadedIndex(files, fromIndex = 3, step = 1) { it in loaded })
    }

    @Test
    fun `next unloaded file is searched backward with wrap-around`() {
        val loaded = setOf("a.h3m", "d.h3m")
        assertEquals(2, findNextUnloadedIndex(files, fromIndex = 0, step = -1) { it in loaded })
        assertEquals(1, findNextUnloadedIndex(files, fromIndex = 2, step = -1) { it in loaded })
    }

    @Test
    fun `the start file is checked last`() {
        val loaded = files.toSet() - "b.h3m"
        assertEquals(1, findNextUnloadedIndex(files, fromIndex = 1, step = 1) { it in loaded })
    }

    @Test
    fun `no next file when everything is loaded or there are no files`() {
        assertNull(findNextUnloadedIndex(files, fromIndex = 0, step = 1) { true })
        assertNull(findNextUnloadedIndex(emptyList(), fromIndex = 0, step = 1) { false })
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
