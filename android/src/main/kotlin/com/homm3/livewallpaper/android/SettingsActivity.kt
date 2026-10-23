package com.homm3.livewallpaper.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.homm3.livewallpaper.android.ui.setScreenContent
import com.homm3.livewallpaper.android.ui.settings.SettingsRoute

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setScreenContent {
            SettingsRoute(
                onOpenMaps = { startActivity(MapsActivity.intent(this)) },
                onOpenAssetSetup = { startActivity(AssetSetupActivity.intent(this, isInitialSetup = false)) },
            )
        }
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, SettingsActivity::class.java)
    }
}
