package com.homm3.livewallpaper.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.homm3.livewallpaper.core.AssetPaths

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val atlasFile = filesDir.resolve(AssetPaths.ATLAS_PATH)
        val target = if (atlasFile.exists()) {
            SettingsActivity::class.java
        } else {
            AssetSetupActivity::class.java
        }

        startActivity(Intent(this, target))
        finish()
    }
}
