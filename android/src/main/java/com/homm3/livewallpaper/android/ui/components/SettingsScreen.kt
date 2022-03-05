package com.homm3.livewallpaper.android.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.android.ui.ParsingState
import com.homm3.livewallpaper.android.ui.SettingsViewModel
import com.homm3.livewallpaper.android.ui.components.settings.SettingsCategory
import com.homm3.livewallpaper.android.ui.components.settings.SettingsDropdown
import com.homm3.livewallpaper.android.ui.components.settings.SettingsDropdownItem
import com.homm3.livewallpaper.android.ui.theme.H3lwpnextTheme
import com.homm3.livewallpaper.core.MapUpdateInterval
import com.homm3.livewallpaper.core.Scale
import com.homm3.livewallpaper.core.WallpaperPreferences

class GetMultipleFiles : ActivityResultContracts.GetMultipleContents() {
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
fun createFileSelector(onSelect: (uri: Uri) -> Unit): () -> Unit {
    val context = LocalContext.current
    val filesSelector =
        rememberLauncherForActivityResult(GetMultipleFiles()) { list -> onSelect(list[0]) }

    val requestPermission = permissionGrant {
        if (it) {
            filesSelector.launch("")
        } else {
            Toast.makeText(context, "give access to files", Toast.LENGTH_LONG).show()
        }
    }

    return fun() {
        val hasPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            filesSelector.launch("")
        } else {
            requestPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}

fun showDeliveryStatus(state: ParsingState): String {
    return when (state) {
        is ParsingState.Initial -> "Initial"
        is ParsingState.InProgress -> "In progress"
        is ParsingState.Error -> "Error ${state.ex.message}"
        is ParsingState.Done -> "Done"
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSetWallpaperClick: () -> Unit,
    actions: NavigationActions
) {
    val prefs by viewModel.settingsUiModel.observeAsState(WallpaperPreferences())
    val openFileSelector = createFileSelector { viewModel.parseFile(it) }

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

    val parseStatus = showDeliveryStatus(viewModel.parsingStateUiModel)

    var brightnessSliderValue by remember { mutableStateOf(prefs.brightness) }

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
            item {
                SettingsItem(title = "Parse: $parseStatus", onClick = openFileSelector)
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
                        value = brightnessSliderValue,
                        valueRange = 0f..1f,
                        onValueChange = { brightnessSliderValue = it },
                        onValueChangeFinished = { viewModel.setBrightness(brightnessSliderValue) }
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