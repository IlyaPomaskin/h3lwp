package com.homm3.livewallpaper.core.assets.indexed

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

    // Independent reference: entries [from, to) rotate left by one, the first entry moves to the end.
    private fun referenceRotate(rgb: ByteArray, from: Int, to: Int) {
        val entries = (from until to).map { rgb.copyOfRange(it * rgbEntryBytes, (it + 1) * rgbEntryBytes) }
        (entries.drop(1) + entries.take(1)).forEachIndexed { offset, entry ->
            entry.copyInto(rgb, (from + offset) * rgbEntryBytes)
        }
    }

    @Test
    fun `identical static palettes share a slot`() {
        val table = PaletteTable()
        val first = table.add(PaletteData(gradientPalette()))
        val second = table.add(PaletteData(gradientPalette()))
        val other = table.add(PaletteData(gradientPalette(seed = 1)))

        assertTrue(first.isNew)
        assertEquals(first.slots, second.slots)
        assertFalse(second.isNew)
        assertNotEquals(first.slots, other.slots)
        assertEquals(2, table.slotCount)
    }

    @Test
    fun `static palette input is copied`() {
        val bytes = gradientPalette()
        val table = PaletteTable()
        val slot = table.add(PaletteData(bytes)).slots.first
        bytes.fill(0)

        assertContentEquals(gradientPalette(), table.slotBytes(slot))
        assertEquals(slot, table.add(PaletteData(gradientPalette())).slots.first)
    }

    @Test
    fun `rotating palettes always get their own slots`() {
        val table = PaletteTable()
        val static = table.add(PaletteData(gradientPalette())).slots
        val rotatingA = table.add(PaletteData(gradientPalette(), waterRotations))
        val rotatingB = table.add(PaletteData(gradientPalette(), waterRotations))

        assertTrue(rotatingA.isNew && rotatingB.isNew)
        val allSlots = static.range + rotatingA.slots.range + rotatingB.slots.range
        assertEquals(allSlots.size, allSlots.toSet().size)
        assertEquals(table.slotCount, allSlots.size)
    }

    @Test
    fun `rotating palette gets one slot per phase matching reference rotation`() {
        val table = PaletteTable()
        val slots = table.add(PaletteData(gradientPalette(), waterRotations)).slots
        val reference = toRgb(gradientPalette())

        assertEquals(12, slots.count)
        slots.range.forEachIndexed { phase, slot ->
            assertContentEquals(reference, toRgb(table.slotBytes(slot)), "phase $phase")
            waterRotations.forEach { (from, to) -> referenceRotate(reference, from, to) }
        }
    }

    @Test
    fun `phase count is the full cycle`() {
        val rotations = listOf(183 to 195, 195 to 201) // 12- and 6-entry ranges: cycle = lcm = 12
        val phases = rotationPhases(gradientPalette(), rotations)

        assertEquals(12, phases.size)
        assertEquals(phases.size, phases.map { it.toList() }.toSet().size)
        val afterLastPhase = rotations.fold(phases.last()) { rotated, (from, to) ->
            rotatePaletteEntries(rotated, from, to)
        }
        assertContentEquals(gradientPalette(), afterLastPhase)
    }

    @Test
    fun `slot at step wraps around the phases`() {
        val slots = PaletteSlots(first = 10, count = 3)

        assertEquals(listOf(10, 11, 12, 10, 11), (0L until 5L).map(slots::at))
        assertEquals(7, PaletteSlots(first = 7, count = 1).at(123))
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
    fun `running out of slots fails fast`() {
        val maxSlots = 3
        val table = PaletteTable(maxSlots)
        repeat(maxSlots) { table.add(PaletteData(gradientPalette(seed = it))) }

        assertEquals(0, table.add(PaletteData(gradientPalette(seed = 0))).slots.first)
        val error = assertFailsWith<IllegalStateException> {
            table.add(PaletteData(gradientPalette(seed = maxSlots)))
        }
        assertTrue(error.message.orEmpty().contains("full"))
        assertFailsWith<IllegalStateException> {
            table.add(PaletteData(gradientPalette(), waterRotations))
        }
    }
}
