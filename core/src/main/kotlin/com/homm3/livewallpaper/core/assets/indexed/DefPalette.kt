package com.homm3.livewallpaper.core.assets.indexed

/** Special palette entries, palette cycling and terrain DEF names shared by the sprite pipeline. */
internal object DefPalette {
    // Expected source colors for special palette indices 0-7 (from VCMI)
    // H3 uses 16-bit color (565 RGB) so actual values may differ by up to 7
    private val sourceColors = arrayOf(
        intArrayOf(0, 255, 255),     // 0: cyan (transparency)
        intArrayOf(255, 150, 255),   // 1: light magenta (shadow border)
        intArrayOf(255, 100, 255),   // 2: mid magenta (shadow border) - fuzzy matched
        intArrayOf(255, 50, 255),    // 3: dark magenta (shadow body) - fuzzy matched
        intArrayOf(255, 0, 255),     // 4: pure magenta (shadow body)
        intArrayOf(255, 255, 0),     // 5: yellow (selection/flag)
        intArrayOf(180, 0, 255),     // 6: purple (shadow below selection)
        intArrayOf(0, 255, 0)        // 7: green (shadow border below selection)
    )

    // Target alpha for each special index
    private val targetAlpha = byteArrayOf(
        0x00.toByte(), // 0: fully transparent
        0x40.toByte(), // 1: shadow border (alpha 64)
        0x40.toByte(), // 2: shadow border (alpha 64)
        0x80.toByte(), // 3: shadow body (alpha 128)
        0x80.toByte(), // 4: shadow body (alpha 128)
        0xFF.toByte(), // 5: flag/selection — neutral gray (no player ownership in wallpaper)
        0x80.toByte(), // 6: shadow below selection (alpha 128)
        0x40.toByte()  // 7: shadow border below selection (alpha 64)
    )

    // Neutral flag color (VCMI neutral: 0x84, 0x84, 0x84)
    private val neutralFlagColor = intArrayOf(0x84, 0x84, 0x84)

    // Known flag colors used for detection: yellow marker + 8 player colors + neutral gray
    // Castle/mine DEFs have one of these at index 5 instead of the standard yellow
    // All matched entries are replaced with neutralFlagColor (gray)
    // Original VCMI player colors:
    //   red (255, 0, 0), blue (49, 82, 255), tan (156, 115, 82),
    //   green (66, 148, 41), orange (255, 132, 0), purple (140, 41, 165),
    //   teal (9, 156, 165), pink (198, 123, 140)
    private val flagColors = arrayOf(
        intArrayOf(255, 255, 0),     // yellow (standard marker)
        intArrayOf(255, 0, 0),       // red (player 0)
        intArrayOf(49, 82, 255),     // blue (player 1)
        intArrayOf(156, 115, 82),    // tan (player 2)
        intArrayOf(66, 148, 41),     // green (player 3)
        intArrayOf(255, 132, 0),     // orange (player 4)
        intArrayOf(140, 41, 165),    // purple (player 5)
        intArrayOf(9, 156, 165),     // teal (player 6)
        intArrayOf(198, 123, 140),   // pink (player 7)
        intArrayOf(0x84, 0x84, 0x84) // neutral gray
    )

    // Indices 0, 1, 4 are always replaced (shadow/transparency)
    // Index 5: replaced only if it matches a known flag/player color
    // Indices 2, 3, 6, 7 only replaced if original color matches expected source
    private val alwaysReplace = setOf(0, 1, 4)
    private val fuzzyReplace = setOf(2, 3, 6, 7)
    private val colorThreshold = 8

    private fun colorSimilar(palette: ByteArray, index: Int, expected: IntArray): Boolean {
        val offset = index * 3
        val r = palette[offset].toInt() and 0xFF
        val g = palette[offset + 1].toInt() and 0xFF
        val b = palette[offset + 2].toInt() and 0xFF
        return Math.abs(r - expected[0]) < colorThreshold &&
                Math.abs(g - expected[1]) < colorThreshold &&
                Math.abs(b - expected[2]) < colorThreshold
    }

    private fun isFlagColor(palette: ByteArray, index: Int): Boolean {
        return flagColors.any { colorSimilar(palette, index, it) }
    }

    /**
     * Apply special palette handling: overwrite palette RGB for shadow/flag indices,
     * and build the corresponding tRNS alpha array.
     * Indices 0,1,4: always replaced (shadow/transparency).
     * Index 5: replaced with neutral gray only if color matches a known flag/player color.
     * Indices 2,3,6,7: replaced only if color matches expected source (fuzzy).
     */
    internal fun applySpecialPalette(originalPalette: ByteArray): Pair<ByteArray, ByteArray> {
        val palette = originalPalette.clone()
        val alpha = ByteArray(8) { 0xFF.toByte() } // default opaque

        for (i in 0 until 8) {
            val shouldReplace = when {
                i in alwaysReplace -> true
                i == 5 -> isFlagColor(originalPalette, i)
                i in fuzzyReplace -> colorSimilar(originalPalette, i, sourceColors[i])
                else -> false
            }
            if (shouldReplace) {
                val offset = i * 3
                if (i == 5) {
                    // Flag/selection: use neutral gray
                    palette[offset] = neutralFlagColor[0].toByte()
                    palette[offset + 1] = neutralFlagColor[1].toByte()
                    palette[offset + 2] = neutralFlagColor[2].toByte()
                } else {
                    palette[offset] = 0
                    palette[offset + 1] = 0
                    palette[offset + 2] = 0
                }
                alpha[i] = targetAlpha[i]
            }
        }
        // Replace any sourceColor found anywhere in the full palette with neutral gray
        for (i in 0 until palette.size / 3) {
            if (sourceColors.any { colorSimilar(palette, i, it) }) {
                val offset = i * 3
                palette[offset] = neutralFlagColor[0].toByte()
                palette[offset + 1] = neutralFlagColor[1].toByte()
                palette[offset + 2] = neutralFlagColor[2].toByte()
            }
        }

        return palette to alpha
    }

    internal val paletteRotations = hashMapOf(
        "watrtl.def" to listOf(229 to 241, 242 to 254),
        "lavatl.def" to listOf(246 to 254),
        "clrrvr.def" to listOf(183 to 195, 195 to 201),
        "mudrvr.def" to listOf(183 to 189, 240 to 246),
        "lavrvr.def" to listOf(240 to 248)
    )

    internal val terrainDefNames = setOf(
        "dirttl.def", "sandtl.def", "grastl.def", "snowtl.def", "swmptl.def",
        "rougtl.def", "subbtl.def", "lavatl.def", "watrtl.def", "rocktl.def",
        "highlnd.def", "wastlnd.def",
        "clrrvr.def", "icyrvr.def", "mudrvr.def", "lavrvr.def",
        "dirtrd.def", "gravrd.def", "cobbrd.def",
        "edg.def"
    )

    internal val terrainPcxPrefixes = mapOf(
        "wstlt" to "wastlnd.def",
        "hglnt" to "highlnd.def"
    )
}
