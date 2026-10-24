package com.homm3.livewallpaper.core.install

import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.core.GameConfig
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import com.homm3.livewallpaper.parser.h3m.H3mHeaderReader
import com.homm3.livewallpaper.parser.h3m.H3mVersion
import com.homm3.livewallpaper.parser.inno.TestFixtures
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
    fun gogInstallerInstallsBaseLodAndBundledMaps() {
        TestFixtures.requireGogInstaller()
        val dataDir = tempFolder.newFolder("data")
        val installer = GameDataInstaller(dataDir, tempFolder.newFolder("tmp"), oneBundledMap)

        val result = RandomAccessFile(TestFixtures.gogDataSlice, "r").use { slice ->
            installer.installBaseFromGog(TestFixtures.gogSetupExe, listOf(slice.channel)) {}
        }

        assertIs<InstallResult.Success>(result)
        assertEquals(GOG_H3SPRITE_SIZE, dataDir.resolve(AssetPaths.LOD_FILE).length())
        assertTrue(dataDir.resolve("${AssetPaths.USER_MAPS_FOLDER}/bundled.h3m").exists())
        val maps = baseGameMaps(dataDir)
        assertEquals(GOG_SOD_MAP_COUNT, maps.size)
        assertTrue(maps.all { map -> map.inputStream().use { H3mHeaderReader.readVersion(it) } == H3mVersion.SOD })
    }

    @Test
    fun gogInstallerExtractsBaseGameMapsWhenHotaIsInstalled() {
        TestFixtures.requireGogInstaller()
        val dataDir = tempFolder.newFolder("data")
        dataDir.resolve(AssetPaths.HOTA_LOD_FILE).writeText("hota")
        val installer = GameDataInstaller(dataDir, tempFolder.newFolder("tmp"), oneBundledMap)

        val result = RandomAccessFile(TestFixtures.gogDataSlice, "r").use { slice ->
            installer.installBaseFromGog(TestFixtures.gogSetupExe, listOf(slice.channel)) {}
        }

        assertIs<InstallResult.Success>(result)
        assertEquals(GOG_SOD_MAP_COUNT, baseGameMaps(dataDir).size)
    }

    @Test
    fun hotaInstallKeepsBaseGameMapsAndOthers() {
        TestFixtures.requireInstallers()
        val dataDir = tempFolder.newFolder("data")
        val mapsDir = dataDir.resolve(AssetPaths.USER_MAPS_FOLDER).apply { mkdirs() }
        mapsDir.resolve("[SoD] Arrogance.h3m").writeText("sod")
        mapsDir.resolve("imported.h3m").writeText("user")
        val installer = GameDataInstaller(dataDir, tempFolder.newFolder("tmp"), oneBundledMap)

        val result = installer.installHota(TestFixtures.installer180, extractInstallerMaps = false) {}

        assertIs<InstallResult.Success>(result)
        assertEquals(1, baseGameMaps(dataDir).size)
        assertTrue(mapsDir.resolve("imported.h3m").exists())
    }

    @Test
    fun hotaInstallDoesNotExtractCampaignMaps() {
        TestFixtures.requireInstallers()
        val dataDir = tempFolder.newFolder("data")
        val installer = GameDataInstaller(dataDir, tempFolder.newFolder("tmp"), oneBundledMap)

        val result = installer.installHota(TestFixtures.installer180, extractInstallerMaps = false) {}

        assertIs<InstallResult.Success>(result)
        val maps = dataDir.resolve(AssetPaths.USER_MAPS_FOLDER).list().orEmpty()
        assertTrue(maps.isEmpty(), "Unexpected maps: ${maps.joinToString()}")
    }

    @Test
    fun hotaInstallDeletesMapsLargerThanSupported() {
        TestFixtures.requireInstallers()
        val dataDir = tempFolder.newFolder("data")
        val installer = GameDataInstaller(dataDir, tempFolder.newFolder("tmp"), oneBundledMap)

        val result = installer.installHota(TestFixtures.installer180, extractInstallerMaps = true) {}

        assertIs<InstallResult.Success>(result)
        val sizes = dataDir.resolve(AssetPaths.USER_MAPS_FOLDER)
            .listFiles { file -> file.name.endsWith(".h3m") && file.length() > 0 }
            .orEmpty()
            .associate { map -> map.name to map.inputStream().use { H3mHeaderReader.readMapSize(it) } }
        assertTrue(sizes.isNotEmpty())
        assertTrue(sizes.values.all { it <= GameConfig.MAX_MAP_SIZE }, "Oversized maps: ${sizes.filterValues { it > GameConfig.MAX_MAP_SIZE }}")
    }

    @Test
    fun failedHotaInstallKeepsBaseGameMaps() {
        TestFixtures.requireGogInstaller()
        val dataDir = tempFolder.newFolder("data")
        val mapsDir = dataDir.resolve(AssetPaths.USER_MAPS_FOLDER).apply { mkdirs() }
        mapsDir.resolve("[SoD] Arrogance.h3m").writeText("sod")
        val installer = GameDataInstaller(dataDir, tempFolder.newFolder("tmp"), oneBundledMap)

        val result = installer.installHota(TestFixtures.gogSetupExe, extractInstallerMaps = false) {}

        assertIs<InstallResult.Failure>(result)
        assertEquals(1, baseGameMaps(dataDir).size)
    }

    @Test
    fun installerWithoutBaseLodIsRejected() {
        TestFixtures.requireInstallers()
        val dataDir = tempFolder.newFolder("data")
        val installer = GameDataInstaller(dataDir, tempFolder.newFolder("tmp"), oneBundledMap)

        val result = installer.installBaseFromGog(TestFixtures.installer180, dataSlices = emptyList()) {}

        assertIs<InstallResult.Failure>(result)
        assertFalse(dataDir.resolve(AssetPaths.LOD_FILE).exists())
    }

    @Test
    fun sanitizeFileNameReplacesIllegalCharacters() {
        assertEquals("a_b_c.h3m", sanitizeFileName(" a:b?c.h3m "))
        assertEquals("x_y.h3m", sanitizeFileName("x\u0001y.h3m"))
        assertEquals("unnamed.h3m", sanitizeFileName("   "))
    }

    @Test
    fun hotaMapFileNameAddsPrefixOnce() {
        assertEquals("[HotA] Jebus.h3m", hotaMapFileName("maps\\Jebus.h3m"))
        assertEquals("[HotA] Jebus.h3m", hotaMapFileName("maps\\[HotA] Jebus.h3m"))
    }

    private fun baseGameMaps(dataDir: File): List<File> =
        dataDir.resolve(AssetPaths.USER_MAPS_FOLDER)
            .listFiles { file -> file.name.startsWith("[SoD] ") }
            .orEmpty()
            .toList()

    private companion object {
        const val GOG_H3SPRITE_SIZE = 64_973_159L
        const val GOG_SOD_MAP_COUNT = 56
    }
}
