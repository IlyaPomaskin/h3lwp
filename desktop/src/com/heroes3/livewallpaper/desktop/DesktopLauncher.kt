package com.heroes3.livewallpaper.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.utils.GdxNativesLoader
import com.heroes3.livewallpaper.core.Assets
import com.heroes3.livewallpaper.core.Engine
import com.heroes3.livewallpaper.parser.AssetsParser
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FileInputStream
import kotlin.system.measureTimeMillis

object DesktopLauncher {
    private fun getSelectedFile(): File? {
        val dialog = FileDialog(Frame())
        dialog.mode = FileDialog.LOAD
        dialog.file = "*.lod"
        dialog.isMultipleMode = false
        dialog.show()
        return dialog.files[0]
    }

    private fun parse() {
        try {
            val file = getSelectedFile() ?: return
            measureTimeMillis {
                GdxNativesLoader.load()
                AssetsParser(FileInputStream(file))
                    .parseLodToAtlas(File(Assets.atlasFolder), Assets.atlasName)
            }.run { println("done for $this ms") }
        } catch (e: Exception) {
            println("parse error")
            println(e.message)
            e.stackTrace.forEach { println(it) }
        }
    }

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

        val engine = Engine()
        engine.onSettingButtonClick = { parse() }
        LwjglApplication(engine, config)
    }
}