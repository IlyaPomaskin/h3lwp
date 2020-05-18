package com.homm3.livewallpaper.android

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
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
        private lateinit var sharedPreferences: SharedPreferences

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            sharedPreferences = requireActivity()
                .getSharedPreferences(Constants.Preferences.PREFERENCES_NAME, Context.MODE_PRIVATE)

            updateAssetsButtons()

            findPreference<Preference>("select_file")?.let {
                val isAssetsReady = sharedPreferences.getBoolean(Constants.Preferences.IS_ASSETS_READY_KEY, false)
                if (isAssetsReady) {
                    it.isVisible = false
                } else {
                    it.summary = Constants.INSTRUCTIONS
                }
            }

            findPreference<SeekBarPreference>("update_timeout")?.let {
                it.summaryProvider = Preference.SummaryProvider<SeekBarPreference> { pref ->
                    if (pref.value > 0) {
                        String.format("Every %d minutes", pref.value)
                    } else {
                        String.format("Every switch to home screen")
                    }
                }
                it.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { _, newValue ->
                        val nextValue = newValue.toString().toIntOrNull()
                            ?: Constants.Preferences.DEFAULT_MAP_UPDATE_INTERVAL
                        setPreferenceValue(Constants.Preferences.MAP_UPDATE_INTERVAL, nextValue)
                        it.value = nextValue
                        true
                    }
                it.value = sharedPreferences.getInt(Constants.Preferences.MAP_UPDATE_INTERVAL, Constants.Preferences.DEFAULT_MAP_UPDATE_INTERVAL)
            }

            findPreference<DropDownPreference>("scale")?.let {
                it.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                    setPreferenceValue(Constants.Preferences.SCALE, newValue.toString())
                    true
                }
                it.value = sharedPreferences.getString(Constants.Preferences.SCALE, Constants.Preferences.DEFAULT_SCALE)
            }
        }

        private fun updateAssetsButtons() {
            val isAssetsReady = sharedPreferences.getBoolean(Constants.Preferences.IS_ASSETS_READY_KEY, false)

            findPreference<Preference>("select_file")?.let {
                if (isAssetsReady) {
                    it.isEnabled = false
                }
            }

            findPreference<Preference>("wallpaper_change")?.let {
                it.isVisible = isAssetsReady
            }
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

            if (preference?.key == "wallpaper_change") {
                startActivity(Intent()
                    .setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                    .putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(requireContext(), LiveWallpaperService::class.java)
                    )
                )

                return true
            }

            if (preference?.key == "credits_button") {
                startActivity(Intent(context, CreditsActivity::class.java))

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

        private fun setStatus(block: (parsingStatus: Preference) -> Unit) {
            val preference = findPreference<Preference>("select_file")
            requireActivity().runOnUiThread { preference?.apply(block) }
        }

        private fun clearOutputDirectory(outputDirectory: File) {
            if (outputDirectory.exists()) {
                outputDirectory.deleteRecursively()
                outputDirectory.delete()
            }
            outputDirectory.mkdirs()
        }

        private fun setPreferenceValue(name: String, value: Any) {
            sharedPreferences
                .edit()
                .let {
                    when (value) {
                        is Long -> it.putLong(name, value)
                        is Float -> it.putFloat(name, value)
                        is Int -> it.putInt(name, value)
                        is Boolean -> it.putBoolean(name, value)
                        is String -> it.putString(name, value)
                        else -> throw Error("Not supported value type")
                    }
                }
                .apply()
        }

        private fun sendParsingDoneMessage() {
            val intent = Intent()
                .setAction(context?.packageName)
                .putExtra("parsingDone", true)
            context?.sendBroadcast(intent)
        }

        private fun setAssetsReadyFlag(value: Boolean) {
            setPreferenceValue(Constants.Preferences.IS_ASSETS_READY_KEY, value)
            Handler(context?.mainLooper).post { updateAssetsButtons() }
        }

        private fun handleFileSelection(filePath: Uri) {
            GdxNativesLoader.load()

            thread {
                var stream: InputStream? = null
                var outputDirectory: File? = null
                try {
                    setStatus {
                        it.summary = "Parsing...\nCan take few minutes"
                        it.isSelectable = false
                    }
                    kotlin
                        .runCatching {
                            stream = requireContext()
                                .contentResolver
                                .openInputStream(filePath)
                        }
                        .onFailure { throw Exception("Can't open file. Check app permissions.") }
                        .mapCatching {
                            outputDirectory = requireContext()
                                .filesDir
                                .resolve(Assets.atlasFolder)
                                .also(::clearOutputDirectory)
                            setAssetsReadyFlag(false)
                        }
                        .onFailure { throw Exception("Can't prepare output directory. Check free space.") }
                        .map { AssetsConverter(stream!!, outputDirectory!!, Assets.atlasName).convertLodToTextureAtlas() }
                        .map {
                            setAssetsReadyFlag(true)
                            setStatus { it.summary = "Parsing successfully done!" }
                            sendParsingDoneMessage()
                        }
                } catch (ex: Exception) {
                    outputDirectory?.run(::clearOutputDirectory)
                    setAssetsReadyFlag(false)
                    setStatus {
                        it.summary = ex.message
                        it.isSelectable = true
                    }
                } finally {
                    stream?.close()
                }
            }
        }

        override fun onSharedPreferenceChanged(p0: SharedPreferences?, key: String?) {
            p0?.all?.forEach {
                println("Pref ${it.key} value ${it.value.toString()}")
            }
        }
    }
}