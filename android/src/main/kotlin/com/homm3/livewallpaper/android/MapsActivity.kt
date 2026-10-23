package com.homm3.livewallpaper.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.homm3.livewallpaper.android.ui.maps.MapsRoute
import com.homm3.livewallpaper.android.ui.setScreenContent

class MapsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setScreenContent {
            MapsRoute(onBack = ::finish)
        }
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, MapsActivity::class.java)
    }
}
