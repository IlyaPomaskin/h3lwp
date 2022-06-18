package com.homm3.livewallpaper.android.ui.components

import android.widget.Toast
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val filesSelector = createFileSelector { file -> viewModel.copyMap(file) }
    val files by viewModel.mapsList.collectAsState()
    val readingError by viewModel.mapReadingError.collectAsState()
    val isAddEnabled = files.size < 5
    val isRemoveEnabled = files.size > 1

    H3lwpnextTheme {
        Scaffold(
            floatingActionButton = {
                Fab(
                    disabled = !isAddEnabled,
                    onClick = { filesSelector() },
                    onDisabledClick = {
                        Toast
                            .makeText(
                                context,
                                "Too much maps",
                                Toast.LENGTH_LONG
                            )
                            .show()
                    })
            },
        ) {
            MapLoadingErrorAlert(error = readingError, onClose = { viewModel.resetCopyMapError() })

            SettingsContainer {
                item { SettingsCategory(text = "Maps") }

                items(files, key = { it.name }) {
                    DismissListItem(
                        disabled = !isRemoveEnabled,
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