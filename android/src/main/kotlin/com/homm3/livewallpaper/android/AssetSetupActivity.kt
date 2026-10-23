package com.homm3.livewallpaper.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.homm3.livewallpaper.android.ui.setScreenContent
import com.homm3.livewallpaper.android.ui.setup.AssetSetupRoute

class AssetSetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isInitialSetup = intent.getBooleanExtra(EXTRA_IS_INITIAL_SETUP, false)

        setScreenContent {
            if (isInitialSetup) {
                AssetSetupRoute(onBack = null, onContinue = ::openSettings)
            } else {
                AssetSetupRoute(onBack = ::finish, onContinue = null)
            }
        }
    }

    private fun openSettings() {
        startActivity(SettingsActivity.intent(this))
        finish()
    }

    companion object {
        private const val EXTRA_IS_INITIAL_SETUP = "is_initial_setup"

        fun intent(context: Context, isInitialSetup: Boolean): Intent =
            Intent(context, AssetSetupActivity::class.java).putExtra(EXTRA_IS_INITIAL_SETUP, isInitialSetup)
    }
}
