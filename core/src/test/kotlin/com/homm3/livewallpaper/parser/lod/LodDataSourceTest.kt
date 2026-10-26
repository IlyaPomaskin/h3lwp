package com.homm3.livewallpaper.parser.lod

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LodDataSourceTest {
    private val lods = listOf(
        File("/Users/ilyapomaskin/temp/h3lwp-claude/assets-desktop/HotA.lod"),
        File("/Users/ilyapomaskin/temp/h3lwp-claude/assets-desktop/H3sprite.lod"),
    )

    @Test
    fun byteArraySourcePadsReadsPastEnd() {
        val source = ByteArrayLodDataSource(byteArrayOf(1, 2, 3, 4))
        assertContentEquals(byteArrayOf(3, 4, 0, 0), source.read(2, 4))
        assertContentEquals(byteArrayOf(0, 0), source.read(10, 2))
    }

    @Test
    fun rejectsReadsLongerThanArchive() {
        val source = ByteArrayLodDataSource(byteArrayOf(1, 2, 3))
        assertFailsWith<IllegalArgumentException> { source.read(0, 4) }
    }

    @Test
    fun rejectsHugeEntryCount() {
        val header = ByteArray(0x5C).also {
            byteArrayOf(0x4c, 0x4f, 0x44, 0, 0xc8.toByte(), 0, 0, 0).copyInto(it)
            ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).putInt(8, 50_000_000)
        }
        assertFailsWith<IllegalArgumentException> { LodReader(ByteArrayLodDataSource(header)).read() }
    }

    @Test
    fun fileSourceMatchesInMemoryReader() {
        assumeTrue(lods.all { it.exists() })
        for (lod in lods) {
            val inMemory = LodReader(ByteArrayLodDataSource(lod.readBytes()))
            LodReader(FileLodDataSource(lod)).use { fromFile ->
                val expected = inMemory.read()
                val actual = fromFile.read()
                assertEquals(expected, actual)
                val step = maxOf(1, expected.files.size / SAMPLE_COUNT)
                expected.files.filterIndexed { index, _ -> index % step == 0 }.forEach { entry ->
                    assertContentEquals(
                        inMemory.readFileContent(entry).readBytes(),
                        fromFile.readFileContent(entry).readBytes(),
                        "${lod.name} ${entry.name}"
                    )
                }
            }
        }
    }

    private companion object {
        const val SAMPLE_COUNT = 300
    }
}
