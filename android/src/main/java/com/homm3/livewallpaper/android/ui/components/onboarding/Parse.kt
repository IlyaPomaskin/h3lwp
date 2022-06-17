package com.homm3.livewallpaper.android.ui.components.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.homm3.livewallpaper.android.ui.OnboardingViewModel
import com.homm3.livewallpaper.android.ui.components.createFileSelector
import com.homm3.livewallpaper.android.ui.components.showParsingState

@Composable
fun Parse(viewModel: OnboardingViewModel) {
    val openFileSelector = createFileSelector { viewModel.parseFile(it) }
    val parseStatus = showParsingState(viewModel.parsingStateUiModel)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Button(onClick = { openFileSelector() }) {
            Text(
                textAlign = TextAlign.Center,
                text = "Parse $parseStatus"
            )
        }
    }
}
