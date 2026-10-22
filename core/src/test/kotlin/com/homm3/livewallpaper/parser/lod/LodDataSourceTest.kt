package com.homm3.livewallpaper.parser.lod

import java.io.File
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class LodDataSourceTest {
    private val lods = listOf(
        File("/Users/ilyapomaskin/temp/h3lwp-claude/assets-desktop/HotA.lod"),
        File("/Users/ilyapomaskin/temp/h3lwp-claude/assets-desktop/H3sprite.lod"),
    )

    @Test
    fun byteArraySourcePadsReadsPastEnd() {
        val source = ByteArrayLodDataSource(byteArrayOf(1, 2, 3))
        assertContentEquals(byteArrayOf(2, 3, 0, 0), source.read(1, 4))
        assertContentEquals(byteArrayOf(0, 0), source.read(10, 2))
    }

    @Test
    fun fileSourceMatchesInMemoryReader() {
        assumeTrue(lods.all { it.exists() })
        for (lod in lods) {
            val inMemory = LodReader(lod.inputStream())
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
