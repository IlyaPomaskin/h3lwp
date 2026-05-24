package com.homm3.livewallpaper.parser.inno

import org.junit.Assume.assumeTrue
import java.io.File

object TestFixtures {
    private val projectRoot: File by lazy {
        var dir: File? = File(System.getProperty("user.dir"))
        while (dir != null && !File(dir, "data").isDirectory) dir = dir.parentFile
        requireNotNull(dir) { "Cannot locate project root (no data/ folder upwards from ${System.getProperty("user.dir")})" }
    }

    val installer173: File get() = projectRoot.resolve("data/HotA_1.7.3_setup.exe")
    val installer180: File get() = projectRoot.resolve("data/HotA_1.8.0_setup.exe")
    val goldenLod173: File get() = projectRoot.resolve("data/hota.lod")
    val goldenLod180: File get() = projectRoot.resolve("data/hota18.lod")
    val lng180: File get() = projectRoot.resolve("data/HotA_lng.lod")

    fun requireLng180() {
        assumeTrue("HotA 1.8.0 language LOD missing (data/HotA_lng.lod)", lng180.isFile)
    }

    fun requireInstallers() {
        assumeTrue("HotA 1.7.3 installer not in data/", installer173.isFile)
        assumeTrue("HotA 1.8.0 installer not in data/", installer180.isFile)
    }

    fun requireGoldens() {
        assumeTrue("Golden hota.lod missing", goldenLod173.isFile)
        assumeTrue("Golden hota18.lod missing", goldenLod180.isFile)
    }
}
