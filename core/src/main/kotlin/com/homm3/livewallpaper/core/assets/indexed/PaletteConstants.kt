package com.homm3.livewallpaper.core.assets.indexed

const val PALETTE_SIZE = 256
const val PALETTE_ENTRY_BYTES = 4
const val PALETTE_BYTES = PALETTE_SIZE * PALETTE_ENTRY_BYTES

/**
 * Palette texture: PALETTE_TEXTURE_WIDTH x PALETTE_TEXTURE_HEIGHT RGBA8888 holding PALETTES_PER_ROW palettes per row.
 * Palette slot s lives in row s / PALETTES_PER_ROW, column s % PALETTES_PER_ROW.
 */
const val PALETTES_PER_ROW = 4
const val PALETTE_TEXTURE_WIDTH = PALETTE_SIZE * PALETTES_PER_ROW
const val PALETTE_TEXTURE_HEIGHT = 2048
const val MAX_PALETTE_SLOTS = PALETTES_PER_ROW * PALETTE_TEXTURE_HEIGHT
