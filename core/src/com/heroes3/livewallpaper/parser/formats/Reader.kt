package com.heroes3.livewallpaper.parser.formats

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Reader(private val stream: InputStream) {
    var position: Long = 0

    private fun toByteBuffer(bytes: ByteArray): ByteBuffer {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    }

    fun readByte(): Int {
        return java.lang.Byte.toUnsignedInt(
            toByteBuffer(readBytes(1)).array()[0]
        )
    }

    fun readBool(): Boolean {
        return readByte() == 1
    }

    fun readShort(): Int {
        return java.lang.Short.toUnsignedInt(toByteBuffer(readBytes(2)).short)
    }

    fun readInt(): Int {
        return Integer.toUnsignedLong(toByteBuffer(readBytes(4)).int).toInt()
    }

    fun readString(length: Int): String {
        return String(readBytes(length)).replace("\u0000.*".toRegex(), "")
    }

    fun readBytes(length: Int): ByteArray {
        val buffer = ByteArray(length)
        read(buffer, 0, length)
        return buffer
    }

    @Synchronized
    private fun read(b: ByteArray, offset: Int, length: Int): Int {
        val n = stream.read(b, offset, length)
        if (n > 0) {
            position += length.toLong()
        }
        return n
    }

    fun skip(amount: Int): Long {
        return skip(amount.toLong())
    }

    @Synchronized
    fun skip(amount: Long): Long {
        val n = stream.skip(amount)
        if (n > 0) {
            position += amount
        }
        return n
    }

    @Synchronized
    private fun reset() {
        stream.reset()
        position = 0
    }

    fun seek(offset: Long) {
        reset()
        skip(offset)
    }
}