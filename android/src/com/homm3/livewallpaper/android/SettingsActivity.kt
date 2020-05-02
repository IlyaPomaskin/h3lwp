package com.homm3.livewallpaper.android

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.badlogic.gdx.utils.GdxNativesLoader
import com.homm3.livewallpaper.R
import com.homm3.livewallpaper.core.Assets
import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.parser.AssetsConverter
import java.io.*
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

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

                measureTimeMillis {
                    handleFileSelection(intent.data!!)
                }.run { println("Parsing done for $this ms") }
            }
        }

        private fun setStatus(block: (parsingStatus: Preference) -> Unit) {
            val preference = findPreference<Preference>("select_file")
            requireActivity().runOnUiThread { preference?.apply(block) }
        }

        private fun clearOutputDirectory(outputDirectory: File) {
            if (outputDirectory.exists()) {
                outputDirectory.deleteRecursively()
            }
            outputDirectory.mkdirs()
        }

        private fun setAssetsReadyFlag(value: Boolean) {
            activity
                ?.getSharedPreferences(Engine.PREFERENCES_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putBoolean(Engine.IS_ASSETS_READY_KEY, value)
                ?.apply()
        }

        private fun handleFileSelection(filePath: Uri) {
            GdxNativesLoader.load()

            thread {
                var stream: InputStream? = null
                var outputDirectory: File? = null
                try {
                    setStatus {
                        it.summary = "Parsing..."
                        it.isSelectable = false
                    }
                    kotlin
                        .runCatching {
                            stream = requireContext()
                                .contentResolver
                                .openInputStream(filePath)!!
                        }
                        .onFailure { throw Exception("Can't open file") }
                        .mapCatching {
                            outputDirectory = requireContext()
                                .filesDir
                                .resolve(Assets.atlasFolder)
                                .also { clearOutputDirectory(it) }
                            setAssetsReadyFlag(false)
                        }
                        .onFailure { throw Exception("Can't prepare output directory") }
                        .map { AssetsConverter(stream!!, outputDirectory!!, Assets.atlasName).convertLodToTextureAtlas() }
                        .map {
                            setAssetsReadyFlag(true)
                            setStatus { it.summary = "Parsing successfully done!" }
                        }
                } catch (ex: Exception) {
                    outputDirectory?.run { clearOutputDirectory(this) }
                    setAssetsReadyFlag(false)
                    setStatus { it.summary = ex.message }
                } finally {
                    setStatus { it.isSelectable = true }
                    stream?.close()
                }
            }
        }

        companion object {
            const val PICK_FILE_RESULT_CODE = 1
        }
    }
}