package com.homm3.livewallpaper.core.install

/** Receives human-readable status lines while an install runs; called from the installing thread. */
fun interface InstallProgress {
    fun report(message: String)
}
