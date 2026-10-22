package com.homm3.livewallpaper.core.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.NumberUtils
import com.homm3.livewallpaper.core.assets.indexed.MAX_PALETTE_ROWS
import com.homm3.livewallpaper.core.assets.indexed.PaletteColor
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.floor

class PaletteColorTest {
    private val rows = listOf(0, 1, 255, 256, 1000, MAX_PALETTE_ROWS - 1)

    @Test
    fun rowSurvivesVertexColorPacking() {
        rows.forEach { row ->
            val color = unpack(PaletteColor.encode(row))
            val decodedRow = shaderRound(color.r) + ROW_BYTE * shaderRound(color.g)
            assertEquals("row $row", row, decodedRow)
        }
    }

    @Test
    fun rowBytesMatchRawVertexBytes() {
        rows.forEach { row ->
            val abgr = NumberUtils.floatToIntColor(PaletteColor.encode(row))
            val red = abgr and BYTE_MASK
            val green = (abgr ushr Byte.SIZE_BITS) and BYTE_MASK
            assertEquals("row $row", row, red + ROW_BYTE * green)
        }
    }

    @Test
    fun alphaSurvivesPackingWithinTwoSteps() {
        listOf(0f, 0.25f, 0.5f, 1f).forEach { alpha ->
            val color = unpack(PaletteColor.encode(MAX_PALETTE_ROWS - 1, alpha))
            val shaderAlpha = color.a * (BYTE_MAX / PACKED_ALPHA_MAX)
            assertEquals("alpha $alpha", alpha, shaderAlpha, ALPHA_TOLERANCE)
        }
    }

    private fun unpack(packed: Float): Color {
        return Color().also { Color.abgr8888ToColor(it, packed) }
    }

    private fun shaderRound(normalized: Float): Int = floor(normalized * BYTE_MAX + 0.5f).toInt()

    private companion object {
        const val ROW_BYTE = 256
        const val BYTE_MASK = 0xFF
        const val BYTE_MAX = 255f
        const val PACKED_ALPHA_MAX = 254f
        // toFloatBits truncates alpha to an int byte and then masks it to an even value: up to two steps lost
        const val ALPHA_TOLERANCE = 2f / 255f
    }
}
