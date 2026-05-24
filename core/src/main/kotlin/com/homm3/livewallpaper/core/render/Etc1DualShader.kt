package com.homm3.livewallpaper.core.render

import com.badlogic.gdx.graphics.glutils.ShaderProgram

object Etc1DualShader {
    private const val VERT = """
        attribute vec4 a_position;
        attribute vec4 a_color;
        attribute vec2 a_texCoord0;
        uniform mat4 u_projTrans;
        varying vec4 v_color;
        varying vec2 v_texCoords;
        void main() {
            v_color = a_color;
            v_color.a = v_color.a * (255.0/254.0);
            v_texCoords = a_texCoord0;
            gl_Position = u_projTrans * a_position;
        }
    """

    private const val FRAG = """
        #ifdef GL_ES
        precision mediump float;
        #endif
        varying vec4 v_color;
        varying vec2 v_texCoords;
        uniform sampler2D u_texture;
        uniform sampler2D u_alpha;
        uniform float u_useDualSample;
        void main() {
            vec4 rgba = texture2D(u_texture, v_texCoords);
            float a = mix(rgba.a, texture2D(u_alpha, v_texCoords).r, u_useDualSample);
            gl_FragColor = v_color * vec4(rgba.rgb, a);
        }
    """

    fun compile(): ShaderProgram {
        ShaderProgram.pedantic = false
        val program = ShaderProgram(VERT, FRAG)
        check(program.isCompiled) { "Etc1DualShader compile failed: ${program.log}" }
        return program
    }

    fun setUseDualSample(program: ShaderProgram, on: Boolean) {
        program.bind()
        program.setUniformf("u_useDualSample", if (on) 1f else 0f)
        program.setUniformi("u_texture", 0)
        program.setUniformi("u_alpha", 1)
    }
}
