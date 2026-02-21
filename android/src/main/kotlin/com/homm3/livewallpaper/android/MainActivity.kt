package com.homm3.livewallpaper.android

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import com.homm3.livewallpaper.R
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.parser.atlas.AtlasConverter
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private lateinit var statusText: TextView
    private lateinit var selectButton: Button

    private val mapPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        statusText.text = "Copying map..."

        thread {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: run {
                        runOnUiThread { statusText.text = "Failed to open file" }
                        return@thread
                    }

                val mapsDir = filesDir.resolve(AssetPaths.USER_MAPS_FOLDER)
                mapsDir.mkdirs()

                // Extract filename from URI or use a default
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "map.h3m"
                val outputFile = mapsDir.resolve(fileName)

                inputStream.use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                runOnUiThread { statusText.text = "Map uploaded: ${outputFile.name}" }
            } catch (e: Exception) {
                runOnUiThread { statusText.text = "Error: ${e.message}" }
            }
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        selectButton.isEnabled = false
        statusText.text = "Starting conversion..."

        thread {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                    ?: run {
                        runOnUiThread {
                            statusText.text = "Failed to open file"
                            selectButton.isEnabled = true
                        }
                        return@thread
                    }

                val outputDir = filesDir.resolve(AssetPaths.ATLAS_FOLDER)
                outputDir.mkdirs()

                val converter = AtlasConverter(inputStream, outputDir, AssetPaths.ATLAS_NAME)
                converter.convert { progress ->
                    runOnUiThread { statusText.text = progress }
                }

                runOnUiThread {
                    statusText.text = "Done! You can now set the live wallpaper."
                    selectButton.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "Error: ${e.message}"
                    selectButton.isEnabled = true
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        System.loadLibrary("gdx")

        statusText = findViewById(R.id.tv_status)
        selectButton = findViewById(R.id.btn_select_lod)
        selectButton.setOnClickListener {
            filePickerLauncher.launch("application/octet-stream")
        }

        findViewById<Button>(R.id.btn_upload_map).setOnClickListener {
            mapPickerLauncher.launch("application/octet-stream")
        }

        findViewById<Button>(R.id.btn_set_wallpaper).setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, LiveWallpaperService::class.java)
            )
            startActivity(intent)
        }
    }
}
