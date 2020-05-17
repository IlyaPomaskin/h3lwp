package com.homm3.livewallpaper.android

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
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

        private fun setAssetsReadyFlag(value: Boolean) {
            activity
                ?.getSharedPreferences(Engine.PREFERENCES_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putBoolean(Engine.IS_ASSETS_READY_KEY, value)
                ?.apply()
        }

        private fun sendParsingDoneMessage() {
            val intent = Intent()
                .setAction(context?.packageName)
                .putExtra("parsingDone", true)
            context?.sendBroadcast(intent)
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
                                .openInputStream(filePath)!!
                        }
                        .onFailure { throw Exception("Can't open file") }
                        .mapCatching {
                            outputDirectory = requireContext()
                                .filesDir
                                .resolve(Assets.atlasFolder)
                                .also(::clearOutputDirectory)
                            setAssetsReadyFlag(false)
                        }
                        .onFailure { throw Exception("Can't prepare output directory") }
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
    }
}