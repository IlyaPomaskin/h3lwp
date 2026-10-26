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
    val gogSetupExe: File get() = projectRoot.resolve("data/h3-gog/$GOG_SETUP_BASE_NAME.exe")
    val gogDataSlice: File get() = projectRoot.resolve("data/h3-gog/$GOG_SETUP_BASE_NAME-1.bin")
    val macDemoSit: File get() = projectRoot.resolve("data/heroes_3_complete_demo.sit")

    /** MD5 of h3sprite.lod in [macDemoSit], as extracted by `unar`. */
    const val MAC_DEMO_H3SPRITE_MD5 = "3c9c7637d1aee5dd0fb8c2b91d58c217"
    const val MAC_DEMO_H3SPRITE_SIZE = 49_501_869L

    private const val GOG_SETUP_BASE_NAME = "setup_heroes_of_might_and_magic_3_complete_4.0_(3.2)_gog_0.1_(77075)"

    fun requireLng180() {
        assumeTrue("HotA 1.8.0 language LOD missing (data/HotA_lng.lod)", lng180.isFile)
    }

    fun requireInstallers() {
        assumeTrue("HotA 1.7.3 installer not in data/", installer173.isFile)
        assumeTrue("HotA 1.8.0 installer not in data/", installer180.isFile)
    }

    fun requireGogInstaller() {
        assumeTrue("GOG setup .exe not in data/h3-gog/", gogSetupExe.isFile)
        assumeTrue("GOG setup -1.bin not in data/h3-gog/", gogDataSlice.isFile)
    }

    fun requireMacDemoSit() {
        assumeTrue("Mac demo archive not in data/ (archive.org/details/heroes_3_complete_demo)", macDemoSit.isFile)
    }

    fun requireGoldens() {
        assumeTrue("Golden hota.lod missing", goldenLod173.isFile)
        assumeTrue("Golden hota18.lod missing", goldenLod180.isFile)
    }
}
