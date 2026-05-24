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
        void main() {
            vec3 rgb = texture2D(u_texture, v_texCoords).rgb;
            float a   = texture2D(u_alpha,   v_texCoords).r;
            gl_FragColor = v_color * vec4(rgb, a);
        }
    """

    fun compile(): ShaderProgram {
        ShaderProgram.pedantic = false
        val program = ShaderProgram(VERT, FRAG)
        check(program.isCompiled) { "Etc1DualShader compile failed: ${program.log}" }
        return program
    }
}
