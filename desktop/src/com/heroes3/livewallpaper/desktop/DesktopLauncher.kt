package com.heroes3.livewallpaper.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.heroes3.livewallpaper.core.Engine

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        val config = LwjglApplicationConfiguration()
        config.backgroundFPS = 60
        config.foregroundFPS = 60
        config.x = 0
        config.y = 0
        config.height = 426
        config.width = 500
        LwjglApplication(Engine(), config)
    }
}