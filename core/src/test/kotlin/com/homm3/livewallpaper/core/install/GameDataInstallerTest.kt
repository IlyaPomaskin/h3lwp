package com.homm3.livewallpaper.core.install

import com.homm3.livewallpaper.core.AssetPaths
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.InputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class GameDataInstallerTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val oneBundledMap = object : BundledMapSource {
        override fun list(): List<String> = listOf("bundled.h3m")
        override fun open(fileName: String): InputStream = ByteArray(0).inputStream()
    }

    @Test
    fun invalidLodIsRejectedAndRemoved() {
        val dataDir = tempFolder.newFolder("data")
        val installer = GameDataInstaller(dataDir, tempFolder.newFolder("tmp"), oneBundledMap)

        val result = installer.installBaseLod("not a lod".byteInputStream()) {}

        assertIs<InstallResult.Failure>(result)
        assertFalse(dataDir.resolve(AssetPaths.LOD_FILE).exists())
        assertFalse(dataDir.resolve("${AssetPaths.USER_MAPS_FOLDER}/bundled.h3m").exists())
    }

    @Test
    fun sanitizeFileNameReplacesIllegalCharacters() {
        assertEquals("a_b_c.h3m", sanitizeFileName(" a:b?c.h3m "))
        assertEquals("x_y.h3m", sanitizeFileName("x\u0001y.h3m"))
        assertEquals("unnamed.h3m", sanitizeFileName("   "))
    }
}
