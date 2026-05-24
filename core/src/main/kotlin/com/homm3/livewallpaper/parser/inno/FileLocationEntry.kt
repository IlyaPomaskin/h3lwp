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
    // 2-byte stored_flags for 5.5.7u data_entry::Flags.
    // Flags are assigned sequentially via stored_flag_reader (one bit per .add() call, LSB first):
    //   Byte 0: [VersionInfoValid(0), VersionInfoNotValid(1), TimeStampInUTC(2),
    //            IsUninstallerExe(3), CallInstructionOptimized(4), Touch(5),
    //            ChunkEncrypted(6), ChunkCompressed(7)]
    //   Byte 1: [SolidBreak(0), Sign(1), SignOnce(2), ...]
    // => ChunkEncrypted  = byte0 bit 6 = (1 shl 6) in flagsInt if we read LE u16
    // => ChunkCompressed = byte0 bit 7 = (1 shl 7) in flagsInt if we read LE u16
    private const val FLAGS_BYTES = 2
    private const val FLAG_CHUNK_ENCRYPTED  = 1 shl 6
    private const val FLAG_CHUNK_COMPRESSED = 1 shl 7

    fun readAll(stream: InputStream, count: Int): List<FileLocationEntry> =
        (0 until count).map { readOne(stream) }

    private fun readOne(stream: InputStream): FileLocationEntry {
        val firstSlice  = readU32(stream)
        val lastSlice   = readU32(stream)
        val chunkOffset = readU32(stream).toLong() and 0xFFFFFFFFL
        val fileOffset  = readU64(stream)
        val fileSize    = readU64(stream)
        val chunkSize   = readU64(stream)
        stream.skipNBytes(20) // SHA-1 checksum (5.3.9 <= v < 6.4.0)
        stream.skipNBytes(8)  // Win32 FILETIME (int64) -- timestamp_nsec is computed, not stored
        stream.skipNBytes(4)  // file_version_ms
        stream.skipNBytes(4)  // file_version_ls
        // Flags: 2 bytes, read as little-endian u16 so byte[0] lands at bits 0-7
        val b0 = stream.read(); require(b0 >= 0) { "EOF reading data_entry flags byte 0" }
        val b1 = stream.read(); require(b1 >= 0) { "EOF reading data_entry flags byte 1" }
        val flagsInt = b0 or (b1 shl 8)
        val chunkCompressed = (flagsInt and FLAG_CHUNK_COMPRESSED) != 0
        val chunkEncrypted  = (flagsInt and FLAG_CHUNK_ENCRYPTED)  != 0

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
