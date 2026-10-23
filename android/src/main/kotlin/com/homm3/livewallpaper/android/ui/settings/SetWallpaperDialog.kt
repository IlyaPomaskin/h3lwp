package com.homm3.livewallpaper.android.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.homm3.livewallpaper.R
import kotlinx.coroutines.delay

private const val CONFIRM_DELAY_SECONDS = 5
private const val SECOND_MILLIS = 1_000L

@Composable
fun SetWallpaperDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var secondsLeft by rememberSaveable { mutableIntStateOf(CONFIRM_DELAY_SECONDS) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(SECOND_MILLIS)
            secondsLeft--
        }
    }

    val isConfirmEnabled = secondsLeft == 0
    val confirmText = if (isConfirmEnabled) {
        stringResource(R.string.set_wallpaper_dialog_confirm)
    } else {
        stringResource(R.string.set_wallpaper_dialog_confirm_countdown, secondsLeft)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.set_wallpaper_dialog_title)) },
        text = { Text(stringResource(R.string.set_wallpaper_dialog_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = isConfirmEnabled) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
