package com.homm3.livewallpaper.android

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.homm3.livewallpaper.android.data.MapsViewModel
import com.homm3.livewallpaper.android.data.MapsViewModelFactory
import com.homm3.livewallpaper.android.data.WallpaperPreferencesRepository
import com.homm3.livewallpaper.android.data.dataStore
import com.homm3.livewallpaper.android.ui.SettingsViewModel
import com.homm3.livewallpaper.android.ui.components.NavigationHost


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

        val mapsViewModel: MapsViewModel by viewModels {
            MapsViewModelFactory(contentResolver, filesDir)
        }

        val settingsViewModel = SettingsViewModel(
            application,
            WallpaperPreferencesRepository(dataStore)
        )

        setContent {
            NavigationHost(
                mapViewModel = mapsViewModel,
                settingsViewModel = settingsViewModel,
                onSetWallpaperClick = { setWallpaper() }
            )
        }
    }
}

