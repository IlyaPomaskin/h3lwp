package com.homm3.livewallpaper

import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.core.WallpaperPreferences
import kotlinx.coroutines.flow.MutableStateFlow

fun Main() = Engine(prefs = MutableStateFlow(WallpaperPreferences()))
