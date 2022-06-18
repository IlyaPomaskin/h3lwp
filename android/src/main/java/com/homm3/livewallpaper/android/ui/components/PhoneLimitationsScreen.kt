package com.homm3.livewallpaper.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.homm3.livewallpaper.android.ui.theme.H3lwpnextTheme

@Composable
fun PhoneLimitations(actions: NavigationActions) {
    H3lwpnextTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    textAlign = TextAlign.Center,
                    text = "Please note few things:"
                )
            }

            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    textAlign = TextAlign.Center,
                    text = "Most of the phones with MIUI and some Samsung phones don't allow to set live wallpapers on the lock screen"
                )
            }

            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    textAlign = TextAlign.Center,
                    text = "And some phones don't allow to set live wallpapers at all"
                )
            }

            Button(modifier = Modifier.padding(vertical = 8.dp),
                onClick = { actions.settings }) {
                Text(text = "Okay")
            }
        }
    }
}