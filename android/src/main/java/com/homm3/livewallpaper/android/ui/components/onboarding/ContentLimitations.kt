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
fun ContentLimitations() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Row(Modifier.padding(12.dp)) {
            Text(
                textAlign = TextAlign.Center,
                text = "Due to copyright restrictions app can't use resources from the game"
            )
        }

        Row(Modifier.padding(12.dp)) {
            Text(
                textAlign = TextAlign.Center,
                text = "You will need to provide file \"heroes_3/Data/H3Sprite.lod\" from your copy of Heroes® of Might & Magic® III"
            )
        }

        Row(Modifier.padding(12.dp)) {
            Text(
                textAlign = TextAlign.Center,
                text = "Please note that only original version of the game is supported\nHD Edition will not work"
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ContentLimitationsPreview() {
    ContentLimitations()
}