package com.homm3.livewallpaper.android.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SettingsContainer(
    content: LazyListScope.() -> Unit
) {
    val columnState = rememberLazyListState()

    Box {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = columnState,
            content = content
        )
    }
}
