package com.homm3.livewallpaper.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.homm3.livewallpaper.parser.formats.H3mReader
import java.io.File

object Lwjgl3Launcher {
    @JvmStatic
    fun main(args: Array<String>) {
        val stream = File("gr.h3m").inputStream()
        val h3m = H3mReader(stream).read()
        println(h3m)
        stream.close()

//        createApplication()
    }

    private fun createApplication(): Lwjgl3Application {
        val config = Lwjgl3ApplicationConfiguration().apply {
            setForegroundFPS(60)
            setWindowPosition(0, 0)
            setWindowedMode(426, 500)
            setTitle("h3lwp")
            setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png")
        }

        return Lwjgl3Application(DesktopEngine(), config)
    }
}