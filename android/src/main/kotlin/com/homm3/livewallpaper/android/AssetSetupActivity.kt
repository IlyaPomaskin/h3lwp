package com.homm3.livewallpaper.android

import android.content.Intent
import android.os.Bundle
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
import com.homm3.livewallpaper.parser.atlas.AtlasConverter
import kotlin.concurrent.thread

class AssetSetupActivity : ComponentActivity() {

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        isConverting = true
        statusMessage = ""

        thread {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: run {
                        runOnUiThread {
                            statusMessage = "Failed to open file"
                            isConverting = false
                        }
                        return@thread
                    }

                val outputDir = filesDir.resolve(AssetPaths.ATLAS_FOLDER)
                outputDir.mkdirs()

                val converter = AtlasConverter(inputStream, outputDir, AssetPaths.ATLAS_NAME)
                converter.convert { progress ->
                    runOnUiThread { statusMessage = progress }
                }

                copyBundledMaps()

                runOnUiThread {
                    isConverting = false
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusMessage = "Error: ${e.message}"
                    isConverting = false
                }
            }
        }
    }

    private fun copyBundledMaps() {
        val mapsDir = filesDir.resolve(AssetPaths.USER_MAPS_FOLDER)
        mapsDir.mkdirs()
        val bundled = assets.list(AssetPaths.USER_MAPS_FOLDER) ?: return
        for (name in bundled) {
            if (!name.endsWith(".h3m", ignoreCase = true)) continue
            val target = mapsDir.resolve(name)
            if (target.exists()) continue
            assets.open("${AssetPaths.USER_MAPS_FOLDER}/$name").use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }

    private val hotaFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        isHotaConverting = true
        hotaStatusMessage = ""

        thread {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: run {
                        runOnUiThread {
                            hotaStatusMessage = "Failed to open file"
                            isHotaConverting = false
                        }
                        return@thread
                    }

                val outputDir = filesDir.resolve(AssetPaths.ATLAS_FOLDER)
                outputDir.mkdirs()

                val converter = AtlasConverter(inputStream, outputDir, AssetPaths.HOTA_ATLAS_NAME, minimalDefCount = 0)
                converter.convert { progress ->
                    runOnUiThread { hotaStatusMessage = progress }
                }

                runOnUiThread {
                    isHotaConverting = false
                }
            } catch (e: Exception) {
                runOnUiThread {
                    hotaStatusMessage = "Error: ${e.message}"
                    isHotaConverting = false
                }
            }
        }
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
                    onSelectHotaFile = { hotaFilePickerLauncher.launch("application/octet-stream") }
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
