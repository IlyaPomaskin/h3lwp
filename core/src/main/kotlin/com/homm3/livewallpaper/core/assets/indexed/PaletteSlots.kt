package com.homm3.livewallpaper.core.assets.indexed

/**
 * Consecutive palette slots of one palette: a single slot for a static palette, one slot per rotation phase
 * for a cycling one, so animation picks a slot instead of re-uploading the palette.
 */
data class PaletteSlots(val first: Int, val count: Int) {
    init {
        require(first >= 0) { "first slot must be non-negative, got $first" }
        require(count > 0) { "slot count must be positive, got $count" }
    }

    val range: IntRange get() = first until first + count

    /** Slot showing animation [step]; [step] must be non-negative. */
    fun at(step: Long): Int = first + (step % count).toInt()
}
