package com.homm3.livewallpaper.parser.inno

import org.tukaani.xz.LZMAInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ChunkDecompressor {
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
        require(location.chunkCompressed) { "Uncompressed chunks not handled in this version" }
        require(location.firstSlice == 0 && location.lastSlice == 0) { "Multi-slice not supported" }

        RandomAccessFile(installer, "r").use { raf ->
            raf.seek(offset1 + location.chunkOffset)
            val magic = ByteArray(4); raf.readFully(magic)
            require(magic.contentEquals(CHUNK_MAGIC)) {
                "Bad chunk magic: ${magic.joinToString { "%02x".format(it) }}"
            }

            // Parse 5-byte LZMA header (1 byte props + 4 bytes dict size LE)
            val lzmaHeader = ByteArray(5); raf.readFully(lzmaHeader)
            val propsByte = lzmaHeader[0]
            val dictSize = ByteBuffer.wrap(lzmaHeader, 1, 4).order(ByteOrder.LITTLE_ENDIAN).int

            // Wrap remaining file region as an InputStream
            val raw = RandomAccessFileInputStream(raf)
            val lzma = LZMAInputStream(BufferedInputStream(raw, 64 * 1024), -1L, propsByte, dictSize)

            // Skip fileOffset bytes within the decompressed stream
            var toSkip = location.fileOffset
            val skipBuf = ByteArray(64 * 1024)
            while (toSkip > 0) {
                val want = minOf(skipBuf.size.toLong(), toSkip).toInt()
                val n = lzma.read(skipBuf, 0, want)
                require(n > 0) { "Stream ended while skipping prefix; $toSkip bytes remaining" }
                toSkip -= n
            }

            // Copy fileSize bytes to output
            var remaining = location.fileSize
            val buf = ByteArray(64 * 1024)
            var lastProgressReport = 0L
            while (remaining > 0) {
                val want = minOf(buf.size.toLong(), remaining).toInt()
                val n = lzma.read(buf, 0, want)
                require(n > 0) { "Stream ended early; $remaining bytes remaining" }
                out.write(buf, 0, n)
                remaining -= n
                val written = location.fileSize - remaining
                if (onProgress != null && written - lastProgressReport >= 1024 * 1024) {
                    onProgress(written, location.fileSize)
                    lastProgressReport = written
                }
            }
            onProgress?.invoke(location.fileSize, location.fileSize)
        }
    }

    /** InputStream adapter over an open RandomAccessFile at its current position. */
    private class RandomAccessFileInputStream(private val raf: RandomAccessFile) : java.io.InputStream() {
        override fun read(): Int = raf.read()
        override fun read(b: ByteArray, off: Int, len: Int): Int = raf.read(b, off, len)
    }
}
