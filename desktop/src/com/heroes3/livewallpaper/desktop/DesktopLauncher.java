package com.heroes3.livewallpaper.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import com.heroes3.livewallpaper.clojure.LiveWallpaperEngine;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.height = 1136;
		config.width = 640;

		new LwjglApplication(new LiveWallpaperEngine(), config);
	}
}
