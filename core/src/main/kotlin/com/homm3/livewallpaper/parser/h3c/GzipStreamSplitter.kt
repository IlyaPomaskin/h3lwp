package com.homm3.livewallpaper.parser.h3c

import java.util.logging.Logger
import java.util.zip.Inflater

internal data class GzipStream(
    val compressedStart: Int,
    val compressedEndExclusive: Int,
    val decompressed: ByteArray,
)

internal object GzipStreamSplitter {
    private val log = Logger.getLogger(GzipStreamSplitter::class.java.name)

    private const val GZIP_MAGIC_0: Byte = 0x1f.toByte()
    private const val GZIP_MAGIC_1: Byte = 0x8b.toByte()
    private const val DEFLATE_METHOD: Byte = 0x08
    private const val FLG_FTEXT = 0x01
    private const val FLG_FHCRC = 0x02
    private const val FLG_FEXTRA = 0x04
    private const val FLG_FNAME = 0x08
    private const val FLG_FCOMMENT = 0x10
    private const val TRAILER_BYTES = 8

    fun split(input: ByteArray): List<GzipStream> {
        log.info("split: input=${input.size} bytes")
        val out = mutableListOf<GzipStream>()
        var cursor = 0
        while (cursor < input.size) {
            val stream = readOne(input, cursor)
            log.info(
                "  stream[${out.size}]: compressed=[$cursor,${stream.compressedEndExclusive}) " +
                    "(${stream.compressedEndExclusive - cursor} bytes) → " +
                    "decompressed=${stream.decompressed.size} bytes"
            )
            out += stream
            require(stream.compressedEndExclusive > cursor) {
                "GzipStreamSplitter did not advance at offset $cursor"
            }
            cursor = stream.compressedEndExclusive
        }
        log.info("split: produced ${out.size} streams")
        return out
    }

    private fun readOne(input: ByteArray, start: Int): GzipStream {
        require(input.size - start >= 10) { "Not enough bytes for gzip header at offset $start" }
        require(input[start] == GZIP_MAGIC_0 && input[start + 1] == GZIP_MAGIC_1) {
            "Bad gzip magic at offset $start: " +
                "${"%02x".format(input[start])} ${"%02x".format(input[start + 1])}"
        }
        require(input[start + 2] == DEFLATE_METHOD) {
            "Unsupported compression method ${input[start + 2].toInt() and 0xFF} at offset $start"
        }
        val flg = input[start + 3].toInt() and 0xFF
        var p = start + 10  // fixed gzip header

        if ((flg and FLG_FEXTRA) != 0) {
            val xlen = (input[p].toInt() and 0xFF) or ((input[p + 1].toInt() and 0xFF) shl 8)
            p += 2 + xlen
        }
        if ((flg and FLG_FNAME) != 0) p = skipCString(input, p)
        if ((flg and FLG_FCOMMENT) != 0) p = skipCString(input, p)
        if ((flg and FLG_FHCRC) != 0) p += 2

        val inflater = Inflater(true)  // nowrap = raw deflate, no zlib header
        try {
            inflater.setInput(input, p, input.size - p)
            val buf = ByteArray(1 shl 16)
            val decoded = java.io.ByteArrayOutputStream()
            var iterations = 0
            val maxIterations = 10 * 1024 * 1024 / buf.size + 1024  // generous upper bound
            while (!inflater.finished()) {
                iterations++
                if (iterations > maxIterations) {
                    error(
                        "Inflate spinning at offset $p after $iterations iterations: " +
                            "bytesRead=${inflater.bytesRead} bytesWritten=${inflater.bytesWritten} " +
                            "finished=${inflater.finished()} needsInput=${inflater.needsInput()} " +
                            "needsDict=${inflater.needsDictionary()}"
                    )
                }
                val n = inflater.inflate(buf)
                if (n == 0) {
                    if (inflater.finished()) break
                    if (inflater.needsInput() || inflater.needsDictionary()) {
                        error(
                            "Truncated deflate stream at offset $p: " +
                                "bytesRead=${inflater.bytesRead} needsInput=${inflater.needsInput()} " +
                                "needsDict=${inflater.needsDictionary()}"
                        )
                    }
                    // No progress, no input needed, not finished — would spin. Surface it.
                    error(
                        "Inflate stalled at offset $p: n=0, finished=false, needsInput=false, needsDict=false " +
                            "(bytesRead=${inflater.bytesRead} bytesWritten=${inflater.bytesWritten})"
                    )
                } else {
                    decoded.write(buf, 0, n)
                }
            }
            val consumed = inflater.bytesRead.toInt()
            val end = p + consumed + TRAILER_BYTES
            require(end <= input.size) { "Truncated gzip trailer at offset $p (need $end, have ${input.size})" }
            return GzipStream(start, end, decoded.toByteArray())
        } finally {
            inflater.end()
        }
    }

    private fun skipCString(input: ByteArray, from: Int): Int {
        var i = from
        while (i < input.size && input[i] != 0.toByte()) i++
        require(i < input.size) { "Unterminated cstring in gzip header" }
        return i + 1
    }
}
