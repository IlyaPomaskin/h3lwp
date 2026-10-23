package com.homm3.livewallpaper.android

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
}
