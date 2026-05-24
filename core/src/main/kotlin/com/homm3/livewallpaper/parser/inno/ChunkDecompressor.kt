package com.homm3.livewallpaper.parser.inno

import org.tukaani.xz.LZMA2InputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.logging.Level
import java.util.logging.Logger

object ChunkDecompressor {
    private val log = Logger.getLogger(ChunkDecompressor::class.java.name)
    private val CHUNK_MAGIC = byteArrayOf('z'.code.toByte(), 'l'.code.toByte(), 'b'.code.toByte(), 0x1A)

    /**
     * Extracts one file from the setup-1 stream.
     * @param installer the .exe file
     * @param offset1 absolute byte offset of the setup-1 stream's start
     * @param location data_entry describing where the file lives within setup-1
     * @param out output stream — caller closes it
     * @param onProgress called with (bytesWritten, fileSize) periodically (~every 1 MB)
     */
    fun extract(
        installer: File,
        offset1: Long,
        location: FileLocationEntry,
        out: OutputStream,
        onProgress: ((Long, Long) -> Unit)? = null,
    ) {
        require(!location.chunkEncrypted) { "Encrypted chunks not supported" }
        require(location.firstSlice == 0 && location.lastSlice == 0) { "Multi-slice not supported" }

        log.info(
            "extract: offset1=$offset1 chunkOffset=${location.chunkOffset} " +
                "absChunk=${offset1 + location.chunkOffset} " +
                "fileOffset=${location.fileOffset} fileSize=${location.fileSize} " +
                "chunkSize=${location.chunkSize} compressed=${location.chunkCompressed}"
        )

        RandomAccessFile(installer, "r").use { raf ->
            raf.seek(offset1 + location.chunkOffset)

            if (location.chunkCompressed) {
                extractCompressed(raf, location, out, onProgress)
            } else {
                extractStored(raf, location, out, onProgress)
            }
        }
    }

    /**
     * Handles LZMA2-compressed chunks (preceded by "zlb\x1a" magic + 1-byte LZMA2 filter props).
     *
     * Inno Setup 5.5.7u / 5.6.0u stores compressed chunks as:
     *   zlb\x1a magic  (4 bytes)
     *   LZMA2 props    (1 byte) — encodes the dictionary size per the XZ-format convention:
     *                             props ≤ 40; dictSize = 2 << (props / 2 + 10) when props is even,
     *                                         (3 << (props / 2 + 9))          when props is odd.
     *   LZMA2 bitstream (raw LZMA2 data, no further header)
     */
    private fun extractCompressed(
        raf: RandomAccessFile,
        location: FileLocationEntry,
        out: OutputStream,
        onProgress: ((Long, Long) -> Unit)?,
    ) {
        val magic = ByteArray(4); raf.readFully(magic)
        require(magic.contentEquals(CHUNK_MAGIC)) {
            "Bad chunk magic: ${magic.joinToString { "%02x".format(it) }}"
        }

        // Read 1-byte LZMA2 filter properties and decode the dictionary size.
        val propsByte = raf.read().also { require(it >= 0) { "EOF reading LZMA2 props byte" } }
        require(propsByte <= 40) { "Invalid LZMA2 props byte: 0x${propsByte.toString(16)}" }
        // XZ-format LZMA2 dict-size encoding: dict = (2 or 3) << (propsByte/2 + 11).
        // propsByte=40 is the special "max" marker (4 GiB → Int.MAX_VALUE in practice).
        val dictSize = if (propsByte == 40) Int.MAX_VALUE
                       else (if (propsByte % 2 == 0) 2 else 3) shl (propsByte / 2 + 11)

        log.info("LZMA2 propsByte=$propsByte dictSize=$dictSize (${dictSize / (1024 * 1024)} MiB)")

        val raw = RandomAccessFileInputStream(raf)
        val lzma2 = LZMA2InputStream(BufferedInputStream(raw, 64 * 1024), dictSize)

        try {
            copyWithSkipAndLimit(lzma2, location.fileOffset, location.fileSize, out, onProgress)
        } catch (e: Exception) {
            log.log(
                Level.SEVERE,
                "LZMA2 decompression failed: propsByte=$propsByte dictSize=$dictSize " +
                    "fileOffset=${location.fileOffset} fileSize=${location.fileSize}",
                e,
            )
            throw e
        }
    }

    /** Handles stored (uncompressed) chunks — zlb magic + raw bytes. */
    private fun extractStored(
        raf: RandomAccessFile,
        location: FileLocationEntry,
        out: OutputStream,
        onProgress: ((Long, Long) -> Unit)?,
    ) {
        // Stored chunks still have the zlb\x1a magic header
        val magic = ByteArray(4); raf.readFully(magic)
        require(magic.contentEquals(CHUNK_MAGIC)) {
            "Bad chunk magic (stored): ${magic.joinToString { "%02x".format(it) }}"
        }
        // Raw data follows directly (no LZMA header for stored chunks)
        val raw = RandomAccessFileInputStream(raf)
        copyWithSkipAndLimit(raw, location.fileOffset, location.fileSize, out, onProgress)
    }

    private fun copyWithSkipAndLimit(
        input: InputStream,
        skipBytes: Long,
        limitBytes: Long,
        out: OutputStream,
        onProgress: ((Long, Long) -> Unit)?,
    ) {
        // Skip fileOffset bytes
        var toSkip = skipBytes
        val skipBuf = ByteArray(64 * 1024)
        while (toSkip > 0) {
            val want = minOf(skipBuf.size.toLong(), toSkip).toInt()
            val n = input.read(skipBuf, 0, want)
            require(n > 0) { "Stream ended while skipping prefix; $toSkip bytes remaining" }
            toSkip -= n
        }

        // Copy limitBytes bytes to output
        var remaining = limitBytes
        val buf = ByteArray(64 * 1024)
        var lastProgressReport = 0L
        while (remaining > 0) {
            val want = minOf(buf.size.toLong(), remaining).toInt()
            val n = input.read(buf, 0, want)
            require(n > 0) { "Stream ended early; $remaining bytes remaining" }
            out.write(buf, 0, n)
            remaining -= n
            val written = limitBytes - remaining
            if (onProgress != null && written - lastProgressReport >= 1024 * 1024) {
                onProgress(written, limitBytes)
                lastProgressReport = written
            }
        }
        onProgress?.invoke(limitBytes, limitBytes)
    }

    /** InputStream adapter over an open RandomAccessFile at its current position. */
    private class RandomAccessFileInputStream(private val raf: RandomAccessFile) : java.io.InputStream() {
        override fun read(): Int = raf.read()
        override fun read(b: ByteArray, off: Int, len: Int): Int = raf.read(b, off, len)
    }
}
