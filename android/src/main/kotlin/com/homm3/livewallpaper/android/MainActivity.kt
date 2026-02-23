package com.homm3.livewallpaper.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.homm3.livewallpaper.core.AssetPaths

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lodFile = filesDir.resolve(AssetPaths.LOD_FILE)
        val target = if (lodFile.exists()) {
            SettingsActivity::class.java
        } else {
            AssetSetupActivity::class.java
        }

        startActivity(Intent(this, target))
        finish()
    }
}
