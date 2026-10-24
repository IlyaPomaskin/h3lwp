@file:JvmName("Lwjgl3Launcher")

package com.homm3.livewallpaper.lwjgl3

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.core.WallpaperPreferences
import com.homm3.livewallpaper.core.install.BundledMapSource
import com.homm3.livewallpaper.core.install.GameDataInstaller
import com.homm3.livewallpaper.core.install.InstallResult
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.io.InputStream
import java.util.logging.Logger
import kotlin.concurrent.thread

private val log: Logger = Logger.getLogger("Lwjgl3Launcher")

/** Resolves `data/...` source paths relative to the JVM cwd (where the user
 *  launched the app from). We don't touch `user.dir` — desktop writes are
 *  redirected via [DesktopFiles] inside [DesktopLwjgl3Application]. */
private val CWD: File = File("").absoluteFile

/** Launches the desktop (LWJGL3) application. */
fun main(args: Array<String>) {
    // This handles macOS support and helps on Windows.
    if (StartupHelper.startNewJvmIfRequired())
        return

    log.info("cwd = ${CWD.absolutePath}")
    log.info("assets-desktop = ${CWD.resolve("assets-desktop").absolutePath}")

    val parsedArgs = parseArgs(args)
    installFiles(parsedArgs)

    DesktopLwjgl3Application(
        Engine(
            prefs = MutableStateFlow(WallpaperPreferences(brightness = 1.0f)),
            onSettingsButtonClick = ::copyLodFile,
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

private const val LOD_SOURCE = "data/H3sprite.lod"

/** Created on use: [Gdx.files] only exists once the application has started. */
private fun createInstaller() = GameDataInstaller(
    dataDir = File(Gdx.files.localStoragePath),
    tempDir = File(System.getProperty("java.io.tmpdir")),
    bundledMaps = InternalBundledMaps(),
)

private class InternalBundledMaps : BundledMapSource {
    private val folder get() = Gdx.files.internal(AssetPaths.USER_MAPS_FOLDER)

    override fun list(): List<String> = folder.list().map { it.name() }

    override fun open(fileName: String): InputStream = folder.child(fileName).read()
}

private fun copyLodFile(onProgress: (String) -> Unit, onDone: () -> Unit) {
    val lodFile = CWD.resolve(LOD_SOURCE)
    log.info("copyLodFile: source=${lodFile.absolutePath} exists=${lodFile.exists()}")
    if (!lodFile.exists()) {
        onProgress("$LOD_SOURCE not found")
        return
    }

    thread(isDaemon = true) {
        val result = createInstaller().installBaseLod(lodFile.inputStream(), onProgress)
        reportResult(result, onProgress, onDone)
    }
}

private fun reportResult(result: InstallResult, onProgress: (String) -> Unit, onDone: () -> Unit) {
    onProgress(result.message)
    if (result is InstallResult.Success) onDone()
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
    val src = File(srcPath).let { if (it.isAbsolute) it else CWD.resolve(srcPath) }
    require(src.exists()) { "File not found: $srcPath" }
    val dest = File(destPath)
    dest.parentFile?.mkdirs()
    if (src.canonicalPath != dest.canonicalPath) {
        src.copyTo(dest, overwrite = true)
    }
}
