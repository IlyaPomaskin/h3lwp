package com.homm3.livewallpaper.parser.sit

import com.homm3.livewallpaper.parser.inno.TestFixtures
import org.junit.Test
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StuffIt5ReaderTest {
    @Test
    fun extractsH3spriteFromMacDemo() {
        TestFixtures.requireMacDemoSit()
        val digest = MessageDigest.getInstance("MD5")
        var lastProgress = 0L

        val isFound = TestFixtures.macDemoSit.inputStream().buffered().use { input ->
            StuffIt5Reader.extract(
                input = input,
                matches = { it.equals("h3sprite.lod", ignoreCase = true) },
                output = DigestOutputStream(OutputStream.nullOutputStream(), digest),
                onProgress = { written, _ -> lastProgress = written },
            )
        }

        assertTrue(isFound)
        assertEquals(TestFixtures.MAC_DEMO_H3SPRITE_SIZE, lastProgress)
        assertEquals(TestFixtures.MAC_DEMO_H3SPRITE_MD5, digest.digest().toHex())
    }

    @Test
    fun missingFileIsNotFound() {
        TestFixtures.requireMacDemoSit()

        val isFound = TestFixtures.macDemoSit.inputStream().buffered().use { input ->
            StuffIt5Reader.extract(input, { it == "missing.lod" }, OutputStream.nullOutputStream()) { _, _ -> }
        }

        assertFalse(isFound)
    }

    @Test
    fun otherDataIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            StuffIt5Reader.extract(ByteArray(200).inputStream(), { true }, OutputStream.nullOutputStream()) { _, _ -> }
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
