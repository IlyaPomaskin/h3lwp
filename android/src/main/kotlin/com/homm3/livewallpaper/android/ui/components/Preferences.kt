package com.homm3.livewallpaper.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.homm3.livewallpaper.R

data class PreferenceOption<T>(val value: T, val title: String)

@Composable
fun PreferenceCategory(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
fun Preference(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ListItem(
        headlineContent = { Text(title) },
        modifier = modifier.then(clickableModifier),
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = leadingContent,
        trailingContent = trailingContent,
    )
}

@Composable
fun SwitchPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    ListItem(
        headlineContent = { Text(title) },
        modifier = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        supportingContent = summary?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    )
}

@Composable
fun <T> ListPreference(
    title: String,
    options: List<PreferenceOption<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDialogShown by rememberSaveable { mutableStateOf(false) }
    val selectedTitle = options.firstOrNull { it.value == selected }?.title

    Preference(
        title = title,
        summary = selectedTitle,
        modifier = modifier,
        onClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        OptionsDialog(
            title = title,
            options = options,
            selected = selected,
            onSelected = {
                isDialogShown = false
                onSelected(it)
            },
            onDismiss = { isDialogShown = false },
        )
    }
}

@Composable
private fun <T> OptionsDialog(
    title: String,
    options: List<PreferenceOption<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { option ->
                    val isSelected = option.value == selected
                    ListItem(
                        headlineContent = { Text(option.title) },
                        modifier = Modifier.selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onSelected(option.value) },
                        ),
                        leadingContent = { RadioButton(selected = isSelected, onClick = null) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun SliderPreference(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draggedValue by remember(value) { mutableFloatStateOf(value) }

    ListItem(
        headlineContent = { Text(title) },
        modifier = modifier,
        supportingContent = {
            Slider(
                value = draggedValue,
                valueRange = valueRange,
                onValueChange = { draggedValue = it },
                onValueChangeFinished = { onValueChangeFinished(draggedValue) },
            )
        },
    )
}
