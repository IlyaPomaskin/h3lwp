package com.homm3.livewallpaper.android.ui.components

import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.homm3.livewallpaper.android.data.MapReadingException
import com.homm3.livewallpaper.android.data.MapsViewModel
import com.homm3.livewallpaper.android.ui.components.settings.SettingsCategory
import com.homm3.livewallpaper.android.ui.components.settings.SettingsItem
import com.homm3.livewallpaper.android.ui.theme.H3lwpnextTheme

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
    val filesSelector = createFileSelector { file -> viewModel.copyMap(file) }
    val files by viewModel.mapsList.collectAsState()
    val readingError by viewModel.mapReadingError.collectAsState()

    H3lwpnextTheme {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { filesSelector() }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "add map")
                }
            },
        ) {
            MapLoadingErrorAlert(error = readingError, onClose = { viewModel.resetCopyMapError() })

            SettingsContainer {
                item { SettingsCategory(text = "Maps") }

                items(files, key = { it.name }) {
                    DismissListItem(
                        disabled = files.size == 1,
                        onDismiss = { viewModel.removeMap(it.name) }) {
                        SettingsItem(
                            title = it.name,
                            onClick = { actions.mapByName(it.name) }
                        )
                    }
                }
            }
        }
    }
}