package com.homm3.livewallpaper.android

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.parser.h3m.H3mReader
import com.homm3.livewallpaper.parser.h3m.H3mVersion
import android.provider.OpenableColumns
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream
import kotlin.concurrent.thread

class MapsActivity : ComponentActivity() {

    private var mapFiles = mutableStateListOf<File>()
    private var statusMessage by mutableStateOf("")
    private var isUploading by mutableStateOf(false)

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        isUploading = true
        statusMessage = ""

        thread {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: run {
                        runOnUiThread {
                            statusMessage = "Failed to open file"
                            isUploading = false
                        }
                        return@thread
                    }

                val bytes = inputStream.use { it.readBytes() }

                // Step 1: Check magic header (version) after GZIP decompression
                val decompressed = GZIPInputStream(bytes.inputStream()).use { it.readBytes() }
                if (decompressed.size < 4) {
                    runOnUiThread {
                        statusMessage = "File is too small to be a valid map"
                        isUploading = false
                    }
                    return@thread
                }
                val versionInt = ByteBuffer.wrap(decompressed, 0, 4)
                    .order(ByteOrder.LITTLE_ENDIAN).int
                try {
                    H3mVersion.fromInt(versionInt)
                } catch (e: IllegalArgumentException) {
                    runOnUiThread {
                        statusMessage = "Unknown map version: 0x${versionInt.toString(16)}"
                        isUploading = false
                    }
                    return@thread
                }

                // Step 2: Full map parsing validation
                H3mReader(bytes.inputStream()).read()

                val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
                } ?: "map.h3m"
                val safeName = if (fileName.endsWith(".h3m", ignoreCase = true)) fileName else "$fileName.h3m"
                val mapsDir = filesDir.resolve(AssetPaths.USER_MAPS_FOLDER)
                mapsDir.mkdirs()
                val destFile = mapsDir.resolve(safeName)
                destFile.writeBytes(bytes)

                runOnUiThread {
                    if (mapFiles.none { it.name == destFile.name }) {
                        mapFiles.add(destFile)
                    }
                    statusMessage = ""
                    isUploading = false
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusMessage = "Invalid map file: ${e.message}"
                    isUploading = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mapsDir = filesDir.resolve(AssetPaths.USER_MAPS_FOLDER)
        mapsDir.mkdirs()
        mapFiles.addAll(
            mapsDir.listFiles { file -> file.extension.equals("h3m", ignoreCase = true) }
                ?.sortedBy { it.name }
                ?: emptyList()
        )

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) darkColorScheme() else lightColorScheme()
            }
            MaterialTheme(colorScheme = colorScheme) {
                MapsScreen(
                    mapFiles = mapFiles,
                    isUploading = isUploading,
                    statusMessage = statusMessage,
                    onAddMap = { filePickerLauncher.launch("application/octet-stream") },
                    onDeleteMap = ::deleteMap,
                )
            }
        }
    }

    private fun deleteMap(file: File) {
        file.delete()
        mapFiles.remove(file)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapsScreen(
    mapFiles: List<File>,
    isUploading: Boolean,
    statusMessage: String,
    onAddMap: () -> Unit,
    onDeleteMap: (File) -> Unit,
) {
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text(stringResource(R.string.maps_title)) },
            text = { Text(stringResource(R.string.maps_delete_confirm, file.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMap(file)
                    fileToDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.maps_title)) })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMap,
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.maps_add_button))
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            if (isUploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (mapFiles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.maps_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(mapFiles, key = { it.name }) { file ->
                        MapFileItem(
                            file = file,
                            canDelete = mapFiles.size > 1,
                            onDelete = { fileToDelete = file },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapFileItem(
    file: File,
    canDelete: Boolean,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(file.name) },
        trailingContent = {
            IconButton(
                onClick = onDelete,
                enabled = canDelete,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                )
            }
        },
    )
}
