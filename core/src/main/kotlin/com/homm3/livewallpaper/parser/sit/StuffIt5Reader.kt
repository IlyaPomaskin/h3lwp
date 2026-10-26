package com.homm3.livewallpaper.parser.sit

import com.homm3.livewallpaper.parser.skipExactly
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Streams through a StuffIt 5 archive (the Mac Heroes III demo is one) and extracts the data fork of one file.
 * Supports stored and method 13 entries without encryption.
 */
object StuffIt5Reader {
    private val SIGNATURE = "StuffIt (c)1997-".toByteArray(Charsets.US_ASCII)
    val SIGNATURE_LENGTH: Int = SIGNATURE.size

    private const val ARCHIVE_HEADER_SIZE = 100
    private const val ARCHIVE_VERSION_OFFSET = 82
    private const val ARCHIVE_FLAGS_OFFSET = 83
    private const val ARCHIVE_ENTRY_COUNT_OFFSET = 92
    private const val ARCHIVE_FIRST_ENTRY_OFFSET = 94
    private const val SUPPORTED_ARCHIVE_VERSION = 5
    private const val ARCHIVE_FLAG_ENCRYPTED = 0x80

    private const val ENTRY_ID = 0xA5A5A5A5.toInt()
    private const val ENTRY_FIXED_SIZE = 48
    private const val ENTRY_VERSION_OFFSET = 4
    private const val ENTRY_HEADER_SIZE_OFFSET = 6
    private const val ENTRY_FLAGS_OFFSET = 9
    private const val ENTRY_NAME_LENGTH_OFFSET = 30
    private const val ENTRY_DATA_LENGTH_OFFSET = 34
    private const val ENTRY_DATA_COMPRESSED_OFFSET = 38
    private const val ENTRY_DATA_CRC_OFFSET = 42
    private const val ENTRY_CHILD_COUNT_OFFSET = 46
    private const val ENTRY_METHOD_OFFSET = 46
    private const val ENTRY_PASSWORD_LENGTH_OFFSET = 47
    private const val ENTRY_FLAG_DIRECTORY = 0x40
    private const val ENTRY_FLAG_ENCRYPTED = 0x20
    private const val PLACEHOLDER_DIRECTORY_LENGTH = 0xFFFF_FFFFL

    private const val FINDER_INFO_SIZE = 14
    private const val FINDER_FLAG_RESOURCE_FORK = 0x01
    private const val FINDER_TRAILER_SIZE_V1 = 22
    private const val FINDER_TRAILER_SIZE = 18
    private const val RESOURCE_INFO_SIZE = 14
    private const val RESOURCE_COMPRESSED_OFFSET = 4
    private const val RESOURCE_PASSWORD_LENGTH_OFFSET = 13

    private const val METHOD_STORED = 0
    private const val METHOD_LZ_HUFFMAN = 13

    fun hasSignature(header: ByteArray): Boolean =
        header.size >= SIGNATURE.size && header.copyOf(SIGNATURE.size).contentEquals(SIGNATURE)

    /**
     * Writes the data fork of the first file whose name (without folders) [matches] to [output].
     * Returns false when no file matches. Leaves [input] open.
     */
    fun extract(
        input: InputStream,
        matches: (fileName: String) -> Boolean,
        output: OutputStream,
        onProgress: (written: Long, total: Long) -> Unit,
    ): Boolean {
        val archive = PositionedInputStream(input)
        val header = archive.readBytes(ARCHIVE_HEADER_SIZE)
        require(hasSignature(header)) { "Not a StuffIt 5 archive" }
        val version = header.u8(ARCHIVE_VERSION_OFFSET)
        require(version == SUPPORTED_ARCHIVE_VERSION) { "Unsupported StuffIt archive version: $version" }
        require(header.u8(ARCHIVE_FLAGS_OFFSET) and ARCHIVE_FLAG_ENCRYPTED == 0) { "Encrypted StuffIt archives are not supported" }

        archive.skipTo(header.u32(ARCHIVE_FIRST_ENTRY_OFFSET))
        var remainingEntries = header.u16(ARCHIVE_ENTRY_COUNT_OFFSET)
        while (remainingEntries > 0) {
            remainingEntries--
            val entryStart = archive.position
            val entry = archive.readBytes(ENTRY_FIXED_SIZE)
            require(entry.u32(0).toInt() == ENTRY_ID) { "Corrupt StuffIt entry at $entryStart" }

            val flags = entry.u8(ENTRY_FLAGS_OFFSET)
            val isDirectory = flags and ENTRY_FLAG_DIRECTORY != 0
            val dataLength = entry.u32(ENTRY_DATA_LENGTH_OFFSET)
            if (isDirectory && dataLength == PLACEHOLDER_DIRECTORY_LENGTH) {
                remainingEntries++
                continue
            }
            if (isDirectory) remainingEntries += entry.u16(ENTRY_CHILD_COUNT_OFFSET)

            val fileName = String(archive.readBytes(entry.u16(ENTRY_NAME_LENGTH_OFFSET)), Charsets.ISO_8859_1)
            archive.skipTo(entryStart + entry.u16(ENTRY_HEADER_SIZE_OFFSET))
            val resourceCompressedSize = skipFinderInfo(archive, entry.u8(ENTRY_VERSION_OFFSET))
            if (isDirectory) continue

            archive.skipExactly(resourceCompressedSize)
            val compressedSize = entry.u32(ENTRY_DATA_COMPRESSED_OFFSET)
            if (!matches(fileName)) {
                archive.skipExactly(compressedSize)
                continue
            }

            val isEncrypted = flags and ENTRY_FLAG_ENCRYPTED != 0 || entry.u8(ENTRY_PASSWORD_LENGTH_OFFSET) != 0
            require(!isEncrypted) { "$fileName is encrypted" }
            val data = BufferedInputStream(LimitedInputStream(archive, compressedSize))
            val checkedOutput = Crc16OutputStream(output)
            val progress = { written: Long -> onProgress(written, dataLength) }
            when (val method = entry.u8(ENTRY_METHOD_OFFSET)) {
                METHOD_STORED -> copyStored(data, checkedOutput, dataLength, progress)
                METHOD_LZ_HUFFMAN -> StuffIt13Decoder.decode(data, checkedOutput, dataLength, progress)
                else -> throw IllegalArgumentException("$fileName uses unsupported StuffIt method $method")
            }
            val expectedCrc = entry.u16(ENTRY_DATA_CRC_OFFSET)
            require(checkedOutput.crc == expectedCrc) { "$fileName is corrupt: CRC mismatch" }
            return true
        }
        return false
    }

