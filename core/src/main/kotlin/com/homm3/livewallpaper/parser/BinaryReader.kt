package com.homm3.livewallpaper.parser

import java.nio.charset.Charset

private val MAP_CHARSET = Charset.forName("windows-1251")
private val NUL_TAIL = "\u0000.*".toRegex()
private const val NUL_BYTE: Byte = 0
private const val BYTE_MASK = 0xFF
private const val SHORT_MASK = 0xFFFF

/** Little-endian reader over an in-memory buffer. Reads past the end yield zeros and leave the cursor at the end. */
class BinaryReader(private val data: ByteArray) {
    private var cursor = 0

    val position: Long
        get() = cursor.toLong()

    fun readBool(): Boolean {
        return readByte() == 1
    }

    fun readByte(): Int {
        val value = byteAt(0)
        advance(Byte.SIZE_BYTES)
        return value
    }

    fun readShort(): Int {
        val value = byteAt(0) or (byteAt(1) shl 8)
        advance(Short.SIZE_BYTES)
        return value and SHORT_MASK
    }

    fun readInt(): Int {
        val value = byteAt(0) or (byteAt(1) shl 8) or (byteAt(2) shl 16) or (byteAt(3) shl 24)
        advance(Int.SIZE_BYTES)
        return value
    }

    fun readString(): String {
        return readString(readInt())
    }

    fun readString(length: Int): String {
        val isInBounds = length >= 0 && length <= data.size - cursor
        if (isInBounds && !containsNul(cursor, length)) {
            val text = String(data, cursor, length, MAP_CHARSET)
            advance(length)
            return text
        }
        return String(readBytes(length), MAP_CHARSET).replace(NUL_TAIL, "")
    }

    fun readBytes(length: Int): ByteArray {
        val buffer = ByteArray(length)
        readInto(buffer, 0, length)
        return buffer
    }

    /** Copies [length] bytes into [destination]; bytes past the end of the data are left untouched. */
    fun readInto(destination: ByteArray, offset: Int, length: Int) {
        val available = minOf(length, data.size - cursor)
        System.arraycopy(data, cursor, destination, offset, available)
        advance(length)
    }

    fun skip(amount: Int): Long {
        return skip(amount.toLong())
    }

    fun skip(amount: Long): Long {
        if (amount <= 0) return 0
        val skipped = minOf(amount, (data.size - cursor).toLong())
        cursor += skipped.toInt()
        return skipped
    }

    fun seek(offset: Long) {
        cursor = 0
        skip(offset)
    }

    private fun byteAt(offset: Int): Int {
        val index = cursor + offset
        if (index >= data.size) return 0
        return data[index].toInt() and BYTE_MASK
    }

    private fun advance(count: Int) {
        cursor = minOf(cursor.toLong() + count, data.size.toLong()).toInt()
    }

    private fun containsNul(from: Int, length: Int): Boolean {
        for (index in from until from + length) {
            if (data[index] == NUL_BYTE) return true
        }
        return false
    }
}
