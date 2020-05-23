package com.homm3.livewallpaper.android

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import com.badlogic.gdx.utils.GdxNativesLoader
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.core.Assets
import com.homm3.livewallpaper.core.Constants
import com.homm3.livewallpaper.parser.AssetsConverter
import java.io.File
import java.io.InputStream
import kotlin.concurrent.thread


class SettingsActivity : AppCompatActivity() {
    companion object {
        const val PICK_FILE_RESULT_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = Constants.Preferences.PREFERENCES_NAME
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            // TODO check files existence
            val isAssetsReady = preferenceManager
                .sharedPreferences
                .getBoolean(Constants.Preferences.IS_ASSETS_READY_KEY, false)

            findPreference<Preference>("select_file")?.let {
                if (isAssetsReady) {
                    it.isVisible = false
                } else {
                    it.summary = Constants.INSTRUCTIONS
                    it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
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
                        true
                    }
                }
            }

            findPreference<Preference>("wallpaper_change")?.let {
                it.isVisible = isAssetsReady
                it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    startActivity(Intent()
                        .setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                        .putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(requireContext(), LiveWallpaperService::class.java)
                        )
                    )
                    true
                }
            }

            findPreference<MultiSelectListPreference>("maps")?.let {
                val files = requireContext()
                    .assets
                    .list("maps")

                it.entries = files
                it.entryValues = files
            }

            findPreference<Preference>("credits_button")?.let {
                it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    startActivity(Intent(context, CreditsActivity::class.java))
                    true
                }
            }
        }

        override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
            if (key == Constants.Preferences.IS_ASSETS_READY_KEY) {
                val isAssetsReady = preferenceManager
                    .sharedPreferences
                    .getBoolean(Constants.Preferences.IS_ASSETS_READY_KEY, false)

                findPreference<Preference>("select_file")?.isEnabled = !isAssetsReady
                findPreference<Preference>("wallpaper_change")?.isVisible = isAssetsReady
            }

            println("change: $key : ${preferences?.getString(key ?: "", "default")}")
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
            super.onActivityResult(requestCode, resultCode, intent)

            if (requestCode == PICK_FILE_RESULT_CODE
                && resultCode == Activity.RESULT_OK
                && intent?.data != null) {

                handleFileSelection(intent.data!!)
            }
        }

        private fun updateSelectFilePreference(block: (parsingStatus: Preference) -> Unit) {
            activity?.runOnUiThread {
                findPreference<Preference>("select_file")?.apply(block)
            }
        }

        private fun prepareFileStream(uri: Uri): InputStream {
            return requireContext()
                .contentResolver
                .runCatching { openInputStream(uri) }
                .getOrElse { throw Exception("Can't open file. Check app permissions.") }
        }

        private fun prepareOutputDirectory(path: String): File {
            return requireContext()
                .filesDir
                .resolve(path)
                .runCatching {
                    if (this.exists()) {
                        this.deleteRecursively()
                    }
                    this.mkdirs()
                    this
                }
                .getOrElse { throw Exception("Can't prepare output directory. Check free space.") }
        }

        private fun setAssetsReadyFlag(value: Boolean) {
            preferenceManager
                .sharedPreferences
                .edit()
                .putBoolean(Constants.Preferences.IS_ASSETS_READY_KEY, value)
                .apply()
        }

        private fun handleFileSelection(filePath: Uri) {
            GdxNativesLoader.load()

            thread {
                try {
                    updateSelectFilePreference {
                        it.summary = "Parsing...\nCan take few minutes"
                        it.isSelectable = false
                    }
                    AssetsConverter(
                        prepareFileStream(filePath),
                        prepareOutputDirectory(Assets.atlasFolder),
                        Assets.atlasName
                    ).convertLodToTextureAtlas()
                    setAssetsReadyFlag(true)
                    updateSelectFilePreference { it.summary = "Parsing successfully done!" }
                    context?.sendBroadcast(Intent()
                        .setAction(context?.packageName)
                        .putExtra(LiveWallpaperService.PARSING_DONE_MESSAGE, true))
                } catch (ex: Exception) {
                    setAssetsReadyFlag(false)
                    updateSelectFilePreference {
                        it.summary = ex.message
                        it.isSelectable = true
                    }
                }
            }
        }
    }
}