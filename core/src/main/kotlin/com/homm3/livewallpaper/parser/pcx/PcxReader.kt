package com.homm3.livewallpaper.parser.pcx

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PcxReader(stream: InputStream) {
    private val bytes = stream.readBytes()

    fun read(): PcxImage {
        require(bytes.size >= 12) { "PCX file too small" }

        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val fSize = buf.int
        val width = buf.int
        val height = buf.int

        return when (fSize) {
            width * height -> read8bit(width, height)
            width * height * 3 -> read24bit(width, height)
            else -> throw IllegalArgumentException(
                "Unknown PCX format: fSize=$fSize, width=$width, height=$height"
            )
        }
    }

    private fun read8bit(width: Int, height: Int): PcxImage {
        val pixelCount = width * height
        val data = bytes.copyOfRange(12, 12 + pixelCount)
        val paletteOffset = bytes.size - 768
        val palette = bytes.copyOfRange(paletteOffset, paletteOffset + 768)
        return PcxImage(width, height, palette, data)
    }

    private fun read24bit(width: Int, height: Int): PcxImage {
        val pixelCount = width * height
        val dataSize = pixelCount * 3
        val data = bytes.copyOfRange(12, 12 + dataSize)
        // Convert BGR to RGB in-place
        for (i in 0 until pixelCount) {
            val offset = i * 3
            val b = data[offset]
            data[offset] = data[offset + 2]
            data[offset + 2] = b
        }
        return PcxImage(width, height, null, data)
    }
}
