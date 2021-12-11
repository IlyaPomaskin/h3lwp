package com.homm3.livewallpaper.android.ui.components

import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.android.data.MapUpdateInterval
import com.homm3.livewallpaper.android.data.Scale
import com.homm3.livewallpaper.android.data.WallpaperPreferences
import com.homm3.livewallpaper.android.ui.SettingsViewModel
import com.homm3.livewallpaper.android.ui.components.settings.SettingsCategory
import com.homm3.livewallpaper.android.ui.components.settings.SettingsDropdown
import com.homm3.livewallpaper.android.ui.components.settings.SettingsDropdownItem
import com.homm3.livewallpaper.android.ui.theme.H3lwpnextTheme

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSetWallpaperClick: () -> Unit,
    actions: NavigationActions
) {
    val prefs = viewModel.settingsUiModel.observeAsState(WallpaperPreferences()).value

    val scaleOptions = listOf(
        SettingsDropdownItem(Scale.DPI, stringResource(R.string.scale_by_density)),
        SettingsDropdownItem(Scale.X1, "x1"),
        SettingsDropdownItem(Scale.X2, "x2"),
        SettingsDropdownItem(Scale.X3, "x3"),
        SettingsDropdownItem(Scale.X4, "x4"),
    )

    val mapUpdateIntervalOptions = listOf(
        SettingsDropdownItem(
            MapUpdateInterval.EVERY_SWITCH,
            stringResource(R.string.update_timeout_every_switch)
        ),
        SettingsDropdownItem(
            MapUpdateInterval.MINUTES_10,
            stringResource(R.string.update_timeout_10minutes)
        ),
        SettingsDropdownItem(
            MapUpdateInterval.MINUTES_30,
            stringResource(R.string.update_timeout_30minutes)
        ),
        SettingsDropdownItem(
            MapUpdateInterval.HOURS_2,
            stringResource(R.string.update_timeout_2hour)
        ),
        SettingsDropdownItem(
            MapUpdateInterval.HOURS_24,
            stringResource(R.string.update_timeout_24hours)
        ),
    )

    H3lwpnextTheme {
        SettingsContainer {
            item { SettingsCategory(text = "Wallpaper") }
            item {
                SettingsItem(
                    title = stringResource(id = R.string.wallpaper_change_button),
                    onClick = { onSetWallpaperClick() }
                )
            }
            item {
                SettingsItem(
                    title = stringResource(id = R.string.maps_button),
                    onClick = { actions.maps() }
                )
            }

            item { SettingsCategory(text = "Preferences") }
            item {
                SettingsDropdown(
                    title = stringResource(id = R.string.scale_title),
                    subtitle = scaleOptions.find { it.value == prefs.scale }?.title.orEmpty(),
                    items = scaleOptions,
                    selectedItemValue = prefs.scale,
                    onItemSelected = { viewModel.setScale(it.value) },
                )
            }
            item {
                SettingsDropdown(
                    title = stringResource(id = R.string.update_time_title),
                    subtitle = mapUpdateIntervalOptions.find { it.value == prefs.mapUpdateInterval }?.title.orEmpty(),
                    items = mapUpdateIntervalOptions,
                    selectedItemValue = prefs.mapUpdateInterval,
                    onItemSelected = { viewModel.setMapUpdateInterval(it.value) },
                )
            }
            item {
                SettingsItem(
                    title = stringResource(id = R.string.use_scroll_title),
                    subtitle = stringResource(id = R.string.use_scroll_summary),
                    onClick = { viewModel.toggleUseScroll() },
                ) { interactionSource ->
                    Switch(
                        checked = prefs.useScroll,
                        onCheckedChange = { viewModel.toggleUseScroll() },
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
                        value = prefs.brightness,
                        valueRange = 0f..1f,
                        onValueChange = { viewModel.setBrightness(it) }
                    )
                }
            }

            item { SettingsCategory(text = "Credits") }
            item {
                SettingsItem(
                    title = stringResource(id = R.string.credits_button),
                    onClick = { actions.about() },
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Preview() {
    SettingsPreview()
}