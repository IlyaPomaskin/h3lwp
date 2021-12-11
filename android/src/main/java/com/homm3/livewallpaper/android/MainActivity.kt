package com.homm3.livewallpaper.android

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.datastore.preferences.preferencesDataStore
import com.homm3.livewallpaper.android.data.WallpaperPreferencesRepository
import com.homm3.livewallpaper.android.ui.SettingsViewModel
import com.homm3.livewallpaper.android.ui.components.NavigationHost

private const val PREFERENCES_NAME = "wallpaper_preferences"

private val Context.dataStore by preferencesDataStore(
    name = PREFERENCES_NAME,
//    produceMigrations = { context ->
//        // Since we're migrating from SharedPreferences, add a migration based on the
//        // SharedPreferences name
//        listOf(SharedPreferencesMigration(context, PREFERENCES_NAME))
//    }
)

class MainActivity : ComponentActivity() {
   /* override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntAr
        val isReadExternalPermission = requestCode == READ_EXTERNAL_STORAGE_RESULT_CODE
        val isGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

        if (isReadExternalPermission)
    {
        if (isGranted) {
            showFileSelectionDialog()
        } else {
            Toast
                .makeText(requireContext(), R.string.assets_permission_canceled, Toast.LENGTH_LONG)
                .show()
        }
    }
}*/

/*private fun handleFileParse(filePath: Uri) {
    GdxNativesLoader.load()

    thread {
        try {
            updateSelectFilePreference {
                it.summary = getString(R.string.assets_parsing_progress)
                it.isSelectable = false
            }
            AssetsConverter(
                prepareFileStream(filePath),
                prepareOutputDirectory(Constants.Assets.ATLAS_FOLDER),
                Constants.Assets.ATLAS_NAME
            ).convertLodToTextureAtlas()
            setAssetsReadyFlag(true)
            updateSelectFilePreference {
                it.summary = getString(R.string.assets_parsing_done)
            }
        } catch (ex: Exception) {
            val errorMessage = when (ex) {
                is InvalidFileException -> getString(R.string.invalid_file_error)
                is OutputFileWriteException -> getString(R.string.output_error)
                else -> getString(R.string.common_error)
            }
            setAssetsReadyFlag(false)
            updateSelectFilePreference {
                it.summary = errorMessage
                it.isSelectable = true
            }
        }
    }
}*/

/*private fun selectFileToParse() {
    if (isAssetsReady()) {
        it.isVisible = false
    } else {
        it.summary = ResourceBundle
            .getBundle("assets/${Constants.Assets.I18N_PATH}")
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
}*/

private fun setWallpaper() {
    startActivity(
        Intent()
            .setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(
                    applicationContext,
                    LiveWallpaperService::class.java
                )
            )
    )
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val settingsViewModel = SettingsViewModel(WallpaperPreferencesRepository(dataStore))

    setContent {
        NavigationHost(
            viewModel = settingsViewModel,
            onSetWallpaperClick = { setWallpaper() }
        )
    }
}
}