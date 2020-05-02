package com.heroes3.livewallpaper.parser.formats

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.zip.CRC32
import java.util.zip.Deflater

internal class PngWriter {
    private val output = ByteArrayOutputStream()

    @Throws(IOException::class)
    fun create(width: Int, height: Int, palette: ByteArray, transparent: ByteArray, data: ByteArray): ByteArray {
        output.write(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10))
        writeChunk("IHDR", createIHDR(width, height))
        writeChunk("PLTE", palette)
        writeChunk("tRNS", transparent)
        writeChunk("IDAT", createIDAT(data, width, height))
        writeChunk("IEND", ByteArray(0))
        return output.toByteArray()
    }

    private fun createIDAT(data: ByteArray, width: Int, height: Int): ByteArray {
        val scanlines = ByteArrayOutputStream()
        for (i in 0 until height) {
            scanlines.write(0)
            scanlines.write(
                data.copyOfRange(width * i, width * i + width)
            )
        }
        val input = scanlines.toByteArray()
        val output = ByteArray(input.size + 100) // TODO fix hardcoded value
        val deflater = Deflater()
        deflater.setInput(input)
        deflater.finish()
        deflater.deflate(output)
        deflater.end()
        return output
    }

    private fun createIHDR(width: Int, height: Int): ByteArray {
        val ihdr = ByteArrayOutputStream()
        ihdr.write(
            ByteBuffer
                .allocate(4)
                .putInt(width)
                .array()
        )
        ihdr.write(
            ByteBuffer
                .allocate(4)
                .putInt(height)
                .array()
        )
        ihdr.write(8) // bitDepth
        ihdr.write(3) // colorType
        ihdr.write(0) // compressionMethod
        ihdr.write(0) // filterMethod
        ihdr.write(0) // interlaceMethod
        return ihdr.toByteArray()
    }

    private fun writeChunk(type: String, content: ByteArray) {
        output.write(
            ByteBuffer
                .allocate(4)
                .putInt(content.size)
                .array())
        output.write(type.toByteArray())
        output.write(content)
        output.write(
            ByteBuffer
                .allocate(4)
                .putInt(getCRC(type, content).toInt())
                .array()
        )
    }

    private fun getCRC(type: String, content: ByteArray): Long {
        val crc32 = CRC32()
        crc32.update(type.toByteArray(), 0, type.toByteArray().size)
        crc32.update(content, 0, content.size)
        return crc32.value
    }
}