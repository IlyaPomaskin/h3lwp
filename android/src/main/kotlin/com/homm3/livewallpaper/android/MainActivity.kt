package com.homm3.livewallpaper.android

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.badlogic.gdx.utils.GdxNativesLoader
import com.homm3.livewallpaper.parser.atlas.AtlasConverter
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private lateinit var statusText: TextView
    private lateinit var selectButton: Button

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

                val outputDir = filesDir.resolve("sprites")
                outputDir.mkdirs()

                val converter = AtlasConverter(inputStream, outputDir, "sprites")
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

        GdxNativesLoader.load()

        statusText = findViewById(R.id.tv_status)
        selectButton = findViewById(R.id.btn_select_lod)
        selectButton.setOnClickListener {
            filePickerLauncher.launch("application/octet-stream")
        }
    }
}
