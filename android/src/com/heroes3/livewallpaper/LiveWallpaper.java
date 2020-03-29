package com.heroes3.livewallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService;
import com.heroes3.livewallpaper.clojure.LiveWallpaperEngine;

public class LiveWallpaper extends AndroidLiveWallpaperService {
    static String INTENT_NAME = "selectedFile";
    static String INTENT_EXTRA_NAME = "message";
    private LiveWallpaperEngine app;
    private LiveWallpaper instance = this;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String filePath = intent.getStringExtra(INTENT_EXTRA_NAME);
            app.selectFile(filePath);
        }
    };

    public Engine onCreateEngine() {
        return new AndroidWallpaperEngine();
    }

    @Override
    public void onCreateApplication() {
        super.onCreateApplication();

        app = new LiveWallpaperEngine();
        app.setFileSelectHandler(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(instance, FileSelectorActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        LocalBroadcastManager
            .getInstance(this)
            .registerReceiver(receiver, new IntentFilter(INTENT_NAME));

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;

        initialize(app, config);
    }
}
