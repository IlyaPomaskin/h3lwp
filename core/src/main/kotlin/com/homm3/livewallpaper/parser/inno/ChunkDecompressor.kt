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

    /** One file slice within a chunk. */
    data class Slice(
        val fileOffset: Long,
        val fileSize: Long,
        val output: File,
        val tag: String,
    )

    /**
     * Decompresses one chunk and emits each slice to its [Slice.output] file in a single
     * forward pass over the chunk's decompressed bytes. Slices may share `fileOffset`
     * (fan-out: same source bytes → multiple output files); otherwise they must reference
     * non-overlapping byte ranges of the same chunk.
     *
     * [chunkLocation] supplies the chunk-level fields (`chunkOffset`, `chunkCompressed`,
     * `chunkEncrypted`, `firstSlice`, `lastSlice`); the per-slice `file_offset`/`file_size`
     * are taken from [slices]. The caller must pick a single representative
     * `FileLocationEntry` whose chunk-level fields match every slice.
     *
     * @param onProgress called periodically with (bytesWrittenAcrossAllSlices, totalBytes, currentSliceTag)
     */
    fun extractMultiple(
        installer: File,
        offset1: Long,
        chunkLocation: FileLocationEntry,
        slices: List<Slice>,
        onProgress: ((written: Long, total: Long, currentTag: String) -> Unit)? = null,
    ) {
        require(slices.isNotEmpty()) { "No slices to extract" }
        require(!chunkLocation.chunkEncrypted) { "Encrypted chunks not supported" }
        require(chunkLocation.firstSlice == 0 && chunkLocation.lastSlice == 0) { "Multi-slice not supported" }

        val sorted = slices.sortedBy { it.fileOffset }
        val totalBytes = sorted.sumOf { it.fileSize }
        log.info(
            "extractMultiple: offset1=$offset1 chunkOffset=${chunkLocation.chunkOffset} " +
                "absChunk=${offset1 + chunkLocation.chunkOffset} compressed=${chunkLocation.chunkCompressed} " +
                "chunkSize=${chunkLocation.chunkSize} slices=${sorted.size} totalBytes=$totalBytes"
        )

        var totalWritten = 0L
        RandomAccessFile(installer, "r").use { raf ->
            raf.seek(offset1 + chunkLocation.chunkOffset)
            val magic = ByteArray(4); raf.readFully(magic)
            require(magic.contentEquals(CHUNK_MAGIC)) {
                "Bad chunk magic: ${magic.joinToString { "%02x".format(it) }}"
            }

            val input: InputStream = if (chunkLocation.chunkCompressed) {
                val propsByte = raf.read().also { require(it >= 0) { "EOF reading LZMA2 props byte" } }
                require(propsByte <= 40) { "Invalid LZMA2 props byte: 0x${propsByte.toString(16)}" }
                // XZ-format LZMA2 dict-size encoding: dict = (2 or 3) << (propsByte/2 + 11).
                val dictSize = if (propsByte == 40) Int.MAX_VALUE
                               else (if (propsByte % 2 == 0) 2 else 3) shl (propsByte / 2 + 11)
                log.info("LZMA2 propsByte=$propsByte dictSize=$dictSize (${dictSize / (1024 * 1024)} MiB)")
                val raw = RandomAccessFileInputStream(raf)
                LZMA2InputStream(BufferedInputStream(raw, 64 * 1024), dictSize)
            } else {
                RandomAccessFileInputStream(raf)
            }

            try {
                var posInChunk = 0L
                var i = 0
                while (i < sorted.size) {
                    val slice = sorted[i]
                    require(slice.fileOffset >= posInChunk) {
                        "Cannot rewind chunk stream: pos=$posInChunk slice.fileOffset=${slice.fileOffset}"
                    }

                    val toSkip = slice.fileOffset - posInChunk
                    if (toSkip > 0) {
                        skipExact(input, toSkip)
                        posInChunk += toSkip
                    }

                    // Group consecutive slices that share the same (fileOffset, fileSize) —
                    // fan-out: write the same source bytes to multiple output files at once.
                    var j = i + 1
                    val group = mutableListOf(slice)
                    while (j < sorted.size &&
                        sorted[j].fileOffset == slice.fileOffset &&
                        sorted[j].fileSize == slice.fileSize
                    ) {
                        group += sorted[j]; j++
                    }

                    log.info(
                        "[${i + 1}..$j/${sorted.size}] tag='${slice.tag}' " +
                            "fileOffset=${slice.fileOffset} fileSize=${slice.fileSize} fanout=${group.size}"
                    )

                    val outputs = group.map { s ->
                        s.output.parentFile?.mkdirs()
                        s.output.outputStream().buffered()
                    }
                    try {
                        copyToMany(input, slice.fileSize, outputs) { writtenInSlice ->
                            onProgress?.invoke(totalWritten + writtenInSlice, totalBytes, slice.tag)
                        }
                    } finally {
                        outputs.forEach { runCatching { it.close() } }
                    }

                    totalWritten += slice.fileSize
                    posInChunk += slice.fileSize
                    onProgress?.invoke(totalWritten, totalBytes, slice.tag)
                    i = j
                }
            } catch (e: Exception) {
                log.log(
                    Level.SEVERE,
                    "chunk decompression failed at posInChunk=? totalWritten=$totalWritten",
                    e,
                )
                throw e
            }
        }
    }

    private fun skipExact(input: InputStream, n: Long) {
        var remaining = n
        val buf = ByteArray(64 * 1024)
        while (remaining > 0) {
            val want = minOf(buf.size.toLong(), remaining).toInt()
            val r = input.read(buf, 0, want)
            require(r > 0) { "EOF while skipping; $remaining bytes remaining" }
            remaining -= r
        }
    }

    private fun copyToMany(
        input: InputStream,
        totalToCopy: Long,
        outputs: List<OutputStream>,
        onProgressBytes: (Long) -> Unit,
    ) {
        var remaining = totalToCopy
        val buf = ByteArray(64 * 1024)
        var lastReport = 0L
        while (remaining > 0) {
            val want = minOf(buf.size.toLong(), remaining).toInt()
            val r = input.read(buf, 0, want)
            require(r > 0) { "EOF while copying slice; $remaining bytes remaining" }
            for (out in outputs) out.write(buf, 0, r)
            remaining -= r
            val writtenInSlice = totalToCopy - remaining
            if (writtenInSlice - lastReport >= 1024 * 1024) {
                onProgressBytes(writtenInSlice)
                lastReport = writtenInSlice
            }
        }
    }

    /** InputStream adapter over an open RandomAccessFile at its current position. */
    private class RandomAccessFileInputStream(private val raf: RandomAccessFile) : java.io.InputStream() {
        override fun read(): Int = raf.read()
        override fun read(b: ByteArray, off: Int, len: Int): Int = raf.read(b, off, len)
    }
}
