package com.homm3.livewallpaper.parser.inno

import org.tukaani.xz.LZMAInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile
import java.io.SequenceInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections

object CompressedBlockReader {
    private const val SETUP_ID_SIZE = 64

    /** Setup headers of real installers unpack to a few MB; the cap stops a corrupt one from filling the heap. */
    private const val MAX_UNPACKED_BLOCK_SIZE = 64 * 1024 * 1024
    private const val EXPECTED_COMPRESSION_RATIO = 4

    /**
     * Reads and LZMA-decompresses the primary setup-0 stream starting at [offset0].
     * Returns the decompressed bytes. After this call, [raf]'s file pointer is positioned
     * at the start of the secondary block (data entries block).
     */
    fun readSetup0(raf: RandomAccessFile, offset0: Long): ByteArray {
        readVersion(raf, offset0)
        return readCompressedBlock(raf)
    }

    /** Reads the 64-byte setup-0 identity string at [offset0] and leaves [raf] positioned right after it. */
    fun readVersion(raf: RandomAccessFile, offset0: Long): SetupDataVersion {
        raf.seek(offset0)
        val setupId = ByteArray(SETUP_ID_SIZE); raf.readFully(setupId)
        val idStr = String(setupId, Charsets.US_ASCII).trimEnd(' ', '\u0000')
        return SetupDataVersion.fromSetupId(idStr)
    }

    /**
     * Reads and LZMA-decompresses the secondary block (data entries) at the current file
     * position of [raf]. Must be called after [readSetup0] with the same [raf] — the file
     * pointer is left positioned just past the primary block by [readSetup0].
     */
    fun readDataEntriesBlock(raf: RandomAccessFile): ByteArray {
        return readCompressedBlock(raf)
    }

    /**
     * Reads one CRC-framed block from [raf]'s current position; the payload is LZMA-compressed
     * or stored as-is (GOG installers store it).
     * Format: HdrCRC(u32) + StoredSize(u32) + Compressed(u8) + StoredSize bytes of
     * 4096-byte chunks, each prefixed with a 4-byte CRC.
     */
    private fun readCompressedBlock(raf: RandomAccessFile): ByteArray {
        // TCompressedBlockHeader: HdrCRC u32, StoredSize u32, Compressed u8
        val hdr = ByteArray(9); raf.readFully(hdr)
        val hdrBuf = ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN)
        hdrBuf.int /* hdrCrc -- unverified */
        val storedSize = hdrBuf.int.toLong() and 0xFFFFFFFFL
        val compressed = hdrBuf.get().toInt() != 0

        val remainingFileBytes = raf.length() - raf.filePointer
        require(storedSize <= remainingFileBytes) { "Compressed block size $storedSize exceeds the file" }

        // Read StoredSize bytes; concat all 4096-byte LZMA blocks (after stripping 4-byte CRC each)
        val raw = ByteArray(storedSize.toInt())
        raf.readFully(raw)

        val blockSize = 4096
        val streams = mutableListOf<ByteArrayInputStream>()
        var pos = 0
        while (pos < raw.size) {
            pos += 4 // skip CRC
            val take = minOf(blockSize, raw.size - pos)
            streams.add(ByteArrayInputStream(raw, pos, take))
            pos += take
        }
        val concatenated = SequenceInputStream(Collections.enumeration(streams))
        if (!compressed) return concatenated.readBytes()

        // Inno Setup LZMA streams have a 5-byte header: 1 byte props + 4 bytes dict size (no uncompressed size).
        // Read and parse it so we can pass the values to LZMAInputStream(stream, uncompSize, props, dictSize),
        // which expects the stream to start at raw range-coder data (no header).
        val lzmaHeader = ByteArray(5)
        var hdrRead = 0
        while (hdrRead < 5) {
            val n = concatenated.read(lzmaHeader, hdrRead, 5 - hdrRead)
            if (n < 0) error("Unexpected EOF reading LZMA header")
            hdrRead += n
        }
        val lzmaPropsByte = lzmaHeader[0]
        val lzmaDictSize = ByteBuffer.wrap(lzmaHeader, 1, 4).order(ByteOrder.LITTLE_ENDIAN).int

        val lzma = LZMAInputStream(concatenated, -1L, lzmaPropsByte, lzmaDictSize)
        val initialCapacity = minOf(storedSize * EXPECTED_COMPRESSION_RATIO, MAX_UNPACKED_BLOCK_SIZE.toLong()).toInt()
        val out = ByteArrayOutputStream(initialCapacity)
        val buf = ByteArray(8192)
        while (true) {
            val n = try { lzma.read(buf) } catch (e: Exception) { -1 }
            if (n <= 0) break
            out.write(buf, 0, n)
            require(out.size() <= MAX_UNPACKED_BLOCK_SIZE) { "Setup block unpacks to more than $MAX_UNPACKED_BLOCK_SIZE bytes" }
        }
        try { lzma.close() } catch (_: Exception) {}
        return out.toByteArray()
    }
}
