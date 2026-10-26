package com.homm3.livewallpaper.parser.sit

import java.io.InputStream
import java.io.OutputStream

/** StuffIt compression method 13: LZSS over a 64 KiB window with Huffman-coded literals, lengths and offsets. */
internal object StuffIt13Decoder {
    private const val DYNAMIC_CODES = 0
    private const val PRESET_SHIFT = 4
    private const val SHARED_CODE_FLAG = 0x08
    private const val OFFSET_SYMBOLS_MASK = 0x07
    private const val MIN_OFFSET_SYMBOLS = 10
    private const val LITERAL_AND_LENGTH_SYMBOLS = 321

    private const val FIRST_LENGTH_SYMBOL = 0x100
    private const val MEDIUM_LENGTH_SYMBOL = 0x13E
    private const val LONG_LENGTH_SYMBOL = 0x13F
    private const val MIN_MATCH_LENGTH = 3
    private const val EXTENDED_MATCH_LENGTH = 65
    private const val MEDIUM_LENGTH_BITS = 10
    private const val LONG_LENGTH_BITS = 15

    private const val META_RESET_LENGTH = 31
    private const val META_INCREMENT_LENGTH = 32
    private const val META_DECREMENT_LENGTH = 33
    private const val META_REPEAT_MAYBE_ONCE = 34
    private const val META_REPEAT_SHORT = 35
    private const val META_REPEAT_LONG = 36
    private const val UNUSED_LENGTH = -1
    private const val SHORT_REPEAT_BITS = 3
    private const val SHORT_REPEAT_MIN = 2
    private const val LONG_REPEAT_BITS = 6
    private const val LONG_REPEAT_MIN = 10

    private val metaCode = PrefixCode.fromLowBitFirstCodes(
        codes = intArrayOf(
            0x5D8, 0x058, 0x040, 0x0C0, 0x000, 0x078, 0x02B, 0x014,
            0x00C, 0x01C, 0x01B, 0x00B, 0x010, 0x020, 0x038, 0x018,
            0x0D8, 0xBD8, 0x180, 0x680, 0x380, 0xF80, 0x780, 0x480,
            0x080, 0x280, 0x3D8, 0xFD8, 0x7D8, 0x9D8, 0x1D8, 0x004,
            0x001, 0x002, 0x007, 0x003, 0x008,
        ),
        lengths = intArrayOf(
            11, 8, 8, 8, 8, 7, 6, 5, 5, 5, 5, 6, 5, 6, 7, 7, 9, 12, 10, 11, 11, 12,
            12, 11, 11, 11, 12, 12, 12, 12, 12, 5, 2, 2, 3, 4, 5,
        ),
    )

    fun decode(input: InputStream, output: OutputStream, uncompressedSize: Long, onProgress: (written: Long) -> Unit) {
        val reader = LsbBitReader(input)
        val header = reader.read(Byte.SIZE_BITS)
        val presetCodes = header ushr PRESET_SHIFT
        require(presetCodes == DYNAMIC_CODES) { "StuffIt method 13 with preset codes ($presetCodes) is not supported" }

        val firstCode = readCode(reader, LITERAL_AND_LENGTH_SYMBOLS)
        val isSecondCodeShared = header and SHARED_CODE_FLAG != 0
        val secondCode = if (isSecondCodeShared) firstCode else readCode(reader, LITERAL_AND_LENGTH_SYMBOLS)
        val offsetCode = readCode(reader, (header and OFFSET_SYMBOLS_MASK) + MIN_OFFSET_SYMBOLS)

        val window = SlidingWindow(output, uncompressedSize, onProgress)
        var code = firstCode
        while (!window.isComplete) {
            val symbol = code.decode(reader)
            if (symbol < FIRST_LENGTH_SYMBOL) {
                window.literal(symbol)
                code = firstCode
                continue
            }

            code = secondCode
            val isEndOfData = symbol > LONG_LENGTH_SYMBOL
            if (isEndOfData) break
            val length = matchLength(symbol, reader)
            window.match(matchOffset(offsetCode.decode(reader), reader), length)
        }
        window.flush()
        require(window.isComplete) { "Compressed data ended after ${window.written} of $uncompressedSize bytes" }
    }

    private fun matchLength(symbol: Int, reader: LsbBitReader): Int = when (symbol) {
        MEDIUM_LENGTH_SYMBOL -> reader.read(MEDIUM_LENGTH_BITS) + EXTENDED_MATCH_LENGTH
        LONG_LENGTH_SYMBOL -> reader.read(LONG_LENGTH_BITS) + EXTENDED_MATCH_LENGTH
        else -> symbol - FIRST_LENGTH_SYMBOL + MIN_MATCH_LENGTH
    }

    private fun matchOffset(bitLength: Int, reader: LsbBitReader): Int {
        if (bitLength < 2) return bitLength + 1
        val extraBits = bitLength - 1
        return (1 shl extraBits) + reader.read(extraBits) + 1
    }

    /** Reads code lengths as meta-code symbols: set, adjust or repeat the current length. */
    private fun readCode(reader: LsbBitReader, symbolCount: Int): PrefixCode {
        val lengths = IntArray(symbolCount)
        var length = 0
        var index = 0
        fun emit() {
            if (index < symbolCount) lengths[index] = length
            index++
        }

        while (index < symbolCount) {
            when (val meta = metaCode.decode(reader)) {
                META_RESET_LENGTH -> length = UNUSED_LENGTH
                META_INCREMENT_LENGTH -> length++
                META_DECREMENT_LENGTH -> length--
                META_REPEAT_MAYBE_ONCE -> if (reader.read(1) == 1) emit()
                META_REPEAT_SHORT -> repeat(reader.read(SHORT_REPEAT_BITS) + SHORT_REPEAT_MIN) { emit() }
                META_REPEAT_LONG -> repeat(reader.read(LONG_REPEAT_BITS) + LONG_REPEAT_MIN) { emit() }
                else -> length = meta + 1
            }
            emit()
        }
        return PrefixCode.fromLengths(lengths)
    }

    private class SlidingWindow(
        private val output: OutputStream,
        private val size: Long,
        private val onProgress: (Long) -> Unit,
    ) {
        private val bytes = ByteArray(WINDOW_SIZE)
        private var flushed = 0L
        var written = 0L
            private set
        val isComplete: Boolean get() = written >= size

        fun literal(value: Int) {
            bytes[(written and WINDOW_MASK).toInt()] = value.toByte()
            written++
            val isWindowFull = written and WINDOW_MASK == 0L
            if (isWindowFull) flush()
        }

        fun match(offset: Int, length: Int) {
            require(offset <= written) { "Match offset $offset points before the start of data" }
            val count = minOf(length.toLong(), size - written).toInt()
            repeat(count) { literal(bytes[((written - offset) and WINDOW_MASK).toInt()].toInt()) }
        }

        fun flush() {
            val count = (written - flushed).toInt()
            if (count == 0) return
            output.write(bytes, (flushed and WINDOW_MASK).toInt(), count)
            flushed = written
            onProgress(written)
        }

        private companion object {
            const val WINDOW_SIZE = 1 shl 16
            const val WINDOW_MASK = WINDOW_SIZE - 1L
        }
    }
}
