package com.homm3.livewallpaper.core.install

import com.homm3.livewallpaper.parser.inno.TestFixtures
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaseLodArchiveTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val lodBytes = "LOD\u0000 fake sprites".toByteArray()

    @Test
    fun plainFileIsCopied() {
        val target = tempFolder.root.resolve("H3sprite.lod")

        val isFound = BaseLodArchive.extract(lodBytes.inputStream(), target) {}

        assertTrue(isFound)
        assertContentEquals(lodBytes, target.readBytes())
    }

    @Test
    fun zipWithLodInSubfolderIsExtracted() {
        val zip = zipOf("readme.txt" to "hi".toByteArray(), "Heroes3/Data/h3sprite.lod" to lodBytes)
        val target = tempFolder.root.resolve("H3sprite.lod")

        val isFound = BaseLodArchive.extract(zip.inputStream(), target) {}

        assertTrue(isFound)
        assertContentEquals(lodBytes, target.readBytes())
    }

    @Test
    fun zipWithoutLodIsNotFound() {
        val zip = zipOf("readme.txt" to "hi".toByteArray())

        val isFound = BaseLodArchive.extract(zip.inputStream(), tempFolder.root.resolve("H3sprite.lod")) {}

        assertFalse(isFound)
    }

    @Test
    fun zipWithMacDemoSitIsExtracted() {
        TestFixtures.requireMacDemoSit()
        val zipFile = tempFolder.root.resolve("demo.zip")
        zipFile.outputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.setLevel(Deflater.NO_COMPRESSION)
                zip.putNextEntry(ZipEntry("demo/${TestFixtures.macDemoSit.name}"))
                TestFixtures.macDemoSit.inputStream().use { it.copyTo(zip) }
            }
        }
        val target = tempFolder.root.resolve("H3sprite.lod")

        val isFound = zipFile.inputStream().use { BaseLodArchive.extract(it, target) {} }

        assertTrue(isFound)
        assertEquals(TestFixtures.MAC_DEMO_H3SPRITE_SIZE, target.length())
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
            }
        }
        return bytes.toByteArray()
    }
}
