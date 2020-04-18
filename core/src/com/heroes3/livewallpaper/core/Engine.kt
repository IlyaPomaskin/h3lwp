package com.heroes3.livewallpaper.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20

class Engine : ApplicationAdapter() {
    override fun create() {
    }

    override fun render() {
        Gdx.gl.glClearColor(0.5f, 0.2f, 1f, 0.5f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }
}