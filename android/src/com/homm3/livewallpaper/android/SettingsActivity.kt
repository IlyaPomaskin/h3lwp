package com.homm3.livewallpaper.android

import android.Manifest
import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.badlogic.gdx.utils.GdxNativesLoader
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.core.Assets
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.DEFAULT_MAP_UPDATE_INTERVAL
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.DEFAULT_SCALE
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.IS_ASSETS_READY_KEY
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.MAP_UPDATE_INTERVAL
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.PREFERENCES_NAME
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.SCALE
import com.homm3.livewallpaper.parser.AssetsConverter
import com.homm3.livewallpaper.parser.InvalidFileException
import com.homm3.livewallpaper.parser.OutputFileWriteException
import java.io.File
import java.io.InputStream
import java.util.*
import kotlin.concurrent.thread


class SettingsActivity : AppCompatActivity() {
    companion object {
        const val ACTION_GET_CONTENT_RESULT_CODE = 1
        const val READ_EXTERNAL_STORAGE_RESULT_CODE = 2
    }

    lateinit var settingsFragment: SettingsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        settingsFragment = SettingsFragment()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, settingsFragment)
            .commit()
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        settingsFragment.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    class SettingsFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener,
        ActivityCompat.OnRequestPermissionsResultCallback {
        private fun convertOldPreferences() {
            // Old float/integer preferences used in <= 2.2.0
            val prefs = preferenceManager.sharedPreferences
            val editor = prefs.edit()

            try {
                prefs
                    .getFloat(MAP_UPDATE_INTERVAL, DEFAULT_MAP_UPDATE_INTERVAL)
                    .also { editor.remove(MAP_UPDATE_INTERVAL).putString(MAP_UPDATE_INTERVAL, it.toInt().toString()) }
            } catch (e: Exception) {
            }

            try {
                prefs
                    .getInt(SCALE, DEFAULT_SCALE)
                    .also { editor.remove(SCALE).putString(SCALE, it.toString()) }
            } catch (e: Exception) {
            }

            editor.commit()
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = PREFERENCES_NAME
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
            convertOldPreferences()

            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            findPreference<Preference>("select_file")?.let {
                if (isAssetsReady()) {
                    it.isVisible = false
                } else {
                    it.summary = ResourceBundle
                        .getBundle("assets/${Assets.i18nPath}")
                        .getString("instructions")
                    it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        val hasReadStoragePermission = ContextCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasReadStoragePermission) {
                            ActivityCompat.requestPermissions(
                                requireActivity(),
                                Array(1) { Manifest.permission.READ_EXTERNAL_STORAGE },
                                READ_EXTERNAL_STORAGE_RESULT_CODE
                            )
                        } else {
                            showFileSelectionDialog()
                        }
                        true
                    }
                }
            }

            findPreference<Preference>("wallpaper_change")?.let {
                it.isVisible = isAssetsReady()
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

        override fun onDestroy() {
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onDestroy()
        }

        override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
            if (key == IS_ASSETS_READY_KEY) {
                findPreference<Preference>("select_file")?.isEnabled = !isAssetsReady()
                findPreference<Preference>("wallpaper_change")?.isVisible = isAssetsReady()
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
            super.onActivityResult(requestCode, resultCode, intent)

            if (requestCode == ACTION_GET_CONTENT_RESULT_CODE
                && resultCode == Activity.RESULT_OK
                && intent?.data != null) {

                handleFileSelection(intent.data!!)
            }
        }

        private fun showFileSelectionDialog() {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
                .setType("application/octet-stream")
                .addCategory(Intent.CATEGORY_OPENABLE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            startActivityForResult(
                Intent.createChooser(intent, getString(R.string.assets_select_file_activity_title)),
                ACTION_GET_CONTENT_RESULT_CODE
            )
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
            val isReadExternalPermission = requestCode == READ_EXTERNAL_STORAGE_RESULT_CODE
            val isGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

            if (isReadExternalPermission) {
                if (isGranted) {
                    showFileSelectionDialog()
                } else {
                    Toast
                        .makeText(requireContext(), R.string.assets_permission_canceled, Toast.LENGTH_LONG)
                        .show()
                }
            }
        }

        private fun isAssetsReady(): Boolean {
            // TODO check files existence
            return preferenceManager
                .sharedPreferences
                .getBoolean(IS_ASSETS_READY_KEY, false)
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
                .getOrElse { throw Exception(getString(R.string.file_open_error)) }
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
                .getOrElse { throw Exception(getString(R.string.output_error)) }
        }

        private fun setAssetsReadyFlag(value: Boolean) {
            preferenceManager
                .sharedPreferences
                .edit()
                .putBoolean(IS_ASSETS_READY_KEY, value)
                .apply()
        }

        private fun handleFileSelection(filePath: Uri) {
            GdxNativesLoader.load()

            thread {
                try {
                    updateSelectFilePreference {
                        it.summary = getString(R.string.assets_parsing_progress)
                        it.isSelectable = false
                    }
                    AssetsConverter(
                        prepareFileStream(filePath),
                        prepareOutputDirectory(Assets.atlasFolder),
                        Assets.atlasName
                    ).convertLodToTextureAtlas()
                    setAssetsReadyFlag(true)
                    updateSelectFilePreference { it.summary = getString(R.string.assets_parsing_done) }
                    context?.sendBroadcast(Intent()
                        .setAction(context?.packageName)
                        .putExtra(AndroidEngine.PARSING_DONE_MESSAGE, true))
                } catch (ex: Exception) {
                    val errorMessage = when (ex) {
                        is InvalidFileException -> getString(R.string.invalid_file_error)
                        is OutputFileWriteException  -> getString(R.string.output_error)
                        else -> getString(R.string.common_error)
                    }
                    setAssetsReadyFlag(false)
                    updateSelectFilePreference {
                        it.summary = errorMessage
                        it.isSelectable = true
                    }
                }
            }
        }
    }
}