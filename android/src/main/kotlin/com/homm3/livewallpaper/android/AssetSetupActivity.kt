package com.homm3.livewallpaper.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.os.Build
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.core.install.BundledMapSource
import com.homm3.livewallpaper.core.install.GameDataInstaller
import com.homm3.livewallpaper.core.install.InstallResult
import java.io.File
import java.io.InputStream
import kotlin.concurrent.thread

class AssetSetupActivity : ComponentActivity() {

    private val installer by lazy {
        GameDataInstaller(dataDir = filesDir, tempDir = cacheDir, bundledMaps = AssetBundledMaps())
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        isConverting = true
        statusMessage = ""

        thread {
            val inputStream = runCatching { contentResolver.openInputStream(uri) }.getOrNull()
            if (inputStream == null) {
                showBaseResult(InstallResult.Failure(FAILED_TO_OPEN_MESSAGE))
                return@thread
            }
            val result = installer.installBaseLod(inputStream) { message ->
                runOnUiThread { statusMessage = message }
            }
            showBaseResult(result)
        }
    }

    private val hotaFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        isHotaConverting = true
        hotaStatusMessage = ""

        thread {
            val installerCache = cacheDir.resolve(INSTALLER_CACHE_NAME)
            try {
                val copyFailure = copyToFile(uri, installerCache)
                if (copyFailure != null) {
                    showHotaResult(copyFailure)
                    return@thread
                }
                val result = installer.installHota(installerCache, extractInstallerMaps = true) { message ->
                    runOnUiThread { hotaStatusMessage = message }
                }
                showHotaResult(result)
            } finally {
                installerCache.delete()
            }
        }
    }

    /** Copies [uri] to [target]; returns a failure to show, or `null` once copied. */
    private fun copyToFile(uri: Uri, target: File): InstallResult.Failure? {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return InstallResult.Failure(FAILED_TO_OPEN_MESSAGE)
            runOnUiThread { hotaStatusMessage = "Copying installer..." }
            inputStream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            return null
        } catch (e: Exception) {
            Log.e(HOTA_TAG, "Copying installer failed", e)
            return InstallResult.Failure("Error: ${e.message}")
        }
    }

    private fun showBaseResult(result: InstallResult) = runOnUiThread {
        statusMessage = result.message
        isConverting = false
        if (result is InstallResult.Success) {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
    }

    private fun showHotaResult(result: InstallResult) = runOnUiThread {
        hotaStatusMessage = result.message
        isHotaConverting = false
    }

    private inner class AssetBundledMaps : BundledMapSource {
        override fun list(): List<String> = assets.list(AssetPaths.USER_MAPS_FOLDER)?.toList().orEmpty()

        override fun open(fileName: String): InputStream = assets.open("${AssetPaths.USER_MAPS_FOLDER}/$fileName")
    }

    companion object {
        private const val HOTA_TAG = "HotaSetup"
        private const val INSTALLER_CACHE_NAME = "hota_installer.exe"
        private const val FAILED_TO_OPEN_MESSAGE = "Failed to open file"
    }

    private var isConverting by mutableStateOf(false)
    private var statusMessage by mutableStateOf("")
    private var isHotaConverting by mutableStateOf(false)
    private var hotaStatusMessage by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        System.loadLibrary("gdx")

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) darkColorScheme() else lightColorScheme()
            }
            MaterialTheme(colorScheme = colorScheme) {
                AssetSetupScreen(
                    isConverting = isConverting,
                    statusMessage = statusMessage,
                    onSelectFile = { filePickerLauncher.launch("application/octet-stream") },
                    isHotaConverting = isHotaConverting,
                    hotaStatusMessage = hotaStatusMessage,
                    onSelectHotaFile = {
                        hotaFilePickerLauncher.launch(
                            arrayOf(
                                "application/x-msdownload",
                                "application/x-msdos-program",
                                "application/vnd.microsoft.portable-executable",
                                "application/octet-stream",
                                "application/x-ms-installer",
                                "application/*"
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AssetSetupScreen(
    isConverting: Boolean,
    statusMessage: String,
    onSelectFile: () -> Unit,
    isHotaConverting: Boolean,
    hotaStatusMessage: String,
    onSelectHotaFile: () -> Unit
) {
    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.asset_setup_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = stringResource(R.string.asset_setup_description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = onSelectFile,
                enabled = !isConverting
            ) {
                Text(text = stringResource(R.string.asset_setup_select_button))
            }

            if (isConverting) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            Text(
                text = stringResource(R.string.asset_setup_hota_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = stringResource(R.string.asset_setup_hota_description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = onSelectHotaFile,
                enabled = !isHotaConverting
            ) {
                Text(text = stringResource(R.string.asset_setup_hota_select_button))
            }

            if (isHotaConverting) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (hotaStatusMessage.isNotEmpty()) {
                Text(
                    text = hotaStatusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
