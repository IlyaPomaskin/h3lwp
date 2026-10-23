package com.homm3.livewallpaper.android.data

import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.core.install.BundledMapSource
import com.homm3.livewallpaper.core.install.GameDataInstaller
import com.homm3.livewallpaper.core.install.InstallProgress
import com.homm3.livewallpaper.core.install.InstallResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream

data class GameDataStatus(
    val isBaseInstalled: Boolean,
    val isHotaInstalled: Boolean,
)

class GameDataRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dataDir = appContext.filesDir

    private val installer by lazy {
        System.loadLibrary(GDX_LIBRARY)
        GameDataInstaller(
            dataDir = dataDir,
            tempDir = appContext.cacheDir,
            bundledMaps = AssetBundledMaps(appContext.assets),
        )
    }

    private val mutableStatus = MutableStateFlow(readStatus())
    val status: StateFlow<GameDataStatus> = mutableStatus.asStateFlow()

    suspend fun installBase(uri: Uri, progress: InstallProgress): InstallResult = withContext(Dispatchers.IO) {
        val input = appContext.openInputStream(uri) ?: return@withContext InstallResult.Failure(FAILED_TO_OPEN_MESSAGE)
        try {
            installer.installBaseLod(input, progress)
        } finally {
            refreshStatus()
        }
    }

    suspend fun installHota(uri: Uri, progress: InstallProgress): InstallResult = withContext(Dispatchers.IO) {
        val input = appContext.openInputStream(uri) ?: return@withContext InstallResult.Failure(FAILED_TO_OPEN_MESSAGE)
        val installerCopy = appContext.cacheDir.resolve(HOTA_INSTALLER_CACHE_NAME)
        try {
            progress.report(COPYING_INSTALLER_MESSAGE)
            input.use { source -> installerCopy.outputStream().use { source.copyTo(it) } }
            installer.installHota(installerCopy, extractInstallerMaps = true, progress = progress)
        } catch (e: IOException) {
            InstallResult.Failure("Error: ${e.message}")
        } finally {
            installerCopy.delete()
            refreshStatus()
        }
    }

    private fun refreshStatus() {
        mutableStatus.value = readStatus()
    }

    private fun readStatus() = GameDataStatus(
        isBaseInstalled = dataDir.resolve(AssetPaths.LOD_FILE).exists(),
        isHotaInstalled = dataDir.resolve(AssetPaths.HOTA_LOD_FILE).exists(),
    )

    private class AssetBundledMaps(private val assets: AssetManager) : BundledMapSource {
        override fun list(): List<String> = assets.list(AssetPaths.USER_MAPS_FOLDER)?.toList().orEmpty()

        override fun open(fileName: String): InputStream = assets.open("${AssetPaths.USER_MAPS_FOLDER}/$fileName")
    }

    private companion object {
        const val GDX_LIBRARY = "gdx"
        const val HOTA_INSTALLER_CACHE_NAME = "hota_installer.exe"
        const val FAILED_TO_OPEN_MESSAGE = "Failed to open file"
        const val COPYING_INSTALLER_MESSAGE = "Copying installer..."
    }
}
