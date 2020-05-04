package com.homm3.livewallpaper.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.utils.GdxNativesLoader
import com.homm3.livewallpaper.core.Assets
import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.parser.AssetsConverter
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.InputStream
import kotlin.concurrent.thread

object DesktopLauncher {
    private fun getSelectedFile(): File? {
        val dialog = FileDialog(Frame())
        dialog.mode = FileDialog.LOAD
        dialog.file = "*.lod"
        dialog.isMultipleMode = false
        dialog.show()
        return dialog.files.getOrNull(0)
    }

    private fun clearOutputDirectory(outputDirectory: File) {
        if (outputDirectory.exists()) {
            outputDirectory.deleteRecursively()
        }
        outputDirectory.mkdirs()
    }

    private fun setAssetsReadyFlag(value: Boolean) {
        Gdx.app
            .getPreferences(Engine.PREFERENCES_NAME)
            .putBoolean(Engine.IS_ASSETS_READY_KEY, value)
            .flush()
    }

    private fun parse() {
        val file = getSelectedFile()
            ?: return

        GdxNativesLoader.load()

        thread {
            var stream: InputStream? = null
            var outputDirectory: File? = null
            try {
                println("Parsing...")
                kotlin
                    .runCatching { stream = file.inputStream() }
                    .onFailure { throw Exception("Can't open file") }
                    .mapCatching {
                        outputDirectory = File(Assets.atlasFolder)
                            .also {
                                clearOutputDirectory(it)
                                setAssetsReadyFlag(false)
                            }
                    }
                    .onFailure { throw Exception("Can't prepare output directory") }
                    .map { AssetsConverter(stream!!, outputDirectory!!, Assets.atlasName).convertLodToTextureAtlas() }
                    .map {
                        setAssetsReadyFlag(true)
                        println("Parsing successfully done!")
                    }
            } catch (ex: Exception) {
                outputDirectory?.run {
                    clearOutputDirectory(this)
                    setAssetsReadyFlag(false)
                }
                println("Fail: ${ex.message}")
            } finally {
                stream?.close()
            }
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
        engine.onSettingsButtonClick = {
            parse()
            engine.updateVisibleScreen()
        }
        LwjglApplication(engine, config)
    }
}