package com.homm3.livewallpaper.parser.h3c

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

class GzipStreamSplitterTest {

    private fun gzip(bytes: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { it.write(bytes) }
        return baos.toByteArray()
    }

    @Test fun splits_a_single_gzip_stream() {
        val payload = "hello world".toByteArray(Charsets.UTF_8)
        val gz = gzip(payload)

        val streams = GzipStreamSplitter.split(gz)

        assertEquals(1, streams.size)
        assertEquals(0, streams[0].compressedStart)
        assertEquals(gz.size, streams[0].compressedEndExclusive)
        assertArrayEquals(payload, streams[0].decompressed)
    }
}
