package com.homm3.livewallpaper.parser.inno

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class FileLocationEntry(
    val firstSlice: Int,
    val lastSlice: Int,
    val chunkOffset: Long, // absolute offset inside setup-1 stream
    val fileOffset: Long,  // offset inside decompressed chunk
    val fileSize: Long,    // uncompressed bytes of this file
    val chunkSize: Long,   // compressed bytes of the chunk (from chunkOffset)
    val chunkCompressed: Boolean,
    val chunkEncrypted: Boolean,
)

object FileLocationEntries {
    // 2-byte stored_flags for 5.6.0u data_entry::Flags. Verify against innoextract.
    private const val FLAGS_BYTES = 2
    // Flag bit positions within the stored_flags. Order matches data_entry::Flags enum in innoextract.
    private const val FLAG_BZIPPED = 1 shl 2
    private const val FLAG_TIMESTAMP_UTC = 1 shl 3
    private const val FLAG_IS_UNINSTALLER = 1 shl 4
    private const val FLAG_CALL_INSTRUCTION_OPTIMIZED = 1 shl 5
    private const val FLAG_TOUCH = 1 shl 6
    private const val FLAG_CHUNK_ENCRYPTED = 1 shl 7
    private const val FLAG_CHUNK_COMPRESSED = 1 shl 8
    // (Higher bits: SolidBreak, Sign, SignOnce — not needed here.)

    fun readAll(stream: InputStream, count: Int): List<FileLocationEntry> =
        (0 until count).map { readOne(stream) }

    private fun readOne(stream: InputStream): FileLocationEntry {
        val firstSlice = readU32(stream)
        val lastSlice = readU32(stream)
        val chunkOffset = readU32(stream).toLong() and 0xFFFFFFFFL
        val fileOffset = readU64(stream)
        val fileSize = readU64(stream)
        val chunkSize = readU64(stream)
        stream.skipNBytes(20) // SHA1 checksum
        stream.skipNBytes(8)  // FILETIME timestamp
        stream.skipNBytes(4)  // timestamp_nsec
        stream.skipNBytes(4)  // file_version_ms
        stream.skipNBytes(4)  // file_version_ls
        val flagsBytes = ByteArray(FLAGS_BYTES); var off = 0
        while (off < FLAGS_BYTES) {
            val n = stream.read(flagsBytes, off, FLAGS_BYTES - off)
            require(n > 0) { "EOF reading data_entry flags" }
            off += n
        }
        val flagsInt = flagsBytes.fold(0) { acc, b -> (acc shl 8) or (b.toInt() and 0xFF) }
        val chunkCompressed = (flagsInt and FLAG_CHUNK_COMPRESSED) != 0
        val chunkEncrypted = (flagsInt and FLAG_CHUNK_ENCRYPTED) != 0

        return FileLocationEntry(
            firstSlice, lastSlice, chunkOffset, fileOffset, fileSize, chunkSize,
            chunkCompressed, chunkEncrypted
        )
    }

    private fun readU32(stream: InputStream): Int {
        val b = ByteArray(4); var off = 0
        while (off < 4) { val n = stream.read(b, off, 4 - off); require(n > 0); off += n }
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int
    }

    private fun readU64(stream: InputStream): Long {
        val b = ByteArray(8); var off = 0
        while (off < 8) { val n = stream.read(b, off, 8 - off); require(n > 0); off += n }
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).long
    }
}
