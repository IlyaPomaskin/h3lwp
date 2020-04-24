package com.heroes3.livewallpaper.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.heroes3.livewallpaper.core.Engine
import com.heroes3.livewallpaper.parser.AssetsParser
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FileInputStream
import kotlin.system.measureTimeMillis

object DesktopLauncher {
    fun getSelectedFile(): File {
        val dialog = FileDialog(Frame())
        dialog.mode = FileDialog.LOAD
        dialog.file = "*.lod"
        dialog.isMultipleMode = false
        dialog.show()
        return dialog.files[0]!!
    }

    fun parse() {
        try {
            val time = measureTimeMillis {
                val ap = AssetsParser(FileInputStream("/Users/ipomaskin/Documents/Games/Heroes3/Data/H3sprite.lod"))
                ap.parseLodToAtlas(Gdx.files.local("sprites/h3/"), "assets")
            }
            println("done for $time ms")
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
        LwjglApplication(Engine(), config)
//        parse()
    }
}