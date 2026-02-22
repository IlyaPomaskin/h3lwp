@file:JvmName("Lwjgl3Launcher")

package com.homm3.livewallpaper.lwjgl3

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.core.WallpaperPreferences
import com.homm3.livewallpaper.parser.atlas.AtlasConverter
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import kotlin.concurrent.thread

/** Launches the desktop (LWJGL3) application. */
fun main() {
    // This handles macOS support and helps on Windows.
    if (StartupHelper.startNewJvmIfRequired())
        return
    Lwjgl3Application(
        Engine(
            prefs = MutableStateFlow(WallpaperPreferences()),
            onSettingsButtonClick = ::convertLodFile,
            onHotaButtonClick = ::convertHotaLodFile
        ),
        Lwjgl3ApplicationConfiguration().apply {
            setTitle("Heroes 3 LiveWallpaper")
            useVsync(true)
            setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1)
            setWindowedMode(640, 480)
            setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))
        })
}

private fun convertLodFile(onProgress: (String) -> Unit, onDone: () -> Unit) {
    val lodFile = File("H3sprite.lod")
    if (!lodFile.exists()) {
        onProgress("H3sprite.lod not found in project root")
        return
    }

    thread(isDaemon = true) {
        val outputDir = Gdx.files.local(AssetPaths.ATLAS_FOLDER).file()
        outputDir.mkdirs()
        try {
            AtlasConverter(lodFile.inputStream(), outputDir, AssetPaths.ATLAS_NAME)
                .convert(onProgress)
            onDone()
        } catch (e: Exception) {
            onProgress(e.message ?: "Conversion failed")
        }
    }
}

private fun convertHotaLodFile(onProgress: (String) -> Unit, onDone: () -> Unit) {
    val lodFile = File("HotA.lod")
    if (!lodFile.exists()) {
        onProgress("HotA.lod not found in project root")
        return
    }

    thread(isDaemon = true) {
        val outputDir = Gdx.files.local(AssetPaths.ATLAS_FOLDER).file()
        outputDir.mkdirs()
        try {
            AtlasConverter(lodFile.inputStream(), outputDir, AssetPaths.HOTA_ATLAS_NAME, minimalDefCount = 0)
                .convert(onProgress)
            onDone()
        } catch (e: Exception) {
            onProgress(e.message ?: "Conversion failed")
        }
    }
}
