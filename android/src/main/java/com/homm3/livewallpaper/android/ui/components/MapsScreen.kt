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
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
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
    selectFile: ManagedActivityResultLauncher<String, List<Uri>>
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

class GetMultipleMapsFiles : ActivityResultContracts.GetMultipleContents() {
    override fun createIntent(context: Context, input: String): Intent {
        super.createIntent(context, input)

        return Intent(Intent.ACTION_GET_CONTENT)
            .setType("application/octet-stream")
            .addCategory(Intent.CATEGORY_OPENABLE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }
}

@Composable
fun MapsScreen(viewModel: MapsViewModel, actions: NavigationActions) {
    val files by viewModel.mapsList.collectAsState()
    val readingError by viewModel.mapReadingError.collectAsState()

    val context = LocalContext.current

    val filesSelector = rememberLauncherForActivityResult(GetMultipleMapsFiles()) { list ->
        viewModel.copyMap(list[0])
    }

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
                    Icon(imageVector = Icons.Default.Add, contentDescription = "asdf")
                }
            },
        ) {
            SettingsContainer {
                item { SettingsCategory(text = "Maps") }

                files.map {
                    item {
                        SettingsItem(
                            title = it.name,
                            onClick = { println(it.absolutePath) }
                        )
                    }
                }

                item {
                    SettingsItem(
                        title = "err",
                        subtitle = when (readingError) {
                            MapReadingException.CantParseMap -> "CantParseMap"
                            MapReadingException.CantOpenStream -> "CantOpenStream"
                            MapReadingException.CantCopyMap -> "CantCopyMap"
                            else -> "unknown"
                        },
                        onClick = { println("set wallpaper") }
                    )
                }

                item {
                    SettingsItem(
                        title = "maps list",
                        onClick = { println("set wallpaper") }
                    )
                }
                item {
                    SettingsItem(
                        title = "open map qwer",
                        onClick = { actions.mapByName("qwer") }
                    )
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