    /** Skips the Finder info after an entry header; returns the compressed resource fork size that precedes the data fork. */
    private fun skipFinderInfo(archive: PositionedInputStream, entryVersion: Int): Long {
        val finderInfo = archive.readBytes(FINDER_INFO_SIZE)
        val trailerSize = if (entryVersion == 1) FINDER_TRAILER_SIZE_V1 else FINDER_TRAILER_SIZE
        archive.skipExactly(trailerSize.toLong())
        val hasResourceFork = finderInfo.u16(0) and FINDER_FLAG_RESOURCE_FORK != 0
        if (!hasResourceFork) return 0

        val resourceInfo = archive.readBytes(RESOURCE_INFO_SIZE)
        archive.skipExactly(resourceInfo.u8(RESOURCE_PASSWORD_LENGTH_OFFSET).toLong())
        return resourceInfo.u32(RESOURCE_COMPRESSED_OFFSET)
    }

    private fun copyStored(input: InputStream, output: OutputStream, size: Long, onProgress: (Long) -> Unit) {
        val copied = input.copyTo(output)
        require(copied == size) { "Stored data has $copied of $size bytes" }
        onProgress(copied)
    }

    private fun ByteArray.u8(offset: Int): Int = this[offset].toInt() and 0xFF

    private fun ByteArray.u16(offset: Int): Int = (u8(offset) shl Byte.SIZE_BITS) or u8(offset + 1)

    private fun ByteArray.u32(offset: Int): Long = (u16(offset).toLong() shl Short.SIZE_BITS) or u16(offset + 2).toLong()

    private class PositionedInputStream(input: InputStream) : FilterInputStream(input) {
        var position = 0L
            private set

        override fun read(): Int = super.read().also { if (it >= 0) position++ }

        override fun read(b: ByteArray, off: Int, len: Int): Int =
            super.read(b, off, len).also { if (it > 0) position += it }

        override fun skip(n: Long): Long = super.skip(n).also { position += it }

        fun readBytes(count: Int): ByteArray = ByteArray(count).also { DataInputStream(this).readFully(it) }

        fun skipTo(target: Long) {
            require(target >= position) { "StuffIt archive points backwards: $target < $position" }
            skipExactly(target - position)
        }
    }

    private class LimitedInputStream(input: InputStream, private var remaining: Long) : FilterInputStream(input) {
        override fun read(): Int {
            if (remaining <= 0) return -1
            return super.read().also { if (it >= 0) remaining-- }
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (remaining <= 0) return -1
            val count = super.read(b, off, minOf(len.toLong(), remaining).toInt())
            if (count > 0) remaining -= count
            return count
        }

        override fun close() = Unit
    }

    /** CRC-16/ARC (reflected polynomial 0xA001, initial value 0), as StuffIt stores it. */
    private class Crc16OutputStream(private val output: OutputStream) : OutputStream() {
        var crc = 0
            private set

        override fun write(b: Int) {
            update(b)
            output.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            for (i in off until off + len) update(b[i].toInt())
            output.write(b, off, len)
        }

        private fun update(byte: Int) {
            crc = (crc ushr Byte.SIZE_BITS) xor TABLE[(crc xor byte) and 0xFF]
        }

        private companion object {
            const val POLYNOMIAL = 0xA001
            val TABLE = IntArray(256) { index ->
                (0 until Byte.SIZE_BITS).fold(index) { crc, _ ->
                    if (crc and 1 != 0) (crc ushr 1) xor POLYNOMIAL else crc ushr 1
                }
            }
        }
    }
}
