package com.homm3.livewallpaper.core.assets.indexed

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable
import com.homm3.livewallpaper.core.GameConfig.FRAME_TIME

class PaletteAtlas : Disposable {
    private val table = PaletteTable()
    private val slotPixmap = Pixmap(PALETTE_SIZE, SLOT_HEIGHT, Pixmap.Format.RGBA8888)
    private var elapsedSinceStep = 0f

    val texture: Texture = Texture(PALETTE_TEXTURE_WIDTH, PALETTE_TEXTURE_HEIGHT, Pixmap.Format.RGBA8888).apply {
        setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
    }

    fun add(palette: PaletteData): Int {
        val assignment = table.add(palette)
        if (assignment.isNew) uploadSlot(assignment.slot)
        return assignment.slot
    }

    /** Advances at most one rotation step per call, so a long pause does not replay missed steps. */
    fun update(delta: Float) {
        elapsedSinceStep += delta
        if (elapsedSinceStep < FRAME_TIME) return

        elapsedSinceStep %= FRAME_TIME
        table.step().forEach(::uploadSlot)
    }

    private fun uploadSlot(slot: Int) {
        val pixels = slotPixmap.pixels
        pixels.clear()
        pixels.put(table.slotBytes(slot))
        pixels.position(0)
        val column = slot % PALETTES_PER_ROW
        val row = slot / PALETTES_PER_ROW
        texture.draw(slotPixmap, column * PALETTE_SIZE, row)
    }

    override fun dispose() {
        texture.dispose()
        slotPixmap.dispose()
    }

    private companion object {
        const val SLOT_HEIGHT = 1
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
 * Same direction as LodSpriteLoader.rotatePalette, on RGBA entries: entries [from, to) shift left by one,
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
