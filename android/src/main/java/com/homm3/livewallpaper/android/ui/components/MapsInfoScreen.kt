package com.homm3.livewallpaper.android.ui.components

import androidx.compose.runtime.Composable
import com.homm3.livewallpaper.android.ui.components.settings.SettingsCategory
import com.homm3.livewallpaper.android.ui.theme.H3lwpnextTheme

@Composable
fun MapsInfoScreen(mapName: String, actions: NavigationActions) {
    H3lwpnextTheme {
        SettingsContainer {
            item { SettingsCategory(text = "Maps") }
            item {
                SettingsItem(
                    title = "map: $mapName",
                    onClick = { println("set wallpaper") }
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