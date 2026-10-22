package com.homm3.livewallpaper.core.assets.indexed

import com.homm3.livewallpaper.core.assets.LodSpriteLoader
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PaletteAtlasTest {
    private val waterRotations = listOf(229 to 241, 242 to 254)
    private val rgbEntryBytes = 3

    private fun gradientPalette(seed: Int = 0) = ByteArray(PALETTE_BYTES) { (it * 7 + seed).toByte() }

    private fun toRgb(rgba: ByteArray) = ByteArray(PALETTE_SIZE * rgbEntryBytes) { i ->
        rgba[(i / rgbEntryBytes) * PALETTE_ENTRY_BYTES + i % rgbEntryBytes]
    }

    private fun referenceRotate(rgb: ByteArray, from: Int, to: Int) {
        val method = LodSpriteLoader::class.java.getDeclaredMethod(
            "rotatePalette", ByteArray::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType,
        )
        method.isAccessible = true
        method.invoke(LodSpriteLoader(), rgb, from, to)
    }

    @Test
    fun `identical static palettes share a row`() {
        val table = PaletteTable()
        val first = table.add(PaletteData(gradientPalette()))
        val second = table.add(PaletteData(gradientPalette()))
        val other = table.add(PaletteData(gradientPalette(seed = 1)))

        assertTrue(first.isNew)
        assertEquals(first.row, second.row)
        assertFalse(second.isNew)
        assertNotEquals(first.row, other.row)
        assertEquals(2, table.rowCount)
    }

    @Test
    fun `static palette input is copied`() {
        val bytes = gradientPalette()
        val table = PaletteTable()
        val row = table.add(PaletteData(bytes)).row
        bytes.fill(0)

        assertContentEquals(gradientPalette(), table.rowBytes(row))
        assertTrue(table.add(PaletteData(gradientPalette())).row == row)
    }

    @Test
    fun `rotating palettes always get unique rows`() {
        val table = PaletteTable()
        val static = table.add(PaletteData(gradientPalette())).row
        val rotatingA = table.add(PaletteData(gradientPalette(), waterRotations))
        val rotatingB = table.add(PaletteData(gradientPalette(), waterRotations))

        assertTrue(rotatingA.isNew && rotatingB.isNew)
        assertEquals(setOf(static, rotatingA.row, rotatingB.row).size, 3)
    }

    @Test
    fun `step rotates only rotating rows`() {
        val table = PaletteTable()
        val static = table.add(PaletteData(gradientPalette())).row
        val rotating = table.add(PaletteData(gradientPalette(), waterRotations)).row

        assertEquals(listOf(rotating), table.step())
        assertContentEquals(gradientPalette(), table.rowBytes(static))
        assertFalse(gradientPalette().contentEquals(table.rowBytes(rotating)))
    }

    @Test
    fun `rotation matches LodSpriteLoader rotatePalette after each step`() {
        val table = PaletteTable()
        val row = table.add(PaletteData(gradientPalette(), waterRotations)).row
        val reference = toRgb(gradientPalette())

        repeat(20) {
            table.step()
            waterRotations.forEach { (from, to) -> referenceRotate(reference, from, to) }
            assertContentEquals(reference, toRgb(table.rowBytes(row)), "step ${it + 1}")
        }
    }

    @Test
    fun `rotation keeps alpha with its entry`() {
        val palette = gradientPalette()
        val rotated = rotatePaletteEntries(palette, 10, 13)
        val entry = { bytes: ByteArray, index: Int ->
            bytes.copyOfRange(index * PALETTE_ENTRY_BYTES, (index + 1) * PALETTE_ENTRY_BYTES)
        }

        assertContentEquals(entry(palette, 11), entry(rotated, 10))
        assertContentEquals(entry(palette, 12), entry(rotated, 11))
        assertContentEquals(entry(palette, 10), entry(rotated, 12))
        assertContentEquals(entry(palette, 9), entry(rotated, 9))
        assertContentEquals(entry(palette, 13), entry(rotated, 13))
    }

    @Test
    fun `full cycle returns to the original palette`() {
        val rotations = listOf(183 to 195, 195 to 201) // 12- and 6-entry ranges: cycle = lcm = 12
        val cycleLength = 12
        val table = PaletteTable()
        val row = table.add(PaletteData(gradientPalette(), rotations)).row

        repeat(cycleLength - 1) {
            table.step()
            assertFalse(gradientPalette().contentEquals(table.rowBytes(row)), "early return at step ${it + 1}")
        }
        table.step()
        assertContentEquals(gradientPalette(), table.rowBytes(row))
    }

    @Test
    fun `invalid rotation range fails fast`() {
        val table = PaletteTable()
        assertFailsWith<IllegalArgumentException> {
            table.add(PaletteData(gradientPalette(), listOf(250 to 300)))
        }
        assertFailsWith<IllegalArgumentException> {
            table.add(PaletteData(gradientPalette(), listOf(20 to 10)))
        }
    }

    @Test
    fun `running out of rows fails fast`() {
        val maxRows = 3
        val table = PaletteTable(maxRows)
        repeat(maxRows) { table.add(PaletteData(gradientPalette(seed = it))) }

        assertEquals(table.add(PaletteData(gradientPalette(seed = 0))).row, 0)
        val error = assertFailsWith<IllegalStateException> {
            table.add(PaletteData(gradientPalette(seed = maxRows)))
        }
        assertTrue(error.message.orEmpty().contains("full"))
        assertFailsWith<IllegalStateException> {
            table.add(PaletteData(gradientPalette(), waterRotations))
        }
    }
}
