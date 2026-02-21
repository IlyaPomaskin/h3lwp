package com.homm3.livewallpaper.android.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsCategory(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text,
        modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall
    )
}
