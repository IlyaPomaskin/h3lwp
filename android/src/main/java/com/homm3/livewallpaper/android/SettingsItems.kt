package com.homm3.livewallpaper.android

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    subtitle: String,
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

data class SettingsOptionsItem(
    val key: String,
    val value: String
)

@Composable
fun SettingsOptions(
    title: String,
    subtitle: String,
    items: List<SettingsOptionsItem>,
    selectedItemKey: String,
    onItemSelected: (SettingsOptionsItem) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var dropDownExpanded by remember { mutableStateOf(false) }
    val selectedItem = items.filter { it.key == selectedItemKey }[0]
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
                    Text(item.value)
                }
            }
        }
    }
}

@Preview
@Composable
fun Prev() {
    H3lwpnextTheme {
        var state by remember { mutableStateOf(false) }

        val columnState = rememberLazyListState()
        val themes = listOf(
            SettingsOptionsItem("system", "System default"),
            SettingsOptionsItem("dark", "Dark"),
            SettingsOptionsItem("light", "Light")
        )
        var selectedTheme by remember { mutableStateOf(themes[0]) }
        var sliderValue by remember { mutableStateOf(0f) }

        Scaffold { innerPadding ->
            Box {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    state = columnState,
                    contentPadding = innerPadding
                ) {
                    item { SettingsCategory(text = "Wallpaper") }
                    item {
                        SettingsItem(
                            title = stringResource(id = R.string.wallpaper_change_button),
                            subtitle = "",
                            onClick = { println("set wallpaper") }
                        )
                    }
                    item { SettingsCategory(text = "Preferences") }
                    item {
                        SettingsOptions(
                            title = stringResource(id = R.string.scale_title),
                            subtitle = "TODO current value",
                            items = listOf(
                                SettingsOptionsItem("key1", "key1"),
                                SettingsOptionsItem("key2", "key2"),
                                SettingsOptionsItem("key3", "key3")
                            ),
                            selectedItemKey = "key2",
                            onItemSelected = { println("click") },
                        )
                    }
                    item {
                        SettingsOptions(
                            title = stringResource(id = R.string.update_time_title),
                            subtitle = "TODO current value",
                            items = listOf(
                                SettingsOptionsItem("key1", "key1"),
                                SettingsOptionsItem("key2", "key2"),
                                SettingsOptionsItem("key3", "key3")
                            ),
                            selectedItemKey = "key2",
                            onItemSelected = { println("click") },
                        )
                    }
                    item {
                        SettingsItem(
                            title = stringResource(id = R.string.use_scroll_title),
                            subtitle = stringResource(id = R.string.use_scroll_summary),
                            onClick = { },
                        ) { interactionSource ->
                            Switch(
                                checked = state,
                                onCheckedChange = { state = !state },
                                interactionSource = interactionSource
                            )
                        }
                    }
                    item {
                        SettingsItem(
                            title = stringResource(id = R.string.brightness_title),
                            subtitle = "",
                            nextLine = true,
                            onClick = { },
                        ) {
                            Slider(
                                value = sliderValue,
                                steps = 1,
                                valueRange = 1f..100f,
                                onValueChange = { sliderValue = it })
                        }
                    }
                    item { SettingsCategory(text = "Credits") }

                    item {
                        SettingsItem(
                            title = stringResource(id = R.string.credits_button),
                            subtitle = "",
                            onClick = {  },
                        )
                    }
                }
            }
        }
    }
}