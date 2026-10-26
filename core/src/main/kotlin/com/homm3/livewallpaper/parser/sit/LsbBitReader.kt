package com.homm3.livewallpaper.parser.sit

import java.io.EOFException
import java.io.InputStream

/** Reads bits least significant first. Peeking past the end of [input] sees zeros; consuming them throws. */
internal class LsbBitReader(private val input: InputStream) {
    private var buffer = 0L
    private var bufferedBits = 0
    private var paddingBits = 0

    fun peek(count: Int): Int {
        require(count in 0..MAX_PEEK_BITS) { "Cannot peek $count bits" }
        fill(count)
        return (buffer and ((1L shl count) - 1)).toInt()
    }

    fun skip(count: Int) {
        buffer = buffer ushr count
        bufferedBits -= count
        if (bufferedBits < paddingBits) throw EOFException("Unexpected end of compressed data")
    }

    fun read(count: Int): Int = peek(count).also { skip(count) }

    private fun fill(count: Int) {
        while (bufferedBits < count) {
            val byte = input.read()
            if (byte == END_OF_STREAM) paddingBits += Byte.SIZE_BITS
            val value = byte.coerceAtLeast(0).toLong()
            buffer = buffer or (value shl bufferedBits)
            bufferedBits += Byte.SIZE_BITS
        }
    }

    private companion object {
        const val END_OF_STREAM = -1
        const val MAX_PEEK_BITS = 32
    }
}
