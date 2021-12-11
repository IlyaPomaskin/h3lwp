package com.homm3.livewallpaper.android.ui.components

import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.android.ui.components.settings.*
import com.homm3.livewallpaper.android.ui.theme.H3lwpnextTheme

@Preview
@Composable
fun SettingsPreview() {
    H3lwpnextTheme {
        val scaleOptions = stringArrayResource(id = R.array.scale_values)
            .zip(stringArrayResource(id = R.array.scale_entries))
            .map { SettingsDropdownItem(it.first, it.second) }
        var selectedScale by remember { mutableStateOf(scaleOptions[0]) }

        val updateIntervalOptions = stringArrayResource(id = R.array.update_timeout_values)
            .zip(stringArrayResource(id = R.array.update_timeout_entries))
            .map { SettingsDropdownItem(it.first, it.second) }
        var selectedUpdateInterval by remember { mutableStateOf(updateIntervalOptions[0]) }

        var useScroll by remember { mutableStateOf(false) }

        var sliderValue by remember { mutableStateOf(0f) }

        SettingsContainer {
            item { SettingsCategory(text = "Wallpaper") }
            item {
                SettingsItem(
                    title = stringResource(id = R.string.wallpaper_change_button),
                    onClick = { println("set wallpaper") }
                )
            }

            item { SettingsCategory(text = "Preferences") }
            item {
                SettingsDropdown(
                    title = stringResource(id = R.string.scale_title),
                    subtitle = selectedScale.title,
                    items = scaleOptions,
                    selectedItemValue = selectedScale.value,
                    onItemSelected = { selectedScale = it },
                )
            }
            item {
                SettingsDropdown(
                    title = stringResource(id = R.string.update_time_title),
                    subtitle = selectedUpdateInterval.title,
                    items = updateIntervalOptions,
                    selectedItemValue = selectedUpdateInterval.value,
                    onItemSelected = { selectedUpdateInterval = it },
                )
            }
            item {
                SettingsItem(
                    title = stringResource(id = R.string.use_scroll_title),
                    subtitle = stringResource(id = R.string.use_scroll_summary),
                    onClick = { useScroll = !useScroll },
                ) { interactionSource ->
                    Switch(
                        checked = useScroll,
                        onCheckedChange = { useScroll = !useScroll },
                        interactionSource = interactionSource
                    )
                }
            }
            item {
                SettingsItem(
                    title = stringResource(id = R.string.brightness_title),
                    nextLine = true,
                    onClick = { },
                ) {
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = { sliderValue = it }
                    )
                }
            }

            item { SettingsCategory(text = "Credits") }
            item {
                SettingsItem(
                    title = stringResource(id = R.string.credits_button),
                    onClick = { },
                )
            }
        }
    }
}