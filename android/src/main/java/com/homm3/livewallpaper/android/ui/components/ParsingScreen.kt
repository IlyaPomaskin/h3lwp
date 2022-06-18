package com.homm3.livewallpaper.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.homm3.livewallpaper.android.data.ParsingState
import com.homm3.livewallpaper.android.data.ParsingViewModel
import com.homm3.livewallpaper.android.ui.theme.H3lwpnextTheme

inline fun <R : Any> AnnotatedString.Builder.boldText(
    crossinline block: AnnotatedString.Builder.() -> R
): R {
    val index = pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
    return try {
        block(this)
    } finally {
        pop(index)
    }
}

@Composable
fun ParsingScreen(viewModel: ParsingViewModel, actions: NavigationActions) {
    val openFileSelector = createFileSelector { viewModel.parseFile(it) }
    val parseStatus = viewModel.parsingStateUiModel
    val isButtonEnabled = parseStatus === ParsingState.Initial

    H3lwpnextTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
                text = "At first you need to upload file with images from original game"
            )

            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
                text = " Due to copyright restrictions we can't distribute this file with the app"
            )

            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
                text = buildAnnotatedString {
                    append("Upload file ")
                    boldText { append("'Heroes 3/Data/H3Sprites.lod'") }
                    append(" to phone from your copy of ")
                    boldText { append("Heroes of Might and Magic® III Shadow of the Death") }
                    append(". Also you can upload your favorite maps.")
                }
            )

            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .alpha(0.6f),
                text = "Please note that 'HD version', 'The Restoration of Erathia', 'Armageddon blade' and 'WoG' will not work. HotA should be OK."
            )

            Button(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .defaultMinSize(minWidth = 40.dp),
                enabled = isButtonEnabled,
                onClick = {
                    if (isButtonEnabled) {
                        openFileSelector()
                    }
                }) {
                when (parseStatus) {
                    ParsingState.Initial -> {
                        Text(
                            textAlign = TextAlign.Center,
                            text = "Select H3Sprites.lod"
                        )
                    }
                    ParsingState.InProgress -> {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    ParsingState.Done -> {
                        LaunchedEffect(key1 = "done", block = { actions.phoneLimitations() })
                    }
                    ParsingState.Error -> {
                        AlertDialog(
                            title = { Text("Can't parse file") },
                            text = {
                                Text("Check file permissions or make sure you are using file from Shadow of the Death")
                            },
                            confirmButton = {
                                Button(onClick = { viewModel.clearParsingError() }) {
                                    Text("Close")
                                }
                            },
                            onDismissRequest = { viewModel.clearParsingError() }
                        )
                    }
                }
            }
        }
    }
}