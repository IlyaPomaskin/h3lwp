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

    @Test fun splits_three_concatenated_streams() {
        val a = "alpha".toByteArray(Charsets.UTF_8)
        val b = ByteArray(1024) { (it % 251).toByte() }  // non-trivial payload
        val c = "gamma!".toByteArray(Charsets.UTF_8)
        val gzA = gzip(a); val gzB = gzip(b); val gzC = gzip(c)
        val joined = gzA + gzB + gzC

        val streams = GzipStreamSplitter.split(joined)

        assertEquals(3, streams.size)
        assertEquals(0, streams[0].compressedStart)
        assertEquals(gzA.size, streams[0].compressedEndExclusive)
        assertArrayEquals(a, streams[0].decompressed)

        assertEquals(gzA.size, streams[1].compressedStart)
        assertEquals(gzA.size + gzB.size, streams[1].compressedEndExclusive)
        assertArrayEquals(b, streams[1].decompressed)

        assertEquals(gzA.size + gzB.size, streams[2].compressedStart)
        assertEquals(joined.size, streams[2].compressedEndExclusive)
        assertArrayEquals(c, streams[2].decompressed)
    }
}
