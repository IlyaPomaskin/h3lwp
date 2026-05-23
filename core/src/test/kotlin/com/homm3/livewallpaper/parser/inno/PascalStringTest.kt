package com.homm3.livewallpaper.parser.inno

import org.junit.Test
import org.junit.Assert.assertEquals
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PascalStringTest {
    private fun encode(s: String): ByteArray {
        val utf16 = s.toByteArray(Charsets.UTF_16LE)
        val out = ByteBuffer.allocate(4 + utf16.size).order(ByteOrder.LITTLE_ENDIAN)
        out.putInt(utf16.size)
        out.put(utf16)
        return out.array()
    }

    @Test fun reads_ascii() {
        val bytes = encode("Horn of the Abyss")
        assertEquals("Horn of the Abyss", PascalString.read(ByteArrayInputStream(bytes)))
    }

    @Test fun reads_empty() {
        val bytes = encode("")
        assertEquals("", PascalString.read(ByteArrayInputStream(bytes)))
    }

    @Test fun reads_non_ascii() {
        val bytes = encode("Русский")
        assertEquals("Русский", PascalString.read(ByteArrayInputStream(bytes)))
    }

    @Test fun skip_advances_past_string() {
        val combined = encode("first") + encode("second")
        val stream = ByteArrayInputStream(combined)
        PascalString.skip(stream)
        assertEquals("second", PascalString.read(stream))
    }
}
