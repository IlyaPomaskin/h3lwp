package com.heroes3.livewallpaper.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class FileSelectorActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        val intent = Intent(Intent.ACTION_GET_CONTENT)
            .setType("application/octet-stream")
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        startActivityForResult(
            Intent.createChooser(intent, "Select H3sprite.lod"),
            PICK_FILE_RESULT_CODE
        )
    }

    private fun getFilePath(data: Intent): String {
        val uri = data.data ?: return ""
        val uriPath = uri.path ?: return ""
        return uriPath.substring(uriPath.indexOf(":") + 1)
    }

    private fun sendFilePath(filePath: String?) {
        val intent = Intent(LiveWallpaper.INTENT_NAME)
        intent.putExtra(LiveWallpaper.INTENT_EXTRA_NAME, filePath)
        sendBroadcast(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_RESULT_CODE && resultCode == RESULT_OK) {
            sendFilePath(getFilePath(data))
            finish()
        }
    }

    companion object {
        private const val PICK_FILE_RESULT_CODE = 1
    }
}