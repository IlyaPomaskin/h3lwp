package com.homm3.livewallpaper.core.assets.indexed

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.Disposable

class PaletteAtlas : Disposable {
    private val table = PaletteTable()
    private val slotBuffer = BufferUtils.newByteBuffer(PALETTE_BYTES)

    val texture: Texture = Texture(PaletteTextureData(table)).apply {
        setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
    }

    fun add(palette: PaletteData): PaletteSlots {
        val assignment = table.add(palette)
        if (assignment.isNew) assignment.slots.range.forEach(::uploadSlot)
        return assignment.slots
    }

    private fun uploadSlot(slot: Int) {
        slotBuffer.clear()
        slotBuffer.put(table.slotBytes(slot)).flip()
        val column = slot % PALETTES_PER_ROW
        val row = slot / PALETTES_PER_ROW
        texture.bind()
        Gdx.gl.glTexSubImage2D(
            GL20.GL_TEXTURE_2D, BASE_LEVEL, column * PALETTE_SIZE, row, PALETTE_SIZE, SLOT_HEIGHT,
            GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, slotBuffer,
        )
    }

    override fun dispose() = texture.dispose()

    private companion object {
        const val SLOT_HEIGHT = 1
        const val BASE_LEVEL = 0
    }
}

/**
 * GL-free palette slots: assignment and dedupe of static palettes. A cycling palette gets one slot per rotation
 * phase up front, so nothing is uploaded while animating.
 */
internal class PaletteTable(private val maxSlots: Int = MAX_PALETTE_SLOTS) {
    data class Assignment(val slots: PaletteSlots, val isNew: Boolean)

    private class PaletteKey(val bytes: ByteArray) {
        private val hash = bytes.contentHashCode()
        override fun hashCode() = hash
        override fun equals(other: Any?) = other is PaletteKey && bytes.contentEquals(other.bytes)
    }

    private val slots = mutableListOf<ByteArray>()
    private val staticSlotByContent = mutableMapOf<PaletteKey, Int>()

    val slotCount: Int get() = slots.size

    fun add(palette: PaletteData): Assignment {
        val isRotating = palette.rotations.isNotEmpty()
        if (isRotating) {
            palette.rotations.forEach { (from, to) -> validateRange(from, to) }
            val phases = rotationPhases(palette.rgba, palette.rotations)
            checkCapacity(phases.size)
            val first = slots.size
            phases.forEach { phase -> slots.add(phase) }
            return Assignment(PaletteSlots(first, phases.size), isNew = true)
        }

        val key = PaletteKey(palette.rgba.copyOf())
        staticSlotByContent[key]?.let { return Assignment(PaletteSlots(it, STATIC_SLOT_COUNT), isNew = false) }

        checkCapacity(STATIC_SLOT_COUNT)
        val slot = slots.size
        slots.add(key.bytes)
        staticSlotByContent[key] = slot
        return Assignment(PaletteSlots(slot, STATIC_SLOT_COUNT), isNew = true)
    }

    fun slotBytes(slot: Int): ByteArray = slots[slot]

    private fun checkCapacity(needed: Int) {
        val hasRoom = slots.size + needed <= maxSlots
        check(hasRoom) { "Palette atlas is full: all $maxSlots slots are used" }
    }

    private fun validateRange(from: Int, to: Int) {
        val isValidRange = from in 0 until to && to <= PALETTE_SIZE
        require(isValidRange) { "invalid palette rotation range [$from, $to)" }
    }

    private companion object {
        const val STATIC_SLOT_COUNT = 1
    }
}

/**
 * Every distinct rotation phase of [palette]: phase k has all [rotations] applied k times. The cycle length is
 * the least common multiple of the range lengths, after which the palette repeats.
 */
internal fun rotationPhases(palette: ByteArray, rotations: List<Pair<Int, Int>>): List<ByteArray> {
    val cycleLength = rotations.map { (from, to) -> to - from }.reduce(::leastCommonMultiple)
    return generateSequence(palette.copyOf()) { phase ->
        rotations.fold(phase) { rotated, (from, to) -> rotatePaletteEntries(rotated, from, to) }
    }.take(cycleLength).toList()
}

private fun leastCommonMultiple(a: Int, b: Int): Int = a / greatestCommonDivisor(a, b) * b

private tailrec fun greatestCommonDivisor(a: Int, b: Int): Int = if (b == 0) a else greatestCommonDivisor(b, a % b)

/**
 * Rotation as in the original game's palette cycling, on RGBA entries: entries [from, to) shift left by one,
 * the entry at [from] moves to position to - 1. Returns a new array.
 */
internal fun rotatePaletteEntries(palette: ByteArray, from: Int, to: Int): ByteArray {
    val fromByte = from * PALETTE_ENTRY_BYTES
    val toByte = to * PALETTE_ENTRY_BYTES
    val firstEntry = palette.copyOfRange(fromByte, fromByte + PALETTE_ENTRY_BYTES)
    val rest = palette.copyOfRange(fromByte + PALETTE_ENTRY_BYTES, toByte)
    return palette.copyOf().also { rotated ->
        rest.copyInto(rotated, fromByte)
        firstEntry.copyInto(rotated, fromByte + rest.size)
    }
}
