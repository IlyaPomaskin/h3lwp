package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.homm3.livewallpaper.core.GameConfig
import com.homm3.livewallpaper.parser.h3m.H3mVersion
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPOutputStream
import kotlin.test.assertEquals

class MapFileScannerTest {
    companion object {
        private const val SUPPORTED_SIZE = 36
        private lateinit var app: HeadlessApplication

        @JvmStatic
        @BeforeClass
        fun setUp() {
            app = HeadlessApplication(object : ApplicationAdapter() {}, HeadlessApplicationConfiguration())
        }

        @JvmStatic
        @AfterClass
        fun tearDown() {
            app.exit()
        }
    }

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val mapsFolder: File get() = tempFolder.root

    private fun writeMap(name: String, version: H3mVersion, paddingBytes: Int = 0): File {
        val file = File(mapsFolder, name)
        GZIPOutputStream(FileOutputStream(file)).use { gzip ->
            val header = ByteBuffer.allocate(Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(version.value)
            gzip.write(header.array())
            gzip.write(ByteArray(paddingBytes) { it.toByte() })
        }
        return file
    }

    private class CountingVersionReader(private val delegate: (File) -> H3mVersion?) : (File) -> H3mVersion? {
        val readFileNames = mutableListOf<String>()

        override fun invoke(file: File): H3mVersion? {
            readFileNames.add(file.name)
            return delegate(file)
        }
    }

    private fun versionsByName(vararg entries: Pair<String, H3mVersion>): (File) -> H3mVersion? {
        val versions = entries.toMap()
        return { file -> versions[file.name] }
    }

    @Test
    fun `lists non-empty h3m files sorted by size`() {
        writeMap("big.h3m", H3mVersion.SOD, paddingBytes = 50_000)
        writeMap("small.h3m", H3mVersion.ROE)
        File(mapsFolder, "empty.h3m").createNewFile()
        File(mapsFolder, "notes.txt").writeText("not a map")

        val names = MapFileScanner(readSize = { SUPPORTED_SIZE }).listMapFiles(mapsFolder, includeHota = false)

        assertEquals(listOf("small.h3m", "big.h3m"), names)
    }

    @Test
    fun `reads real headers and drops HotA maps without HotA assets`() {
        writeMap("hota.h3m", H3mVersion.HOTA)
        writeMap("sod.h3m", H3mVersion.SOD, paddingBytes = 1_000)
        val scanner = MapFileScanner(readSize = { SUPPORTED_SIZE })

        assertEquals(listOf("sod.h3m"), scanner.listMapFiles(mapsFolder, includeHota = false))
        assertEquals(setOf("hota.h3m", "sod.h3m"), scanner.listMapFiles(mapsFolder, includeHota = true).toSet())
    }

    @Test
    fun `does not open files when HotA assets are installed`() {
        writeMap("hota.h3m", H3mVersion.HOTA)
        val reader = CountingVersionReader(versionsByName("hota.h3m" to H3mVersion.HOTA))

        MapFileScanner(reader, readSize = { SUPPORTED_SIZE }).listMapFiles(mapsFolder, includeHota = true)

        assertEquals(emptyList(), reader.readFileNames)
    }

    @Test
    fun `caches versions until the file changes`() {
        val file = writeMap("map.h3m", H3mVersion.SOD)
        val reader = CountingVersionReader(versionsByName("map.h3m" to H3mVersion.SOD))
        val scanner = MapFileScanner(reader, readSize = { SUPPORTED_SIZE })

        scanner.listMapFiles(mapsFolder, includeHota = false)
        scanner.listMapFiles(mapsFolder, includeHota = false)
        scanner.filterSupported(mapsFolder, listOf("map.h3m"), includeHota = false)
        assertEquals(listOf("map.h3m"), reader.readFileNames)

        file.appendBytes(ByteArray(16))
        scanner.listMapFiles(mapsFolder, includeHota = false)
        assertEquals(listOf("map.h3m", "map.h3m"), reader.readFileNames)
    }

    @Test
    fun `filterSupported keeps order and drops HotA maps`() {
        writeMap("b.h3m", H3mVersion.AB)
        writeMap("hota.h3m", H3mVersion.HOTA)
        writeMap("a.h3m", H3mVersion.ROE)

        val names = MapFileScanner(readSize = { SUPPORTED_SIZE }).filterSupported(
            mapsFolder,
            listOf("b.h3m", "hota.h3m", "a.h3m"),
            includeHota = false,
        )

        assertEquals(listOf("b.h3m", "a.h3m"), names)
    }

    @Test
    fun `drops maps larger than the supported size`() {
        writeMap("xl.h3m", H3mVersion.SOD)
        writeMap("huge.h3m", H3mVersion.SOD, paddingBytes = 1_000)
        val sizes = mapOf("xl.h3m" to GameConfig.MAX_MAP_SIZE, "huge.h3m" to GameConfig.MAX_MAP_SIZE + 36)
        val scanner = MapFileScanner(readSize = { file -> sizes.getValue(file.name) })

        assertEquals(listOf("xl.h3m"), scanner.listMapFiles(mapsFolder, includeHota = true))
        assertEquals(listOf("xl.h3m"), scanner.filterSupported(mapsFolder, listOf("huge.h3m", "xl.h3m"), includeHota = true))
    }
}
