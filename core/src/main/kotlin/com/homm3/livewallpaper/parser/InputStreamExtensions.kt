package com.homm3.livewallpaper.parser

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream

private const val END_OF_STREAM = -1
private const val COPY_BUFFER_SIZE = 64 * 1024

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

/** Reads the whole stream, or returns `null` as soon as it turns out longer than [maxBytes]. */
fun InputStream.readAtMost(maxBytes: Int): ByteArray? {
    require(maxBytes >= 0) { "maxBytes must be non-negative: $maxBytes" }
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(COPY_BUFFER_SIZE)
    while (true) {
        val bytesRead = read(buffer)
        if (bytesRead == END_OF_STREAM) return output.toByteArray()
        output.write(buffer, 0, bytesRead)
        if (output.size() > maxBytes) return null
    }
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
