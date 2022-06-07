package com.homm3.livewallpaper.android.ui.components.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun PhoneLimitations() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                textAlign = TextAlign.Center,
                text = "Unfortunately there are some limitations which can't be fixed by app :("
            )
        }

        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                textAlign = TextAlign.Center,
                text = "Most of MIUI and some of Samsung phones don't allow to set live wallpapers on the lock screen"
            )
        }

        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                textAlign = TextAlign.Center,
                text = "And some phones don't allow to set live wallpapers at all"
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PhoneLimitationsPreview() {
    PhoneLimitations()
}