package com.heroes3.livewallpaper.android

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.badlogic.gdx.utils.GdxNativesLoader
import com.heroes3.livewallpaper.R
import com.heroes3.livewallpaper.parser.AssetsParser
import java.io.*
import kotlin.concurrent.thread

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

        private fun applyPreference(name: String, block: (parsingStatus: Preference) -> Unit) {
            val preference = findPreference<Preference>(name)
            requireActivity().runOnUiThread { preference?.apply(block) }
        }

        private fun handleFileSelection(filePath: Uri) {
            GdxNativesLoader.load()

            thread {
                var stream: InputStream? = null

                try {
                    applyPreference("select_file") {
                        it.summary = "Parsing..."
                        it.isSelectable = false
                    }

                    stream = requireContext().contentResolver.openInputStream(filePath)!!
                    AssetsParser(stream)
                        .parseLodToAtlas(
                            requireContext().filesDir.resolve("assets/sprites/test/"),
                            "assets"
                        )
                    applyPreference("select_file") { it.summary = "Parsing successfully done!" }
                } catch (e: Exception) {
                    applyPreference("select_file") { it.summary = "Something went wrong!\n${e.message}" }
                } finally {
                    stream?.close()
                    applyPreference("select_file") { it.isSelectable = true }
                }
            }
        }

        companion object {
            const val PICK_FILE_RESULT_CODE = 1
        }
    }
}