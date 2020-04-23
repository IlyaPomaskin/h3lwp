package com.heroes3.livewallpaper.android

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.heroes3.livewallpaper.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onPreferenceTreeClick(preference: Preference?): Boolean {
            if (preference?.key == "select_file") {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                    .setType("application/octet-stream")
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    .putExtra(Intent.EXTRA_LOCAL_ONLY, true)

                startActivityForResult(
                    Intent.createChooser(intent, getString(R.string.assets_select_file_activity_title)),
                    PICK_FILE_RESULT_CODE
                )

                return true
            }

            return super.onPreferenceTreeClick(preference)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
            super.onActivityResult(requestCode, resultCode, intent)

            if (requestCode == PICK_FILE_RESULT_CODE
                && resultCode == Activity.RESULT_OK
                && intent?.data != null) {
                handleFileSelection(intent.data!!)
            }
        }

        private fun handleFileSelection(filePath: Uri) {
//            val stream = context?.contentResolver?.openInputStream(filePath)
            println("FILE SELECTED $filePath")

            val parsingProgress = findPreference<Preference>("parsing_progress")
            if (parsingProgress != null) {
                parsingProgress.isVisible = true
                parsingProgress.title = "Parsing..."
                parsingProgress.summary = "85%..."
            }
        }

        companion object {
            const val PICK_FILE_RESULT_CODE = 1
        }
    }
}