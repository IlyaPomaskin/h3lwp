package com.homm3.livewallpaper.parser.formats

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

open class Reader(private val stream: InputStream) {
    var position: Long = 0

    private fun toByteBuffer(bytes: ByteArray): ByteBuffer {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    }

    fun readBool(): Boolean {
        return readByte() == 1
    }

    open fun readByte(): Int {
        return toByteBuffer(readBytes(1))[0].toInt().and(0xFF)
    }

    open fun readShort(): Int {
        return toByteBuffer(readBytes(2)).short.toInt().and(0xFFFF)
    }

    open fun readInt(): Int {
        //TODO change return type to Long
        return toByteBuffer(readBytes(4)).int.and(0x7FFFFFFF)
    }

    open fun readString(): String {
        return readString(readInt())
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