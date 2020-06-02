package com.homm3.livewallpaper.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        val config = LwjglApplicationConfiguration().apply {
            backgroundFPS = 60
            foregroundFPS = 60
            x = 0
            y = 0
            height = 426
            width = 500
        }

        LwjglApplication(DesktopEngine(), config)
    }
}