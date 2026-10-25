package com.homm3.livewallpaper.core.assets

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

class MapValidityStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val store: MapValidityStore get() = MapValidityStore(tempFolder.root.resolve("map-validity.txt"))

    private fun mapFile(name: String, content: ByteArray = byteArrayOf(1, 2, 3)): File =
        tempFolder.root.resolve(name).apply { writeBytes(content) }

    @Test
    fun `unchecked maps have no known validity`() {
        val map = mapFile("a.h3m")

        assertEquals(emptyMap(), store.knownValidities(listOf(map)))
    }

    @Test
    fun `recorded validity is returned while the file is unchanged`() {
        val valid = mapFile("valid.h3m")
        val invalid = mapFile("invalid.h3m")
        store.record(valid, MapValidity.VALID)
        store.record(invalid, MapValidity.INVALID)

        val validities = store.knownValidities(listOf(valid, invalid))

        assertEquals(mapOf("valid.h3m" to MapValidity.VALID, "invalid.h3m" to MapValidity.INVALID), validities)
    }

    @Test
    fun `a replaced file counts as unchecked`() {
        val map = mapFile("a.h3m")
        store.record(map, MapValidity.INVALID)
        map.writeBytes(byteArrayOf(1, 2, 3, 4))

        assertEquals(emptyMap(), store.knownValidities(listOf(map)))
    }

    @Test
    fun `the latest record wins`() {
        val map = mapFile("a.h3m")
        store.record(map, MapValidity.INVALID)
        store.record(map, MapValidity.VALID)

        assertEquals(mapOf("a.h3m" to MapValidity.VALID), store.knownValidities(listOf(map)))
    }

    @Test
    fun `check parses an unchecked map and records the result`() {
        val map = mapFile("garbage.h3m", ByteArray(64) { 0x7F })

        assertEquals(MapValidity.INVALID, store.check(map))
        assertEquals(mapOf("garbage.h3m" to MapValidity.INVALID), store.knownValidities(listOf(map)))
    }
}
