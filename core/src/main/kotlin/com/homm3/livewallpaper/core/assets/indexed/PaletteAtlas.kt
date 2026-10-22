package com.homm3.livewallpaper.core.assets.indexed

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.utils.Disposable
import com.homm3.livewallpaper.core.GameConfig.FRAME_TIME

class PaletteAtlas : Disposable {
    private val table = PaletteTable()
    private val rowPixmap = Pixmap(PALETTE_SIZE, ROW_HEIGHT, Pixmap.Format.RGBA8888)
    private var elapsedSinceStep = 0f

    val texture: Texture = Texture(PALETTE_SIZE, MAX_PALETTE_ROWS, Pixmap.Format.RGBA8888).apply {
        setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
    }

    fun add(palette: PaletteData): Int {
        val assignment = table.add(palette)
        if (assignment.isNew) uploadRow(assignment.row)
        return assignment.row
    }

    /** Advances at most one rotation step per call, so a long pause does not replay missed steps. */
    fun update(delta: Float) {
        elapsedSinceStep += delta
        if (elapsedSinceStep < FRAME_TIME) return

        elapsedSinceStep %= FRAME_TIME
        table.step().forEach(::uploadRow)
    }

    private fun uploadRow(row: Int) {
        val pixels = rowPixmap.pixels
        pixels.clear()
        pixels.put(table.rowBytes(row))
        pixels.position(0)
        texture.draw(rowPixmap, 0, row)
    }

    override fun dispose() {
        texture.dispose()
        rowPixmap.dispose()
    }

    private companion object {
        const val ROW_HEIGHT = 1
    }
}

/** GL-free palette rows: assignment, dedupe of static palettes and runtime colour cycling. */
internal class PaletteTable(private val maxRows: Int = MAX_PALETTE_ROWS) {
    data class Assignment(val row: Int, val isNew: Boolean)

    private class RotatingRow(val row: Int, val rotations: List<Pair<Int, Int>>)

    private class PaletteKey(val bytes: ByteArray) {
        private val hash = bytes.contentHashCode()
        override fun hashCode() = hash
        override fun equals(other: Any?) = other is PaletteKey && bytes.contentEquals(other.bytes)
    }

    private val rows = mutableListOf<ByteArray>()
    private val staticRowByContent = mutableMapOf<PaletteKey, Int>()
    private val rotatingRows = mutableListOf<RotatingRow>()

    val rowCount: Int get() = rows.size

    fun add(palette: PaletteData): Assignment {
        val isRotating = palette.rotations.isNotEmpty()
        if (isRotating) {
            palette.rotations.forEach { (from, to) -> validateRange(from, to) }
            val row = appendRow(palette.rgba)
            rotatingRows.add(RotatingRow(row, palette.rotations))
            return Assignment(row, isNew = true)
        }

        val key = PaletteKey(palette.rgba.copyOf())
        staticRowByContent[key]?.let { return Assignment(it, isNew = false) }

        val row = appendRow(key.bytes)
        staticRowByContent[key] = row
        return Assignment(row, isNew = true)
    }

    fun rowBytes(row: Int): ByteArray = rows[row]

    /** Rotates every rotating row by one step; returns the rows that changed. */
    fun step(): List<Int> = rotatingRows.map { rotating ->
        rows[rotating.row] = rotating.rotations.fold(rows[rotating.row]) { palette, (from, to) ->
            rotatePaletteEntries(palette, from, to)
        }
        rotating.row
    }

    private fun appendRow(rgba: ByteArray): Int {
        check(rows.size < maxRows) { "Palette atlas is full: all $maxRows rows are used" }
        rows.add(rgba.copyOf())
        return rows.size - 1
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
