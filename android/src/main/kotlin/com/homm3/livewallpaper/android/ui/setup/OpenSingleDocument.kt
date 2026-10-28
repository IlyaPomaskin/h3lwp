package com.homm3.livewallpaper.android.ui.setup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

/**
 * [ActivityResultContracts.OpenDocument] that also reads the picked file from the result's ClipData:
 * some vendor pickers (seen on Xiaomi HyperOS) return it only there, which the stock contract reports as a cancel.
 */
internal class OpenSingleDocument : ActivityResultContract<Array<String>, Uri?>() {
    private val openDocument = ActivityResultContracts.OpenDocument()

    override fun createIntent(context: Context, input: Array<String>): Intent = openDocument.createIntent(context, input)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK || intent == null) return null

        val clipData = intent.clipData
        val clipDataUri = if (clipData != null && clipData.itemCount > 0) clipData.getItemAt(0).uri else null
        return intent.data ?: clipDataUri
    }
}
