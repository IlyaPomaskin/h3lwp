package com.homm3.livewallpaper.android.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.homm3.livewallpaper.android.data.MapReadingException
import com.homm3.livewallpaper.android.data.MapsViewModel
import com.homm3.livewallpaper.android.ui.components.settings.SettingsCategory
import com.homm3.livewallpaper.android.ui.theme.H3lwpnextTheme

@Composable
fun permissionGrant(onGrant: (isGranted: Boolean) -> Unit): ManagedActivityResultLauncher<String, Boolean> {
    return rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        onGrant(it)
    }
}

fun handleFabClick(
    context: Context,
    requestPermission: ManagedActivityResultLauncher<String, Boolean>,
    selectFile: ManagedActivityResultLauncher<String, Uri?>
) {
    val hasPermission = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED

    if (hasPermission) {
        selectFile.launch("")
    } else {
        requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

class GetMapFile : ActivityResultContracts.GetContent() {
    override fun createIntent(context: Context, input: String): Intent {
        super.createIntent(context, input)

        return Intent(Intent.ACTION_GET_CONTENT)
            .setType("application/octet-stream")
            .addCategory(Intent.CATEGORY_OPENABLE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
    }
}

@Composable
fun MapLoadingErrorAlert(error: MapReadingException?, onClose: () -> Unit) {
    if (error !== null) {
        AlertDialog(
            title = {
                Text(
                    when (error) {
                        MapReadingException.CantParseMap -> "Can't parse content of map"
                        MapReadingException.CantOpenStream -> "Can't open file"
                        MapReadingException.CantCopyMap -> "Can't copy map"
                    }
                )
            },
            text = {
                Text(
                    when (error) {
                        MapReadingException.CantParseMap -> "Check version of the map. App only can read maps from \"Shadow of the Death\"."
                        MapReadingException.CantOpenStream -> "Check permission for reading files."
                        MapReadingException.CantCopyMap -> "Probably there are already same map or no free space on the phone."
                    }
                )
            },
            confirmButton = {
                Button(onClick = { onClose() }) {
                    Text("Close")
                }
            },
            onDismissRequest = { onClose() }
        )
    }
}

@Composable
fun MapsScreen(viewModel: MapsViewModel, actions: NavigationActions) {
    val files by viewModel.mapsList.collectAsState()
    val readingError by viewModel.mapReadingError.collectAsState()

    val filesSelector = rememberLauncherForActivityResult(GetMapFile()) { file ->
        if (file !== null) {
            viewModel.copyMap(file)
        }
    }

    val context = LocalContext.current
    val requestPermission = permissionGrant {
        if (it) {
            filesSelector.launch("application/octet-stream")
        } else {
            Toast.makeText(context, "give access to files", Toast.LENGTH_LONG).show()
        }
    }

    H3lwpnextTheme {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    handleFabClick(
                        context,
                        requestPermission,
                        filesSelector
                    )
                }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "add map")
                }
            },
        ) {
            MapLoadingErrorAlert(error = readingError, onClose = { viewModel.resetCopyMapError() })

            SettingsContainer {
                item { SettingsCategory(text = "Maps") }

                files.map {
                    item {
                        SettingsItem(
                            title = it.name,
                            onClick = { actions.mapByName(it.name) }
                        )
                    }
                }

                item {
                    SettingsItem(
                        title = "back",
                        onClick = { actions.navigateUp() }
                    )
                }
            }
        }
    }
}