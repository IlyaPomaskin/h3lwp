package com.homm3.livewallpaper.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

data class SettingsDropdownItem<T>(
    val value: T,
    val title: String
)

@Composable
fun <T> SettingsDropdown(
    title: String,
    subtitle: String,
    items: List<SettingsDropdownItem<T>>,
    selectedItemValue: T,
    onItemSelected: (SettingsDropdownItem<T>) -> Unit,
    enabled: Boolean = true,
) {
    var dropDownExpanded by remember { mutableStateOf(false) }

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
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.title) },
                    onClick = {
                        dropDownExpanded = false
                        onItemSelected(item)
                    },
                    modifier = Modifier.background(
                        if (selectedItemValue == item.value) MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.3f
                        ) else Color.Unspecified
                    )
                )
            }
        }
    }
}
