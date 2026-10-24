package com.homm3.livewallpaper.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.homm3.livewallpaper.android.ui.links.LinksScreen
import com.homm3.livewallpaper.android.ui.setScreenContent

class LinksActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setScreenContent {
            LinksScreen(onBack = ::finish)
        }
    }

    companion object {
        fun intent(context: Context): Intent = Intent(context, LinksActivity::class.java)
    }
}
