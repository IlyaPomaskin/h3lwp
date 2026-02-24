package com.homm3.livewallpaper.parser

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryReader(private val stream: InputStream) {
    var position: Long = 0
        private set

    fun readBool(): Boolean {
        return readByte() == 1
    }

    fun readByte(): Int {
        return readBytes(1)[0].toInt() and 0xFF
    }

    fun readShort(): Int {
        return ByteBuffer.wrap(readBytes(2)).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
    }

    fun readInt(): Int {
        return ByteBuffer.wrap(readBytes(4)).order(ByteOrder.LITTLE_ENDIAN).int
    }

    fun readString(): String {
        return readString(readInt())
    }

    fun readString(length: Int): String {
        return String(readBytes(length)).replace("\u0000.*".toRegex(), "")
    }

    fun readBytes(length: Int): ByteArray {
        val buffer = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val n = read(buffer, offset, length - offset)
            if (n <= 0) break
            offset += n
        }
        return buffer
    }

    fun skip(amount: Int): Long {
        return skip(amount.toLong())
    }

    fun skip(amount: Long): Long {
        var remaining = amount
        while (remaining > 0) {
            val n = stream.skip(remaining)
            if (n <= 0) break
            remaining -= n
        }
        position += amount - remaining
        return amount - remaining
    }

    fun seek(offset: Long) {
        reset()
        skip(offset)
    }

    @Synchronized
    private fun read(b: ByteArray, offset: Int, length: Int): Int {
        val n = stream.read(b, offset, length)
        if (n > 0) {
            position += n.toLong()
        }
        return n
    }

    @Synchronized
    private fun reset() {
        stream.reset()
        position = 0
    }
}
