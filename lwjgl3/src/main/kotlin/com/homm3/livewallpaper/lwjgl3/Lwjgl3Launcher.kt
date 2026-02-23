@file:JvmName("Lwjgl3Launcher")

package com.homm3.livewallpaper.lwjgl3

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.core.WallpaperPreferences
import com.homm3.livewallpaper.core.assets.LodValidator
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
            prefs = MutableStateFlow(WallpaperPreferences(brightness = 1.0f)),
            onSettingsButtonClick = ::copyLodFile,
            onHotaButtonClick = ::copyHotaLodFile
        ),
        Lwjgl3ApplicationConfiguration().apply {
            setTitle("Heroes 3 LiveWallpaper")
            useVsync(true)
            val displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode()
            setForegroundFPS(displayMode?.refreshRate?.plus(1) ?: 60)
            setWindowedMode(480, 480)
            setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))
        })
}

private fun copyLodFile(onProgress: (String) -> Unit, onDone: () -> Unit) {
    val lodFile = File("H3sprite.lod")
    if (!lodFile.exists()) {
        onProgress("H3sprite.lod not found in project root")
        return
    }

    thread(isDaemon = true) {
        try {
            onProgress("Validating H3sprite.lod...")
            val error = LodValidator.validate(lodFile.inputStream(), isHota = false)
            if (error != null) {
                onProgress(error)
                return@thread
            }
            onProgress("Copying H3sprite.lod...")
            lodFile.copyTo(Gdx.files.local(AssetPaths.LOD_FILE).file(), overwrite = true)
            onProgress("Done!")
            onDone()
        } catch (e: Exception) {
            onProgress(e.message ?: "Copy failed")
        }
    }
}

private fun copyHotaLodFile(onProgress: (String) -> Unit, onDone: () -> Unit) {
    val lodFile = File("HotA.lod")
    if (!lodFile.exists()) {
        onProgress("HotA.lod not found in project root")
        return
    }

    thread(isDaemon = true) {
        try {
            onProgress("Validating HotA.lod...")
            val error = LodValidator.validate(lodFile.inputStream(), isHota = true)
            if (error != null) {
                onProgress(error)
                return@thread
            }
            onProgress("Copying HotA.lod...")
            lodFile.copyTo(Gdx.files.local(AssetPaths.HOTA_LOD_FILE).file(), overwrite = true)
            onProgress("Done!")
            onDone()
        } catch (e: Exception) {
            onProgress(e.message ?: "Copy failed")
        }
    }
}
