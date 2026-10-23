package com.homm3.livewallpaper.parser.inno

import org.tukaani.xz.LZMA2InputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.io.FileOutputStream
import java.util.zip.InflaterInputStream
import java.util.zip.InflaterOutputStream
import java.util.logging.Level
import java.util.logging.Logger

object ChunkDecompressor {
    private val log = Logger.getLogger(ChunkDecompressor::class.java.name)
    private val CHUNK_MAGIC = byteArrayOf('z'.code.toByte(), 'l'.code.toByte(), 'b'.code.toByte(), 0x1A)
    private const val READ_BUFFER_SIZE = 64 * 1024

    /** One file slice within a chunk. [append] continues a file started by an earlier slice;
     *  [inflate] marks slice bytes that are a zlib stream of their own (GOG Galaxy parts). */
    data class Slice(
        val fileOffset: Long,
        val fileSize: Long,
        val output: File,
        val tag: String,
        val append: Boolean = false,
        val inflate: Boolean = false,
    )

    /**
     * Decompresses one chunk and emits each slice to its [Slice.output] file in a single
     * forward pass over the chunk's decompressed bytes. Slices may share `fileOffset`
     * (fan-out: same source bytes → multiple output files); otherwise they must reference
     * non-overlapping byte ranges of the same chunk.
     *
     * [chunkLocation] supplies the chunk-level fields (`chunkCompressed`, `chunkEncrypted`);
     * the per-slice `file_offset`/`file_size` are taken from [slices]. The caller must pick a
     * single representative `FileLocationEntry` whose chunk-level fields match every slice.
     * [chunkStart] is the absolute position of the chunk magic inside [source], which is either
     * the installer itself (embedded data) or an external `-N.bin` slice file.
     *
     * @param onProgress called periodically with (bytesWrittenAcrossAllSlices, totalBytes, currentSliceTag)
     */
    fun extractMultiple(
        source: FileChannel,
        chunkStart: Long,
        compression: ChunkCompression,
        chunkLocation: FileLocationEntry,
        slices: List<Slice>,
        onProgress: ((written: Long, total: Long, currentTag: String) -> Unit)? = null,
    ) {
        require(slices.isNotEmpty()) { "No slices to extract" }
        require(!chunkLocation.chunkEncrypted) { "Encrypted chunks not supported" }

        val sorted = slices.sortedBy { it.fileOffset }
        val totalBytes = sorted.sumOf { it.fileSize }
        log.info(
            "extractMultiple: chunkStart=$chunkStart compression=$compression " +
                "compressed=${chunkLocation.chunkCompressed} " +
                "chunkSize=${chunkLocation.chunkSize} slices=${sorted.size} totalBytes=$totalBytes"
        )

        var totalWritten = 0L
        source.position(chunkStart)
        val raw = BufferedInputStream(Channels.newInputStream(source), READ_BUFFER_SIZE)
        val magic = ByteArray(CHUNK_MAGIC.size)
        readFully(raw, magic)
        require(magic.contentEquals(CHUNK_MAGIC)) {
            "Bad chunk magic: ${magic.joinToString { "%02x".format(it) }}"
        }

        val input: InputStream = if (chunkLocation.chunkCompressed) decompressing(raw, compression) else raw

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

                val outputs = group.map(::openOutput)
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

    private fun openOutput(slice: Slice): OutputStream {
        slice.output.parentFile?.mkdirs()
        val file = FileOutputStream(slice.output, slice.append).buffered()
        if (!slice.inflate) return file
        return InflaterOutputStream(file)
    }

    private fun decompressing(raw: InputStream, compression: ChunkCompression): InputStream =
        when (compression) {
            ChunkCompression.LZMA2 -> lzma2(raw)
            ChunkCompression.ZLIB -> InflaterInputStream(raw)
            else -> throw IllegalArgumentException("Unsupported chunk compression: $compression")
        }

    private fun lzma2(raw: InputStream): InputStream {
        val propsByte = raw.read().also { require(it >= 0) { "EOF reading LZMA2 props byte" } }
        require(propsByte <= 40) { "Invalid LZMA2 props byte: 0x${propsByte.toString(16)}" }
        // XZ-format LZMA2 dict-size encoding: dict = (2 or 3) << (propsByte/2 + 11).
        val dictSize = if (propsByte == 40) Int.MAX_VALUE
                       else (if (propsByte % 2 == 0) 2 else 3) shl (propsByte / 2 + 11)
        log.info("LZMA2 propsByte=$propsByte dictSize=$dictSize (${dictSize / (1024 * 1024)} MiB)")
        return LZMA2InputStream(raw, dictSize)
    }

    private fun readFully(input: InputStream, target: ByteArray) {
        var offset = 0
        while (offset < target.size) {
            val read = input.read(target, offset, target.size - offset)
            require(read > 0) { "EOF reading chunk magic" }
            offset += read
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
}
