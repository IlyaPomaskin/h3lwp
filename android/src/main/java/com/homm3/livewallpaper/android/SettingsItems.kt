package com.homm3.livewallpaper.android

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.badlogic.gdx.scenes.scene2d.actions.Actions.delay
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.android.ui.theme.H3lwpnextTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsCategory(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text,
        modifier
            .padding(start = 16.dp, top = 24.dp, bottom = 8.dp, end = 16.dp),
        color = MaterialTheme.colors.primary,
        style = MaterialTheme.typography.subtitle2
    )
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
    enabled: Boolean = true,
    nextLine: Boolean = false,
    action: @Composable (interactionSource: MutableInteractionSource) -> Unit = {}
) {
    val context = LocalContext.current.resources
    val displayMetrics = context.displayMetrics
    val screenWidth = displayMetrics.widthPixels / displayMetrics.density

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    title,
                    Modifier.width(screenWidth.times(0.8f).dp),
                    style = MaterialTheme.typography.body1,
                    color = if (enabled) Color.Unspecified else MaterialTheme.colors.onSurface.copy(
                        ContentAlpha.disabled
                    )
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        Modifier.width(screenWidth.times(0.8f).dp),
                        style = MaterialTheme.typography.body2,
                        color = if (enabled) MaterialTheme.colors.onSurface.copy(0.5f) else MaterialTheme.colors.onSurface.copy(
                            ContentAlpha.disabled
                        )
                    )
                }
                if (nextLine) {
                    action(interactionSource)
                }
            }

            if (!nextLine) {
                action(interactionSource)
            }
        }
    }
}

data class SettingsDropdownItem(
    val value: String,
    val title: String
)

@Composable
fun SettingsDropdown(
    title: String,
    subtitle: String,
    items: List<SettingsDropdownItem>,
    selectedItemKey: String,
    onItemSelected: (SettingsDropdownItem) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var dropDownExpanded by remember { mutableStateOf(false) }
    val selectedItem = items.filter { it.value == selectedItemKey }[0]
    var selectedItemIndexState by remember { mutableStateOf(items.indexOf(selectedItem)) }
    val scope = rememberCoroutineScope()

    Box {
        SettingsItem(
            title = title,
            subtitle = subtitle,
            enabled = enabled,
            onClick = { dropDownExpanded = !dropDownExpanded }
        )
        DropdownMenu(
            expanded = dropDownExpanded,
            offset = DpOffset(24.dp, 0.dp),
            onDismissRequest = { dropDownExpanded = false },

            ) {
            items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    onClick = {
                        dropDownExpanded = false
                        scope.launch {
                            delay(100F)
                            selectedItemIndexState = index
                            onItemSelected(item)
                        }
                    },
                    Modifier.background(
                        if (selectedItemIndexState == index) MaterialTheme.colors.primary.copy(
                            alpha = 0.3f
                        ) else Color.Unspecified
                    )
                ) {
                    Text(item.title)
                }
            }
        }
    }
}

@Composable
fun ListContainer(
    content: LazyListScope.() -> Unit
) {
    val columnState = rememberLazyListState()

    Scaffold { innerPadding ->
        Box {
            LazyColumn(
                Modifier.fillMaxSize(),
                state = columnState,
                contentPadding = innerPadding,
                content = content
            )
        }
    }
}

@Preview
@Composable
fun Prev() {
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

        ListContainer {
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
                    selectedItemKey = selectedScale.value,
                    onItemSelected = { selectedScale = it },
                )
            }
            item {
                SettingsDropdown(
                    title = stringResource(id = R.string.update_time_title),
                    subtitle = selectedUpdateInterval.title,
                    items = updateIntervalOptions,
                    selectedItemKey = selectedUpdateInterval.value,
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