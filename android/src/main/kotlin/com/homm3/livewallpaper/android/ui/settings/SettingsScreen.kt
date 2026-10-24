package com.homm3.livewallpaper.android.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.android.appContainer
import com.homm3.livewallpaper.android.ui.components.Preference
import com.homm3.livewallpaper.android.ui.components.PreferenceCategory
import com.homm3.livewallpaper.android.ui.components.PreferenceOption
import com.homm3.livewallpaper.android.ui.components.ListPreference
import com.homm3.livewallpaper.android.ui.components.SliderPreference
import com.homm3.livewallpaper.android.ui.components.SwitchPreference
import com.homm3.livewallpaper.android.ui.theme.H3WallpaperTheme
import com.homm3.livewallpaper.android.wallpaper.openLiveWallpaperPicker
import com.homm3.livewallpaper.core.MapUpdateInterval
import com.homm3.livewallpaper.core.Scale
import com.homm3.livewallpaper.core.WallpaperPreferences
import kotlinx.coroutines.launch

private val BRIGHTNESS_RANGE = 0f..0.99f
private val FAB_CLEARANCE = 88.dp

@Composable
fun SettingsRoute(onOpenMaps: () -> Unit, onOpenAssetSetup: () -> Unit, onOpenLinks: () -> Unit) {
    val context = LocalContext.current
    val viewModel = viewModel { SettingsViewModel(context.appContainer.preferencesRepository) }
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val unsupportedMessage = stringResource(R.string.set_wallpaper_unsupported)

    SettingsScreen(
        preferences = preferences,
        snackbarHostState = snackbarHostState,
        onScaleSelected = viewModel::setScale,
        onMapUpdateIntervalSelected = viewModel::setMapUpdateInterval,
        onUseScrollChanged = viewModel::setUseScroll,
        onBrightnessChanged = viewModel::setBrightness,
        onOpenMaps = onOpenMaps,
        onOpenAssetSetup = onOpenAssetSetup,
        onOpenLinks = onOpenLinks,
        onSetWallpaper = {
            val isPickerOpened = context.openLiveWallpaperPicker()
            if (!isPickerOpened) scope.launch { snackbarHostState.showSnackbar(unsupportedMessage) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: WallpaperPreferences?,
    snackbarHostState: SnackbarHostState,
    onScaleSelected: (Scale) -> Unit,
    onMapUpdateIntervalSelected: (MapUpdateInterval) -> Unit,
    onUseScrollChanged: (Boolean) -> Unit,
    onBrightnessChanged: (Float) -> Unit,
    onOpenMaps: () -> Unit,
    onOpenAssetSetup: () -> Unit,
    onOpenLinks: () -> Unit,
    onSetWallpaper: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var isSetWallpaperDialogShown by rememberSaveable { mutableStateOf(false) }

    if (isSetWallpaperDialogShown) {
        SetWallpaperDialog(
            onConfirm = {
                isSetWallpaperDialogShown = false
                onSetWallpaper()
            },
            onDismiss = { isSetWallpaperDialogShown = false },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.set_wallpaper_button)) },
                icon = { Icon(painterResource(R.drawable.ic_wallpaper), contentDescription = null) },
                onClick = { isSetWallpaperDialogShown = true },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding.withExtraBottom(FAB_CLEARANCE)) {
            if (preferences != null) {
                wallpaperSection(
                    preferences = preferences,
                    onScaleSelected = onScaleSelected,
                    onMapUpdateIntervalSelected = onMapUpdateIntervalSelected,
                    onUseScrollChanged = onUseScrollChanged,
                    onBrightnessChanged = onBrightnessChanged,
                )
            }
            contentSection(onOpenMaps = onOpenMaps, onOpenAssetSetup = onOpenAssetSetup, onOpenLinks = onOpenLinks)
        }
    }
}

private fun LazyListScope.wallpaperSection(
    preferences: WallpaperPreferences,
    onScaleSelected: (Scale) -> Unit,
    onMapUpdateIntervalSelected: (MapUpdateInterval) -> Unit,
    onUseScrollChanged: (Boolean) -> Unit,
    onBrightnessChanged: (Float) -> Unit,
) {
    item { PreferenceCategory(stringResource(R.string.settings_section_wallpaper)) }
    item {
        ListPreference(
            title = stringResource(R.string.scale_title),
            options = Scale.entries.map { PreferenceOption(it, stringResource(R.string.scale_option, it.value)) },
            selected = preferences.scale,
            onSelected = onScaleSelected,
        )
    }
    item {
        ListPreference(
            title = stringResource(R.string.update_interval_title),
            options = MapUpdateInterval.entries.map { PreferenceOption(it, stringResource(it.titleRes)) },
            selected = preferences.mapUpdateInterval,
            onSelected = onMapUpdateIntervalSelected,
        )
    }
    item {
        SwitchPreference(
            title = stringResource(R.string.use_scroll_title),
            summary = stringResource(R.string.use_scroll_summary),
            checked = preferences.useScroll,
            onCheckedChange = onUseScrollChanged,
        )
    }
    item {
        SliderPreference(
            title = stringResource(R.string.brightness_title),
            value = preferences.brightness,
            valueRange = BRIGHTNESS_RANGE,
            onValueChangeFinished = onBrightnessChanged,
        )
    }
}

private fun LazyListScope.contentSection(
    onOpenMaps: () -> Unit,
    onOpenAssetSetup: () -> Unit,
    onOpenLinks: () -> Unit,
) {
    item { PreferenceCategory(stringResource(R.string.settings_section_content)) }
    item {
        NavigationPreference(
            title = stringResource(R.string.maps_title),
            iconRes = R.drawable.ic_map,
            onClick = onOpenMaps,
        )
    }
    item {
        NavigationPreference(
            title = stringResource(R.string.asset_setup_title),
            iconRes = R.drawable.ic_folder,
            onClick = onOpenAssetSetup,
        )
    }
    item {
        NavigationPreference(
            title = stringResource(R.string.settings_section_links),
            iconRes = R.drawable.ic_open_in_new,
            onClick = onOpenLinks,
        )
    }
}

@Composable
private fun NavigationPreference(title: String, iconRes: Int, onClick: () -> Unit) {
    Preference(
        title = title,
        leadingContent = { Icon(painterResource(iconRes), contentDescription = null) },
        trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null) },
        onClick = onClick,
    )
}

private val MapUpdateInterval.titleRes: Int
    get() = when (this) {
        MapUpdateInterval.EVERY_SWITCH -> R.string.update_interval_every_switch
        MapUpdateInterval.MINUTES_10 -> R.string.update_interval_10_minutes
        MapUpdateInterval.MINUTES_30 -> R.string.update_interval_30_minutes
        MapUpdateInterval.HOURS_2 -> R.string.update_interval_2_hours
        MapUpdateInterval.HOURS_24 -> R.string.update_interval_24_hours
    }

@Composable
private fun PaddingValues.withExtraBottom(extra: Dp): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = calculateStartPadding(layoutDirection),
        top = calculateTopPadding(),
        end = calculateEndPadding(layoutDirection),
        bottom = calculateBottomPadding() + extra,
    )
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    H3WallpaperTheme {
        SettingsScreen(
            preferences = WallpaperPreferences(),
            snackbarHostState = remember { SnackbarHostState() },
            onScaleSelected = {},
            onMapUpdateIntervalSelected = {},
            onUseScrollChanged = {},
            onBrightnessChanged = {},
            onOpenMaps = {},
            onOpenAssetSetup = {},
            onOpenLinks = {},
            onSetWallpaper = {},
        )
    }
}
