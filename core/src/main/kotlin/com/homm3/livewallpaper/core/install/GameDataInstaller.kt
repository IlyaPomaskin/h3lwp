package com.homm3.livewallpaper.core.install

import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.core.GameConfig
import com.homm3.livewallpaper.parser.h3m.H3mHeaderReader
import com.homm3.livewallpaper.parser.h3m.H3mVersion
import com.homm3.livewallpaper.parser.inno.InnoSetupExtractor
import java.io.File
import java.io.InputStream
import java.nio.channels.FileChannel
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Installs the game data the wallpaper reads into [dataDir]: H3sprite.lod (as a file, from a StuffIt/ZIP archive
 * or from a GOG installer)
 * with the bundled maps, and HotA.lod
 * with the maps from the HotA installer. Blocking; call off the UI and GL threads.
 */
class GameDataInstaller(
    private val dataDir: File,
    private val tempDir: File,
    private val bundledMaps: BundledMapSource,
) {
    private val mapsDir: File get() = dataDir.resolve(AssetPaths.USER_MAPS_FOLDER)

    /** [source] is H3sprite.lod itself, a StuffIt 5 archive (the Mac demo) or a ZIP holding either. */
    fun installBaseLod(source: InputStream, progress: InstallProgress): InstallResult {
        val lodFile = dataDir.resolve(AssetPaths.LOD_FILE)
        try {
            dataDir.mkdirs()
            progress.report("Copying file...")
            val isLodFound = source.use { input ->
                BaseLodArchive.extract(input, lodFile) { percent -> progress.report("Extracting: $percent%") }
            }
            if (!isLodFound) {
                lodFile.delete()
                return InstallResult.Failure(ARCHIVE_LOD_NOT_FOUND_MESSAGE)
            }
            return finishBaseInstall(lodFile, progress)
        } catch (e: Exception) {
            log.log(Level.SEVERE, "installBaseLod failed", e)
            lodFile.delete()
            return InstallResult.Failure("Error: ${e.message}")
        }
    }

    /**
     * Extracts H3sprite.lod from a GOG offline installer: [setupExe] holds the setup headers and
     * [dataSlices] are its `-1.bin`, `-2.bin`... files in order. The caller owns the channels.
     * Also extracts the original SoD maps (not RoE/AB) with a [AssetPaths.SOD_MAP_PREFIX] name.
     */
    fun installBaseFromGog(setupExe: File, dataSlices: List<FileChannel>, progress: InstallProgress): InstallResult {
        val lodFile = dataDir.resolve(AssetPaths.LOD_FILE)
        try {
            dataDir.mkdirs()
            progress.report("Extracting installer...")
            val existingMaps = mapFileNames()
            val extracted = InnoSetupExtractor.extract(setupExe, gogTargets(lodFile), dataSlices) { written, total, _ ->
                progress.report("Extracting: ${percent(written, total)}%")
            }
            val isLodExtracted = extracted.any { it.output == lodFile }
            if (!isLodExtracted) {
                removeBaseGameMaps()
                return InstallResult.Failure(BASE_LOD_NOT_FOUND_MESSAGE)
            }

            removeNonSodBaseGameMaps()
            val result = finishBaseInstall(lodFile, progress)
            if (result is InstallResult.Failure) {
                removeBaseGameMaps()
                return result
            }
            disableMapsOffByDefault(extracted.map { it.output }, existingMaps)
            return result
        } catch (e: Exception) {
            log.log(Level.SEVERE, "installBaseFromGog failed", e)
            lodFile.delete()
            removeBaseGameMaps()
            return InstallResult.Failure("Error: ${e.message}")
        }
    }

    private fun gogTargets(lodFile: File): List<InnoSetupExtractor.Target> {
        val lodTarget = InnoSetupExtractor.Target(
            matcher = { it.endsWith(INSTALLER_BASE_LOD, ignoreCase = true) },
            outputFor = { lodFile },
        )
        mapsDir.mkdirs()
        val mapTarget = InnoSetupExtractor.Target(
            matcher = { it.endsWith(MAP_EXTENSION, ignoreCase = true) },
            outputFor = { destination -> mapsDir.resolve(sanitizeFileName(AssetPaths.SOD_MAP_PREFIX + baseName(destination))) },
        )
        return listOf(lodTarget, mapTarget)
    }

    private fun removeBaseGameMaps() {
        baseGameMaps().forEach { it.delete() }
    }

    private fun removeNonSodBaseGameMaps() {
        baseGameMaps()
            .filterNot { map -> map.inputStream().use { H3mHeaderReader.readVersion(it) } == H3mVersion.SOD }
            .forEach { it.delete() }
    }

    private fun baseGameMaps(): List<File> =
        mapsDir.listFiles { file -> file.name.startsWith(AssetPaths.SOD_MAP_PREFIX) }.orEmpty().toList()

    private fun finishBaseInstall(lodFile: File, progress: InstallProgress): InstallResult {
        progress.report("Validating ${AssetPaths.LOD_FILE}...")
        val error = LodValidator.validate(lodFile, isHota = false)
        if (error != null) {
            log.warning("finishBaseInstall: validation failed: $error")
            lodFile.delete()
            return InstallResult.Failure(error)
        }

        copyBundledMaps()
        return InstallResult.Success(DONE_MESSAGE)
    }

    /**
     * Extracts HotA.lod from [installer]; [extractInstallerMaps] also extracts its .h3m files (not its campaigns)
     * with a [AssetPaths.HOTA_MAP_PREFIX] name.
     */
    fun installHota(installer: File, extractInstallerMaps: Boolean, progress: InstallProgress): InstallResult {
        val hotaLodFile = dataDir.resolve(AssetPaths.HOTA_LOD_FILE)
        try {
            mapsDir.mkdirs()
            progress.report("Extracting installer...")
            val existingMaps = mapFileNames()
            val targets = hotaTargets(hotaLodFile, extractInstallerMaps)
            val extracted = InnoSetupExtractor.extract(installer, targets) { written, total, _ ->
                progress.report("Extracting: ${percent(written, total)}%")
            }
            log.info("installHota: extracted hotaLod=${hotaLodFile.length()}B")

            progress.report("Validating ${AssetPaths.HOTA_LOD_FILE}...")
            val error = LodValidator.validate(hotaLodFile, isHota = true)
            if (error != null) {
                log.warning("installHota: validation failed: $error")
                hotaLodFile.delete()
                return InstallResult.Failure(error)
            }

            disableMapsOffByDefault(extracted.map { it.output }, existingMaps)
            return InstallResult.Success(DONE_MESSAGE)
        } catch (e: Exception) {
            log.log(Level.SEVERE, "installHota failed", e)
            hotaLodFile.delete()
            return InstallResult.Failure("Error: ${e.message}")
        }
    }

    private fun hotaTargets(hotaLodFile: File, extractInstallerMaps: Boolean): List<InnoSetupExtractor.Target> {
        val lodTarget = InnoSetupExtractor.Target(
            matcher = { it.endsWith(INSTALLER_HOTA_LOD, ignoreCase = true) },
            outputFor = { hotaLodFile },
        )
        if (!extractInstallerMaps) return listOf(lodTarget)

        val mapTarget = InnoSetupExtractor.Target(
            matcher = { it.endsWith(MAP_EXTENSION, ignoreCase = true) },
            outputFor = { destination -> mapsDir.resolve(hotaMapFileName(destination)) },
        )
        return listOf(lodTarget, mapTarget)
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

    private fun mapFileNames(): Set<String> = mapsDir.list().orEmpty().toSet()

    private fun disableMapsOffByDefault(files: List<File>, existingMaps: Set<String>) {
        val offByDefaultMapNames = files
            .distinct()
            .filter { it.isFile && it.name.endsWith(MAP_EXTENSION, ignoreCase = true) && it.name !in existingMaps }
            .filterNot { map -> map.inputStream().use(H3mHeaderReader::readMapSize) in GameConfig.DEFAULT_ENABLED_MAP_SIZES }
            .map { it.name }
        if (offByDefaultMapNames.isEmpty()) return

        val disabledMapsFile = dataDir.resolve(AssetPaths.DISABLED_MAPS_FILE)
        val disabledMaps = readDisabledMaps(disabledMapsFile)
        disabledMapsFile.writeText((disabledMaps + offByDefaultMapNames).distinct().joinToString("\n"))
        log.info("disableMapsOffByDefault: disabled ${offByDefaultMapNames.size} maps")
    }

    private fun readDisabledMaps(file: File): List<String> {
        if (!file.exists()) return emptyList()
        return file.readLines().filter { it.isNotBlank() }
    }

    companion object {
        private val log = Logger.getLogger(GameDataInstaller::class.java.name)

        private const val DONE_MESSAGE = "Done!"
        private const val INSTALLER_BASE_LOD = "data\\h3sprite.lod"
        private const val BASE_LOD_NOT_FOUND_MESSAGE = "H3sprite.lod not found in the installer"
        private const val ARCHIVE_LOD_NOT_FOUND_MESSAGE = "H3sprite.lod not found in the archive"
        private const val INSTALLER_HOTA_LOD = "data\\hota.lod"
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

internal fun baseName(installerPath: String): String = installerPath.substringAfterLast('\\').substringAfterLast('/')

internal fun hotaMapFileName(installerPath: String): String {
    val name = baseName(installerPath)
    val hasPrefix = name.startsWith(AssetPaths.HOTA_MAP_PREFIX)
    if (hasPrefix) return sanitizeFileName(name)
    return sanitizeFileName(AssetPaths.HOTA_MAP_PREFIX + name)
}

internal fun sanitizeFileName(name: String): String {
    val cleaned = name.trim().map { char ->
        val isIllegal = char.code < FIRST_PRINTABLE_CHAR || char in illegalFileNameChars
        if (isIllegal) '_' else char
    }.joinToString("")
    return cleaned.ifBlank { FALLBACK_MAP_NAME }
}
