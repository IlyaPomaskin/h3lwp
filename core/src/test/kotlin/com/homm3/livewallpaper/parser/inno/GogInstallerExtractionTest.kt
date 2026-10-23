package com.homm3.livewallpaper.parser.inno

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

class GogInstallerExtractionTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun extracts_h3sprite_lod_from_external_slice() {
        TestFixtures.requireGogInstaller()
        val output = tmp.newFile("H3sprite.lod")

        RandomAccessFile(TestFixtures.gogDataSlice, "r").use { slice ->
            InnoSetupExtractor.extract(
                installer = TestFixtures.gogSetupExe,
                targets = listOf(
                    InnoSetupExtractor.Target(
                        matcher = { it.endsWith(H3SPRITE_DESTINATION_SUFFIX, ignoreCase = true) },
                        outputFor = { output },
                    ),
                ),
                dataSlices = listOf(slice.channel),
            )
        }

        assertEquals(EXPECTED_SIZE, output.length())
        assertEquals(EXPECTED_SHA256, sha256(output))
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 20)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val H3SPRITE_DESTINATION_SUFFIX = "data\\h3sprite.lod"
        const val EXPECTED_SIZE = 64_973_159L
        const val EXPECTED_SHA256 = "57caf2c50573f33a0d91e4222e51d3a73c136d44decf59dde21cacad88fe5d66"
    }
}
