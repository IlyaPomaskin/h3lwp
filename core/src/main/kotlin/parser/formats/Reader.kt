package com.homm3.livewallpaper.parser.formats

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder


open class Reader(private val stream: InputStream) {
    var position: Long = 0

    fun readBool(): Boolean {
        return readByte() == 1
    }

    open fun readByte(): Int {
        return convertByteArrayToInt(readBytes(1))
    }

    open fun readShort(): Int {
        return convertByteArrayToInt(readBytes(2))
    }

    open fun readInt(): Int {
        //TODO change return type to Long
        return convertByteArrayToInt(readBytes(4))
    }

    private fun convertByteArrayToInt(data: ByteArray): Int {
        var value = 0
        for (k in data.size - 1 downTo 0) {
            value = value shl 8 or data[k].toInt().and(0xFF)
        }
        if (value < 0) {
            value = value.and(0x7FFFFFFF)
        }
        return value

    }

    open fun readString(): String {
        return readString(readInt())
    }

    fun readString(length: Int): String {
        return String(readBytes(length)).replace("\u0000.*".toRegex(), "")
    }

    open fun readBytes(length: Int): ByteArray {
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