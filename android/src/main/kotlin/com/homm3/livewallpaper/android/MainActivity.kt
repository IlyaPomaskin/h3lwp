package com.homm3.livewallpaper.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isBaseInstalled = appContainer.gameDataRepository.status.value.isBaseInstalled
        val startIntent = if (isBaseInstalled) {
            SettingsActivity.intent(this)
        } else {
            AssetSetupActivity.intent(this, isInitialSetup = true)
        }
        startActivity(startIntent)
        finish()
    }

    companion object {
        fun newTaskIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
