package com.homm3.livewallpaper.parser

import java.io.EOFException
import java.io.InputStream

private const val END_OF_STREAM = -1

// InputStream.readNBytes(Int) is only available on Android API 33+.
fun InputStream.readUpTo(count: Int): ByteArray {
    require(count >= 0) { "count must be non-negative: $count" }
    val buffer = ByteArray(count)
    var offset = 0
    while (offset < count) {
        val bytesRead = read(buffer, offset, count - offset)
        if (bytesRead == END_OF_STREAM) break
        offset += bytesRead
    }
    if (offset == count) return buffer
    return buffer.copyOf(offset)
}

// InputStream.skipNBytes(Long) is only available on Android API 34+.
fun InputStream.skipExactly(count: Long) {
    require(count >= 0) { "count must be non-negative: $count" }
    var remaining = count
    while (remaining > 0) {
        val skipped = skip(remaining)
        if (skipped > 0) {
            remaining -= skipped
            continue
        }
        if (read() == END_OF_STREAM) throw EOFException("Failed to skip $remaining of $count bytes")
        remaining--
    }
}
