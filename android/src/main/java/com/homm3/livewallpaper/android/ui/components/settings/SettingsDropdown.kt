package com.homm3.livewallpaper.android.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import kotlinx.coroutines.launch

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
                            Actions.delay(100F)
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