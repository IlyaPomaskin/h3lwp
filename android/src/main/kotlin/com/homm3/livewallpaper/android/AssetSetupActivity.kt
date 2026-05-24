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
import com.homm3.livewallpaper.core.assets.CampaignMapInstaller
import com.homm3.livewallpaper.core.assets.LodValidator
import com.homm3.livewallpaper.parser.inno.InnoSetupExtractor
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

                runOnUiThread { statusMessage = "Copying file..." }
                val outputFile = filesDir.resolve(AssetPaths.LOD_FILE)
                inputStream.use { input ->
                    outputFile.outputStream().use { output -> input.copyTo(output) }
                }

                runOnUiThread { statusMessage = "Validating H3sprite.lod..." }
                val error = LodValidator.validate(outputFile.inputStream(), isHota = false)
                if (error != null) {
                    outputFile.delete()
                    runOnUiThread {
                        statusMessage = error
                        isConverting = false
                    }
                    return@thread
                }

                copyBundledMaps()

                runOnUiThread {
                    statusMessage = "Done!"
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

    private fun sanitizeBasename(name: String): String {
        val illegal = setOf(':', '/', '\\', '?', '*', '<', '>', '|', '"')
        val cleaned = buildString(name.length) {
            for (c in name.trim()) {
                if (c.code < 0x20 || c in illegal) append('_') else append(c)
            }
        }
        return cleaned.ifBlank { "unnamed.h3m" }
    }

    private val hotaFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        isHotaConverting = true
        hotaStatusMessage = ""

        thread {
            val installerCache = cacheDir.resolve("hota_installer.exe")
            val lngCache = cacheDir.resolve("HotA_lng.lod")
            val hotaLodOut = filesDir.resolve(AssetPaths.HOTA_LOD_FILE)
            val userMapsDir = filesDir.resolve(AssetPaths.USER_MAPS_FOLDER).apply { mkdirs() }
            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: run {
                        runOnUiThread {
                            hotaStatusMessage = "Failed to open file"
                            isHotaConverting = false
                        }
                        return@thread
                    }

                runOnUiThread { hotaStatusMessage = "Copying installer..." }
                inputStream.use { input ->
                    installerCache.outputStream().use { output -> input.copyTo(output) }
                }

                runOnUiThread { hotaStatusMessage = "Extracting installer..." }
                val targets = listOf(
                    InnoSetupExtractor.Target(
                        matcher = { it.endsWith("data\\hota.lod", ignoreCase = true) },
                        outputFor = { _ -> hotaLodOut },
                    ),
                    InnoSetupExtractor.Target(
                        matcher = { it.endsWith("data\\hota_lng.lod", ignoreCase = true) },
                        outputFor = { _ -> lngCache },
                    ),
                    InnoSetupExtractor.Target(
                        matcher = {
                            it.contains("\\app\\maps\\", ignoreCase = true) &&
                                it.endsWith(".h3m", ignoreCase = true)
                        },
                        outputFor = { dest ->
                            val basename = dest.substringAfterLast('\\').substringAfterLast('/')
                            userMapsDir.resolve(sanitizeBasename(basename))
                        },
                    ),
                )
                InnoSetupExtractor.extract(installerCache, targets) { written, total, _ ->
                    val pct = if (total > 0) ((written * 100L) / total).toInt() else 0
                    runOnUiThread { hotaStatusMessage = "Extracting: $pct%" }
                }

                runOnUiThread { hotaStatusMessage = "Validating HotA.lod..." }
                val error = LodValidator.validate(hotaLodOut.inputStream(), isHota = true)
                if (error != null) {
                    hotaLodOut.delete()
                    runOnUiThread {
                        hotaStatusMessage = error
                        isHotaConverting = false
                    }
                    return@thread
                }

                if (lngCache.isFile) {
                    runOnUiThread { hotaStatusMessage = "Extracting campaigns..." }
                    val r = CampaignMapInstaller.installFromLod(lngCache, userMapsDir) { i, total, msg ->
                        runOnUiThread { hotaStatusMessage = "$msg ($i/$total)" }
                    }
                    runOnUiThread {
                        hotaStatusMessage = "Extracted ${r.mapsWritten} maps from ${r.campaignsFound} campaigns"
                    }
                }

                runOnUiThread {
                    hotaStatusMessage = "Done!"
                    isHotaConverting = false
                }
            } catch (e: Exception) {
                hotaLodOut.delete()
                runOnUiThread {
                    hotaStatusMessage = "Error: ${e.message}"
                    isHotaConverting = false
                }
            } finally {
                installerCache.delete()
                lngCache.delete()
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
