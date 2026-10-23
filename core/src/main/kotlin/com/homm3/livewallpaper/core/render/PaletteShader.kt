package com.homm3.livewallpaper.core.render

import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.homm3.livewallpaper.core.assets.indexed.PALETTES_PER_ROW
import com.homm3.livewallpaper.core.assets.indexed.PALETTE_SIZE
import com.homm3.livewallpaper.core.assets.indexed.PALETTE_TEXTURE_HEIGHT
import com.homm3.livewallpaper.core.assets.indexed.PALETTE_TEXTURE_WIDTH

/**
 * Draws GL_ALPHA index pages through a palette texture. The palette slot arrives in the vertex colour
 * (see PaletteColor): r = slot low byte, g = slot high byte, a = alpha.
 * Slot s is stored in texture row s / PALETTES_PER_ROW at column offset (s % PALETTES_PER_ROW) * PALETTE_SIZE.
 */
object PaletteShader {
    private const val BYTE_MAX = 255
    private const val SLOT_BYTE = 256
    private const val PACKED_ALPHA_MAX = 254

    // Slot is decoded per vertex (highp is guaranteed there) and handed over as a ready palette v coordinate
    // plus a whole-number column offset, so the fragment shader only adds the colour index to the offset.
    private val VERTEX = """
        #ifdef GL_ES
        #define HIGHP highp
        #else
        #define HIGHP
        #endif
        attribute vec4 a_position;
        attribute vec4 a_color;
        attribute vec2 a_texCoord0;
        uniform mat4 u_projTrans;
        varying vec2 v_texCoords;
        varying HIGHP float v_paletteV;
        varying HIGHP float v_paletteOffset;
        varying float v_alpha;
        void main() {
            HIGHP float slot = floor(a_color.r * $BYTE_MAX.0 + 0.5) + $SLOT_BYTE.0 * floor(a_color.g * $BYTE_MAX.0 + 0.5);
            HIGHP float row = floor(slot / $PALETTES_PER_ROW.0);
            v_paletteOffset = (slot - row * $PALETTES_PER_ROW.0) * $PALETTE_SIZE.0;
            v_paletteV = (row + 0.5) / $PALETTE_TEXTURE_HEIGHT.0;
            v_alpha = a_color.a * ($BYTE_MAX.0 / $PACKED_ALPHA_MAX.0);
            v_texCoords = a_texCoord0;
            gl_Position = u_projTrans * a_position;
        }
    """.trimIndent()

    private val FRAGMENT = """
        #ifdef GL_ES
        #ifdef GL_FRAGMENT_PRECISION_HIGH
        #define PALETTE_PRECISION highp
        #else
        #define PALETTE_PRECISION mediump
        #endif
        precision mediump float;
        #else
        #define PALETTE_PRECISION
        #endif
        varying vec2 v_texCoords;
        varying PALETTE_PRECISION float v_paletteV;
        varying PALETTE_PRECISION float v_paletteOffset;
        varying float v_alpha;
        uniform sampler2D u_texture;
        uniform sampler2D u_palette;
        void main() {
            PALETTE_PRECISION float index = floor(texture2D(u_texture, v_texCoords).a * $BYTE_MAX.0 + 0.5);
            PALETTE_PRECISION float paletteU = (v_paletteOffset + index + 0.5) / $PALETTE_TEXTURE_WIDTH.0;
            vec2 paletteUv = vec2(paletteU, v_paletteV);
            vec4 color = texture2D(u_palette, paletteUv);
            gl_FragColor = vec4(color.rgb, color.a * v_alpha);
        }
    """.trimIndent()

    fun compile(): ShaderProgram {
        ShaderProgram.pedantic = false
        val program = ShaderProgram(VERTEX, FRAGMENT)
        check(program.isCompiled) { "PaletteShader compile failed: ${program.log}" }
        return program
    }
}
