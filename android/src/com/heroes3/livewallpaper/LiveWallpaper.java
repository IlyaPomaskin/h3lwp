package com.heroes3.livewallpaper;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidLiveWallpaperService;

public class LiveWallpaper extends AndroidLiveWallpaperService {
    @Override
    public void onCreateApplication() {
        super.onCreateApplication();
        initialize(createListener());
    }

    //    @Override
    public ApplicationListener createListener () {
        return new Heroes3LWP();
    }

//    @Override
    public AndroidApplicationConfiguration createConfig () {
        return new AndroidApplicationConfiguration();
    }
}
