package com.homm3.livewallpaper.core.install

import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.parser.inno.InnoSetupExtractor
import java.io.File
import java.io.InputStream
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Installs the game data the wallpaper reads into [dataDir]: H3sprite.lod with the bundled maps, and HotA.lod
 * with the maps from the HotA installer. Blocking; call off the UI and GL threads.
 */
class GameDataInstaller(
    private val dataDir: File,
    private val tempDir: File,
    private val bundledMaps: BundledMapSource,
) {
    private val mapsDir: File get() = dataDir.resolve(AssetPaths.USER_MAPS_FOLDER)

    fun installBaseLod(source: InputStream, progress: InstallProgress): InstallResult {
        val lodFile = dataDir.resolve(AssetPaths.LOD_FILE)
        try {
            dataDir.mkdirs()
            progress.report("Copying file...")
            source.use { input -> lodFile.outputStream().use { output -> input.copyTo(output) } }

            progress.report("Validating ${AssetPaths.LOD_FILE}...")
            val error = lodFile.inputStream().use { LodValidator.validate(it, isHota = false) }
            if (error != null) {
                log.warning("installBaseLod: validation failed: $error")
                lodFile.delete()
                return InstallResult.Failure(error)
            }

            copyBundledMaps()
            return InstallResult.Success(DONE_MESSAGE)
        } catch (e: Exception) {
            log.log(Level.SEVERE, "installBaseLod failed", e)
            return InstallResult.Failure("Error: ${e.message}")
        }
    }

    /** Extracts HotA.lod and the campaign maps from [installer]; [extractInstallerMaps] also extracts its .h3m files. */
    fun installHota(installer: File, extractInstallerMaps: Boolean, progress: InstallProgress): InstallResult {
        val hotaLodFile = dataDir.resolve(AssetPaths.HOTA_LOD_FILE)
        val lngFile = File.createTempFile(LNG_TEMP_PREFIX, LOD_EXTENSION, tempDir)
        try {
            mapsDir.mkdirs()
            progress.report("Extracting installer...")
            val targets = hotaTargets(hotaLodFile, lngFile, extractInstallerMaps)
            InnoSetupExtractor.extract(installer, targets) { written, total, _ ->
                progress.report("Extracting: ${percent(written, total)}%")
            }
            log.info("installHota: extracted hotaLod=${hotaLodFile.length()}B lng=${lngFile.length()}B")

            progress.report("Validating ${AssetPaths.HOTA_LOD_FILE}...")
            val error = hotaLodFile.inputStream().use { LodValidator.validate(it, isHota = true) }
            if (error != null) {
                log.warning("installHota: validation failed: $error")
                hotaLodFile.delete()
                return InstallResult.Failure(error)
            }

            val isLngExtracted = lngFile.length() > 0
            if (isLngExtracted) installCampaignMaps(lngFile, progress)
            return InstallResult.Success(DONE_MESSAGE)
        } catch (e: Exception) {
            log.log(Level.SEVERE, "installHota failed", e)
            hotaLodFile.delete()
            return InstallResult.Failure("Error: ${e.message}")
        } finally {
            lngFile.delete()
        }
    }

    private fun hotaTargets(hotaLodFile: File, lngFile: File, extractInstallerMaps: Boolean): List<InnoSetupExtractor.Target> {
        val lodTargets = listOf(
            InnoSetupExtractor.Target(
                matcher = { it.endsWith(INSTALLER_HOTA_LOD, ignoreCase = true) },
                outputFor = { hotaLodFile },
            ),
            InnoSetupExtractor.Target(
                matcher = { it.endsWith(INSTALLER_HOTA_LNG_LOD, ignoreCase = true) },
                outputFor = { lngFile },
            ),
        )
        if (!extractInstallerMaps) return lodTargets

        val mapTarget = InnoSetupExtractor.Target(
            matcher = { it.endsWith(MAP_EXTENSION, ignoreCase = true) },
            outputFor = { destination -> mapsDir.resolve(sanitizeFileName(baseName(destination))) },
        )
        return lodTargets + mapTarget
    }

    private fun installCampaignMaps(lngFile: File, progress: InstallProgress) {
        progress.report("Extracting campaigns...")
        val result = CampaignMapInstaller.installFromLod(lngFile, mapsDir) { current, total, message ->
            progress.report("$message ($current/$total)")
        }
        progress.report("Extracted ${result.mapsWritten} maps from ${result.campaignsFound} campaigns")
    }

    private fun copyBundledMaps() {
        mapsDir.mkdirs()
        bundledMaps.list()
            .filter { it.endsWith(MAP_EXTENSION, ignoreCase = true) }
            .map { name -> name to mapsDir.resolve(name) }
            .filterNot { (_, target) -> target.exists() }
            .forEach { (name, target) ->
                bundledMaps.open(name).use { input -> target.outputStream().use { output -> input.copyTo(output) } }
                log.info("copyBundledMaps: wrote ${target.absolutePath} (${target.length()}B)")
            }
    }

    companion object {
        private val log = Logger.getLogger(GameDataInstaller::class.java.name)

        private const val DONE_MESSAGE = "Done!"
        private const val INSTALLER_HOTA_LOD = "data\\hota.lod"
        private const val INSTALLER_HOTA_LNG_LOD = "data\\hota_lng.lod"
        private const val LNG_TEMP_PREFIX = "HotA_lng_"
        private const val LOD_EXTENSION = ".lod"
        private const val MAP_EXTENSION = ".h3m"
    }
}

private val illegalFileNameChars = setOf(':', '/', '\\', '?', '*', '<', '>', '|', '"')
private const val FALLBACK_MAP_NAME = "unnamed.h3m"
private const val FIRST_PRINTABLE_CHAR = 0x20
private const val PERCENT = 100L

private fun percent(written: Long, total: Long): Int {
    if (total <= 0) return 0
    return (written * PERCENT / total).toInt()
}

private fun baseName(installerPath: String): String = installerPath.substringAfterLast('\\').substringAfterLast('/')

internal fun sanitizeFileName(name: String): String {
    val cleaned = name.trim().map { char ->
        val isIllegal = char.code < FIRST_PRINTABLE_CHAR || char in illegalFileNameChars
        if (isIllegal) '_' else char
    }.joinToString("")
    return cleaned.ifBlank { FALLBACK_MAP_NAME }
}
