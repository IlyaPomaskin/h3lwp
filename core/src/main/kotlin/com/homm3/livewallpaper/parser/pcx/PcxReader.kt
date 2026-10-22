package com.homm3.livewallpaper.parser.pcx

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val HEADER_SIZE = 12
private const val PALETTE_SIZE = 256 * 3

class PcxReader(stream: InputStream) {
    private val bytes = stream.readBytes()

    fun read(): PcxImage {
        require(bytes.size >= HEADER_SIZE) { "PCX file too small" }

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
        val paletteOffset = HEADER_SIZE + pixelCount
        require(bytes.size >= paletteOffset + PALETTE_SIZE) { "PCX missing palette" }
        val data = bytes.copyOfRange(HEADER_SIZE, paletteOffset)
        val palette = bytes.copyOfRange(paletteOffset, paletteOffset + PALETTE_SIZE)
        return PcxImage(width, height, palette, data)
    }

    private fun read24bit(width: Int, height: Int): PcxImage {
        val pixelCount = width * height
        val dataSize = pixelCount * 3
        require(bytes.size >= HEADER_SIZE + dataSize) { "PCX truncated" }
        val data = bytes.copyOfRange(HEADER_SIZE, HEADER_SIZE + dataSize)
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
