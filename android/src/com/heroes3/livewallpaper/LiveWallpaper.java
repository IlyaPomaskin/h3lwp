package com.heroes3.livewallpaper;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService;

public class LiveWallpaper extends AndroidLiveWallpaperService {
    @Override
    public void onCreateApplication() {
        super.onCreateApplication();
        initialize(createListener());
    }

    public ApplicationListener createListener () {
        return new h3m.LwpCore();
    }

    public AndroidApplicationConfiguration createConfig () {
        return new AndroidApplicationConfiguration();
    }
}
