package com.homm3.livewallpaper.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.GdxNativesLoader
import com.homm3.livewallpaper.core.Constants
import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.parser.AssetsConverter
import java.io.File
import java.io.InputStream

class DesktopEngine : Engine() {
    private fun clearOutputDirectory(outputDirectory: File) {
        if (outputDirectory.exists()) {
            outputDirectory.deleteRecursively()
        }
        outputDirectory.mkdirs()
    }

    private fun setAssetsReadyFlag(value: Boolean) {
        Gdx.app
            .getPreferences(Constants.Preferences.PREFERENCES_NAME)
            .putBoolean(Constants.Preferences.IS_ASSETS_READY_KEY, value)
            .flush()
    }

    private fun parse(callback: () -> Unit) {
        val file = File("H3sprite.lod")

        if (!file.exists()) {
            println("File ${file.absolutePath} not found")
            return
        }

        GdxNativesLoader.load()

        var stream: InputStream? = null
        var outputDirectory: File? = null
        try {
            println("Parsing...")
            kotlin
                .runCatching { stream = file.inputStream() }
                .onFailure { throw Exception("Can't open file") }
                .mapCatching {
                    outputDirectory = File(Constants.Assets.ATLAS_FOLDER)
                        .also {
                            clearOutputDirectory(it)
                            setAssetsReadyFlag(false)
                        }
                }
                .onFailure { throw Exception("Can't prepare output directory") }
                .map {
                    AssetsConverter(
                        stream!!,
                        outputDirectory!!,
                        Constants.Assets.ATLAS_NAME
                    ).convertLodToTextureAtlas()
                }
                .map {
                    setAssetsReadyFlag(true)
                    println("Parsing successfully done!")
                    callback()
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

    override fun onSettingsButtonClick() {
        parse { Gdx.app.postRunnable { create() } }
    }
}