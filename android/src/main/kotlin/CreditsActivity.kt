package com.homm3.livewallpaper.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.homm3.livewallpaper.R

class CreditsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.credits_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.credits, CreditsFragment())
            .commit()
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class CreditsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.credits_preferences, rootKey)
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            if (listOf("source_code", "icon_author", "map_author").contains(preference?.key)) {
                val intent = Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse(preference?.summary.toString()))
                startActivity(intent)

                return true
            }

            return super.onPreferenceTreeClick(preference)
        }

    }
}