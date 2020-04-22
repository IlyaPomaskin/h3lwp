package com.heroes3.livewallpaper.android

import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService

class LiveWallpaper : AndroidLiveWallpaperService() {
    /*private var app: LiveWallpaperEngine? = null
    private val instance = this
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val filePath = intent.getStringExtra(INTENT_EXTRA_NAME)
            println("SELECTED FILE PATH: $filePath")
            app.selectFile(filePath)
        }
    }*/

    override fun onCreateEngine(): Engine {
        return AndroidWallpaperEngine()
    }

    /*protected fun checkStoragePermission() {
        val hasStoragePermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (!hasStoragePermission) {
            app.setStoragePermissionStatus(false)
        }
    }*/

    override fun onCreateApplication() {
        super.onCreateApplication()

        /*app = LiveWallpaperEngine()
        app.setFileSelectHandler(Runnable {
            val intent = Intent(instance, FileSelectorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        })
        app.setEditPermissionHandler(Runnable {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
            checkStoragePermission()
        })
        registerReceiver(receiver, IntentFilter(INTENT_NAME))*/

        val config = AndroidApplicationConfiguration()
        config.useAccelerometer = false
        config.useCompass = false
        initialize(com.heroes3.livewallpaper.core.Engine(), config)
    }

    /*override fun onDestroy() {
        unregisterReceiver(receiver)
        super.onDestroy()
    }*/

    companion object {
        var INTENT_NAME = "selectedFile"
        var INTENT_EXTRA_NAME = "message"
    }
}