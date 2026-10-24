package com.homm3.livewallpaper.core.assets.indexed

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.Disposable

class PaletteAtlas : Disposable {
    private val table = PaletteTable()
    private val slotBuffer = BufferUtils.newByteBuffer(PALETTE_BYTES)
    private var lastStep = 0L

    val texture: Texture = Texture(PaletteTextureData(table)).apply {
        setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
    }

    fun add(palette: PaletteData): Int {
        val assignment = table.add(palette)
        if (assignment.isNew) uploadSlot(assignment.slot)
        return assignment.slot
    }

    /** Rotates once per new animation [step], so steps coalesced by a late frame are not replayed. */
    fun update(step: Long) {
        if (step == lastStep) return

        lastStep = step
        table.step().forEach(::uploadSlot)
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

/** GL-free palette slots: assignment, dedupe of static palettes and runtime colour cycling. */
internal class PaletteTable(private val maxSlots: Int = MAX_PALETTE_SLOTS) {
    data class Assignment(val slot: Int, val isNew: Boolean)

    private class RotatingSlot(val slot: Int, val rotations: List<Pair<Int, Int>>)

    private class PaletteKey(val bytes: ByteArray) {
        private val hash = bytes.contentHashCode()
        override fun hashCode() = hash
        override fun equals(other: Any?) = other is PaletteKey && bytes.contentEquals(other.bytes)
    }

    private val slots = mutableListOf<ByteArray>()
    private val staticSlotByContent = mutableMapOf<PaletteKey, Int>()
    private val rotatingSlots = mutableListOf<RotatingSlot>()

    val slotCount: Int get() = slots.size

    fun add(palette: PaletteData): Assignment {
        val isRotating = palette.rotations.isNotEmpty()
        if (isRotating) {
            palette.rotations.forEach { (from, to) -> validateRange(from, to) }
            val slot = appendSlot(palette.rgba)
            rotatingSlots.add(RotatingSlot(slot, palette.rotations))
            return Assignment(slot, isNew = true)
        }

        val key = PaletteKey(palette.rgba.copyOf())
        staticSlotByContent[key]?.let { return Assignment(it, isNew = false) }

        val slot = appendSlot(key.bytes)
        staticSlotByContent[key] = slot
        return Assignment(slot, isNew = true)
    }

    fun slotBytes(slot: Int): ByteArray = slots[slot]

    /** Rotates every rotating palette by one step; returns the slots that changed. */
    fun step(): List<Int> = rotatingSlots.map { rotating ->
        slots[rotating.slot] = rotating.rotations.fold(slots[rotating.slot]) { palette, (from, to) ->
            rotatePaletteEntries(palette, from, to)
        }
        rotating.slot
    }

    private fun appendSlot(rgba: ByteArray): Int {
        check(slots.size < maxSlots) { "Palette atlas is full: all $maxSlots slots are used" }
        slots.add(rgba.copyOf())
        return slots.size - 1
    }

    private fun validateRange(from: Int, to: Int) {
        val isValidRange = from in 0 until to && to <= PALETTE_SIZE
        require(isValidRange) { "invalid palette rotation range [$from, $to)" }
    }
}

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
