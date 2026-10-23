package com.homm3.livewallpaper.android.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.InputStream

internal fun Context.openInputStream(uri: Uri): InputStream? =
    runCatching { contentResolver.openInputStream(uri) }.getOrNull()

internal fun Context.queryDisplayName(uri: Uri): String? =
    runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val hasName = nameIndex >= 0 && cursor.moveToFirst()
            if (hasName) cursor.getString(nameIndex) else null
        }
    }.getOrNull()
