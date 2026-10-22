package com.homm3.livewallpaper.parser.lod

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/** Random access to LOD archive bytes. Reads past the end are zero-padded, matching the old stream reader. */
interface LodDataSource : Closeable {
    fun read(offset: Long, length: Int): ByteArray
}

/** Reads only the requested ranges from disk; positional reads, safe to share between threads. */
class FileLodDataSource(file: File) : LodDataSource {
    private val channel: FileChannel = RandomAccessFile(file, "r").channel

    override fun read(offset: Long, length: Int): ByteArray {
        require(offset >= 0 && length >= 0) { "invalid LOD read: offset=$offset length=$length" }
        val bytes = ByteArray(length)
        val buffer = ByteBuffer.wrap(bytes)
        var position = offset
        while (buffer.hasRemaining()) {
            val readCount = channel.read(buffer, position)
            if (readCount <= 0) break
            position += readCount
        }
        return bytes
    }

    override fun close() = channel.close()
}

/** Whole archive already in memory, e.g. from an InputStream. */
class ByteArrayLodDataSource(private val data: ByteArray) : LodDataSource {
    override fun read(offset: Long, length: Int): ByteArray {
        require(offset >= 0 && length >= 0) { "invalid LOD read: offset=$offset length=$length" }
        val bytes = ByteArray(length)
        if (offset >= data.size) return bytes
        val available = minOf(length.toLong(), data.size - offset).toInt()
        System.arraycopy(data, offset.toInt(), bytes, 0, available)
        return bytes
    }

    override fun close() = Unit
}
