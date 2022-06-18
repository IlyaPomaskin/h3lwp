package com.homm3.livewallpaper.android.ui.components

import androidx.compose.runtime.Composable
import com.homm3.livewallpaper.android.ui.components.settings.SettingsCategory
import com.homm3.livewallpaper.android.ui.components.settings.SettingsItem
import com.homm3.livewallpaper.android.ui.theme.H3lwpnextTheme

@Composable
fun AboutScreen(actions: NavigationActions) {
    H3lwpnextTheme {
        SettingsContainer {
            item { SettingsCategory(text = "About") }
            item {
                SettingsItem(
                    title = "test",
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