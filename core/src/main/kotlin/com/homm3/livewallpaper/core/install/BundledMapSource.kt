package com.homm3.livewallpaper.core.install

import java.io.InputStream

/** Maps shipped inside the app package, copied to the maps folder on first install. */
interface BundledMapSource {
    /** File names of the bundled maps. */
    fun list(): List<String>

    fun open(fileName: String): InputStream
}
