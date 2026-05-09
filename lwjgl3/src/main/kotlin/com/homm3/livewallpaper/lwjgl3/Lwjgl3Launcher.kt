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
fun main(args: Array<String>) {
    // This handles macOS support and helps on Windows.
    if (StartupHelper.startNewJvmIfRequired())
        return
    val parsedArgs = parseArgs(args)
    installFiles(parsedArgs)
    Lwjgl3Application(
        Engine(
            prefs = MutableStateFlow(WallpaperPreferences(brightness = 1.0f)),
            onSettingsButtonClick = ::copyLodFile,
            onHotaButtonClick = ::copyHotaLodFile,
            explicitMaps = parsedArgs.mapNames,
            headless = parsedArgs.headless
        ),
        Lwjgl3ApplicationConfiguration().apply {
            setTitle("Heroes 3 LiveWallpaper")
            useVsync(true)
            val displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode()
            setForegroundFPS(displayMode?.refreshRate?.plus(1) ?: 60)
            setWindowedMode(480, 480)
            setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))
            if (parsedArgs.headless) setInitialVisible(false)
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
    val lodFile = File("hota.lod")
    if (!lodFile.exists()) {
        onProgress("hota.lod not found in project root")
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

private data class ParsedArgs(
    val h3spritePath: String? = null,
    val hotaPath: String? = null,
    val mapPaths: List<String> = emptyList(),
    val headless: Boolean = false
) {
    val mapNames: List<String> get() = mapPaths.map { File(it).name }
}

private fun parseArgs(args: Array<String>): ParsedArgs {
    var h3sprite: String? = null
    var hota: String? = null
    val maps = mutableListOf<String>()
    var headless = false
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--h3sprite" -> { i++; require(i < args.size) { "Missing path after --h3sprite" }; h3sprite = args[i] }
            "--hota" -> { i++; require(i < args.size) { "Missing path after --hota" }; hota = args[i] }
            "--map" -> { i++; require(i < args.size) { "Missing path after --map" }; maps.add(args[i]) }
            "--headless" -> headless = true
        }
        i++
    }
    return ParsedArgs(h3sprite, hota, maps, headless)
}

private fun installFiles(args: ParsedArgs) {
    args.h3spritePath?.let { installFile(it, AssetPaths.LOD_FILE) }
    args.hotaPath?.let { installFile(it, AssetPaths.HOTA_LOD_FILE) }
    args.mapPaths.forEach { installFile(it, "${AssetPaths.USER_MAPS_FOLDER}/${File(it).name}") }
}

private fun installFile(srcPath: String, destPath: String) {
    val src = File(srcPath)
    require(src.exists()) { "File not found: $srcPath" }
    val dest = File(destPath)
    dest.parentFile?.mkdirs()
    if (src.canonicalPath != dest.canonicalPath) {
        src.copyTo(dest, overwrite = true)
    }
}
