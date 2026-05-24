@file:JvmName("Lwjgl3Launcher")

package com.homm3.livewallpaper.lwjgl3

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.core.WallpaperPreferences
import com.homm3.livewallpaper.core.assets.CampaignMapInstaller
import com.homm3.livewallpaper.core.assets.LodValidator
import com.homm3.livewallpaper.parser.inno.InnoSetupExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
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

private fun sanitizeBasename(name: String): String {
    val illegal = setOf(':', '/', '\\', '?', '*', '<', '>', '|', '"')
    val cleaned = buildString(name.length) {
        for (c in name.trim()) {
            if (c.code < 0x20 || c in illegal) append('_') else append(c)
        }
    }
    return cleaned.ifBlank { "unnamed.h3m" }
}

private fun copyBundledMaps() {
    val mapsDir = Gdx.files.local(AssetPaths.USER_MAPS_FOLDER).file().apply { mkdirs() }
    val internalHandle = Gdx.files.internal(AssetPaths.USER_MAPS_FOLDER)
    val bundled = internalHandle.list(".h3m")
    log.info("copyBundledMaps: dest=${mapsDir.absolutePath} internalPath=${internalHandle.path()} bundledCount=${bundled.size}")
    for (handle in bundled) {
        val target = mapsDir.resolve(handle.name())
        if (target.exists()) {
            log.fine("copyBundledMaps: skip existing ${target.name}")
            continue
        }
        handle.read().use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        log.info("copyBundledMaps: wrote ${target.absolutePath} (${target.length()}B)")
    }
}

private fun copyLodFile(onProgress: (String) -> Unit, onDone: () -> Unit) {
    val lodFile = CWD.resolve("data/H3sprite.lod")
    log.info("copyLodFile: source=${lodFile.absolutePath} exists=${lodFile.exists()}")
    if (!lodFile.exists()) {
        onProgress("data/H3sprite.lod not found")
        return
    }

    thread(isDaemon = true) {
        val outputFile = Gdx.files.local(AssetPaths.LOD_FILE).file()
        log.info("copyLodFile: dest=${outputFile.absolutePath}")
        try {
            outputFile.parentFile?.mkdirs()
            onProgress("Copying file...")
            lodFile.copyTo(outputFile, overwrite = true)
            log.info("copyLodFile: copied ${outputFile.length()}B → ${outputFile.absolutePath}")

            onProgress("Validating H3sprite.lod...")
            val error = LodValidator.validate(outputFile.inputStream(), isHota = false)
            if (error != null) {
                log.warning("copyLodFile: validation failed: $error")
                outputFile.delete()
                onProgress(error)
                return@thread
            }

            copyBundledMaps()

            log.info("copyLodFile: done")
            onProgress("Done!")
            onDone()
        } catch (e: Exception) {
            log.severe("copyLodFile: failed: ${e.javaClass.name}: ${e.message}")
            onProgress("Error: ${e.message}")
        }
    }
}

private fun copyHotaLodFile(onProgress: (String) -> Unit, onDone: () -> Unit) {
    val installer = CWD.resolve("data/HotA_1.8.0_setup.exe")
    log.info("copyHotaLodFile: source=${installer.absolutePath} exists=${installer.exists()}")
    if (!installer.exists()) {
        onProgress("data/HotA_1.8.0_setup.exe not found")
        return
    }

    thread(isDaemon = true) {
        val hotaLodOut = Gdx.files.local(AssetPaths.HOTA_LOD_FILE).file()
        val userMapsDir = Gdx.files.local(AssetPaths.USER_MAPS_FOLDER).file().apply { mkdirs() }
        val lngCache = File.createTempFile("HotA_lng_", ".lod").apply { deleteOnExit() }
        log.info("copyHotaLodFile: dest hotaLodOut=${hotaLodOut.absolutePath} userMapsDir=${userMapsDir.absolutePath} lngCache=${lngCache.absolutePath}")
        try {
            hotaLodOut.parentFile?.mkdirs()
            onProgress("Extracting installer...")
            val targets = listOf(
                InnoSetupExtractor.Target(
                    matcher = { it.endsWith("data\\hota.lod", ignoreCase = true) },
                    outputFor = { _ -> hotaLodOut },
                ),
                InnoSetupExtractor.Target(
                    matcher = { it.endsWith("data\\hota_lng.lod", ignoreCase = true) },
                    outputFor = { _ -> lngCache },
                ),
             // InnoSetupExtractor.Target(
             //     matcher = {
             //         // it.contains("\\app\\maps\\", ignoreCase = true) &&
             //             it.endsWith(".h3m", ignoreCase = true)
             //     },
             //     outputFor = { dest ->
             //         val basename = dest.substringAfterLast('\\').substringAfterLast('/')
             //         userMapsDir.resolve(sanitizeBasename(basename))
             //     },
             // ),
            )
            InnoSetupExtractor.extract(installer, targets) { written, total, _ ->
                val pct = if (total > 0) ((written * 100L) / total).toInt() else 0
                onProgress("Extracting: $pct%")
            }
            log.info("copyHotaLodFile: extract done — hotaLod=${hotaLodOut.length()}B lngExists=${lngCache.isFile} lng=${if (lngCache.isFile) lngCache.length() else 0L}B")

            onProgress("Validating HotA.lod...")
            val error = LodValidator.validate(hotaLodOut.inputStream(), isHota = true)
            if (error != null) {
                log.warning("copyHotaLodFile: validation failed: $error")
                hotaLodOut.delete()
                onProgress(error)
                return@thread
            }

            if (lngCache.isFile) {
                onProgress("Extracting campaigns...")
                val r = CampaignMapInstaller.installFromLod(lngCache, userMapsDir) { i, total, msg ->
                    onProgress("$msg ($i/$total)")
                }
                log.info("copyHotaLodFile: campaigns done — found=${r.campaignsFound} written=${r.mapsWritten} skipped=${r.skipped}")
                onProgress("Extracted ${r.mapsWritten} maps from ${r.campaignsFound} campaigns")
            }

            log.info("copyHotaLodFile: done")
            onProgress("Done!")
            onDone()
        } catch (e: Exception) {
            log.severe("copyHotaLodFile: failed: ${e.javaClass.name}: ${e.message}")
            hotaLodOut.delete()
            onProgress("Error: ${e.message}")
        } finally {
            lngCache.delete()
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
    val src = File(srcPath).let { if (it.isAbsolute) it else CWD.resolve(srcPath) }
    require(src.exists()) { "File not found: $srcPath" }
    val dest = File(destPath)
    dest.parentFile?.mkdirs()
    if (src.canonicalPath != dest.canonicalPath) {
        src.copyTo(dest, overwrite = true)
    }
}
