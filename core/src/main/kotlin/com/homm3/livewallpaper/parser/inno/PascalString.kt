package com.homm3.livewallpaper.parser.inno

import java.io.InputStream

object PascalString {
    fun read(stream: InputStream): String {
        val len = readU32(stream)
        if (len == 0) return ""
        require(len in 1..(16 * 1024 * 1024)) { "PascalString length out of range: $len" }
        val bytes = stream.readNBytes(len)
        require(bytes.size == len) { "Short read: wanted $len, got ${bytes.size}" }
        return String(bytes, Charsets.UTF_16LE)
    }

    fun skip(stream: InputStream) {
        val len = readU32(stream)
        if (len == 0) return
        var remaining = len.toLong()
        while (remaining > 0) {
            val n = stream.skip(remaining)
            if (n <= 0) throw IllegalStateException("Failed to skip $remaining bytes of string")
            remaining -= n
        }
    }

    private fun readU32(stream: InputStream): Int {
        val b0 = stream.read(); val b1 = stream.read(); val b2 = stream.read(); val b3 = stream.read()
        require(b3 != -1) { "Unexpected EOF reading u32" }
        return (b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24))
    }
}
