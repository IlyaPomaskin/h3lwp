package com.homm3.livewallpaper.core.render

import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.homm3.livewallpaper.core.assets.indexed.MAX_PALETTE_ROWS
import com.homm3.livewallpaper.core.assets.indexed.PALETTE_SIZE

/**
 * Draws GL_ALPHA index pages through a palette texture. The palette row arrives in the vertex colour
 * (see PaletteColor): r = row low byte, g = row high byte, a = alpha.
 */
object PaletteShader {
    private const val BYTE_MAX = 255
    private const val ROW_BYTE = 256
    private const val PACKED_ALPHA_MAX = 254

    // Row is decoded per vertex (highp is guaranteed there) and handed over as a ready palette v coordinate,
    // so the fragment shader does no integer reconstruction and only needs enough precision to hold v.
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
        varying float v_alpha;
        void main() {
            HIGHP float row = floor(a_color.r * $BYTE_MAX.0 + 0.5) + $ROW_BYTE.0 * floor(a_color.g * $BYTE_MAX.0 + 0.5);
            v_paletteV = (row + 0.5) / $MAX_PALETTE_ROWS.0;
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
        varying float v_alpha;
        uniform sampler2D u_texture;
        uniform sampler2D u_palette;
        void main() {
            float index = texture2D(u_texture, v_texCoords).a;
            vec2 paletteUv = vec2((index * $BYTE_MAX.0 + 0.5) / $PALETTE_SIZE.0, v_paletteV);
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
