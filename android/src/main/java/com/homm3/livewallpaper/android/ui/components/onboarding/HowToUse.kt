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
fun HowToUse() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Row(Modifier.padding(12.dp)) {
            Text(
                textAlign = TextAlign.Center,
                text = "This app will change your wallpaper to the animated map from Heroes® of Might & Magic® III!"
            )
        }

        Row(Modifier.padding(12.dp)) {
            Text(
                textAlign = TextAlign.Center,
                text = "At first you must provide one file from your bought copy of the game"
            )
        }

        Row(Modifier.padding(12.dp)) {
            Text(
                textAlign = TextAlign.Center,
                text = "Then you can change appearance settings and set a wallpaper!"
            )
        }

        Row(Modifier.padding(12.dp)) {
            Text(
                textAlign = TextAlign.Center,
                text = "Please note that there are few limitations from copyright owners and phone developers"
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HowToUsePreview() {
    HowToUse()
}