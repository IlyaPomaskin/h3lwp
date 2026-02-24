package com.homm3.livewallpaper.parser.def

import com.homm3.livewallpaper.parser.BinaryReader
import java.io.InputStream

private const val D32_MAGIC = 0x46323344 // "D32F" in little-endian
private const val MAX_DIMENSION = 4096
private const val MAX_GROUPS = 256
private const val MAX_FRAMES_PER_GROUP = 10000

class D32Reader(stream: InputStream) {
    private val reader = BinaryReader(stream)

    init {
        require(stream.markSupported()) { "Mark not supported on input stream" }
        stream.mark(stream.available())
    }

    fun read(): DefSprite {
        val magic = reader.readInt()
        require(magic == D32_MAGIC) { "Invalid D32 magic: ${magic.toString(16)}" }

        reader.readInt() // unknown
        val width = reader.readInt()
        val height = reader.readInt()
        val groupCount = reader.readInt()
        reader.readInt() // unknown
        reader.readInt() // unknown
        reader.readInt() // unknown

        require(groupCount in 0..MAX_GROUPS) { "Invalid groupCount: $groupCount" }

        val groups = (0 until groupCount).map { readGroup() }

        return DefSprite(
            type = 0,
            fullWidth = width,
            fullHeight = height,
            rawPalette = ByteArray(0), // signals direct-color (no palette)
            groups = groups
        )
    }

    private fun readGroup(): DefGroup {
        val groupType = reader.readInt()
        val framesCount = reader.readInt()
        reader.readInt() // unknown
        reader.readInt() // unknown

        require(framesCount in 0..MAX_FRAMES_PER_GROUP) { "Invalid framesCount: $framesCount" }

        val filenames = (0 until framesCount).map { reader.readString(13) }
        val offsets = IntArray(framesCount) { reader.readInt() }

        val frames = offsets.mapIndexed { i, offset ->
            reader.seek(offset.toLong())
            readFrame(filenames[i])
        }

        return DefGroup(groupType, filenames, frames)
    }

    private fun readFrame(frameName: String): DefFrame {
        reader.readInt() // size
        reader.readInt() // compression (always 0 for D32)
        val fullWidth = reader.readInt()
        val fullHeight = reader.readInt()
        val storedW = reader.readInt()
        val storedH = reader.readInt()
        val x = reader.readInt()
        val y = reader.readInt()
        reader.readInt() // unknown
        reader.readInt() // unknown

        require(storedW in 0..MAX_DIMENSION && storedH in 0..MAX_DIMENSION) {
            "Invalid D32 frame dimensions: ${storedW}x${storedH}"
        }

        // Read raw BGRA pixels and convert to RGBA with vertical flip
        val pixelCount = storedW * storedH
        val bgraData = reader.readBytes(pixelCount * 4)
        val rgbaData = ByteArray(pixelCount * 4)

        for (row in 0 until storedH) {
            val srcRow = storedH - 1 - row // vertical flip
            for (col in 0 until storedW) {
                val srcIdx = (srcRow * storedW + col) * 4
                val dstIdx = (row * storedW + col) * 4
                rgbaData[dstIdx] = bgraData[srcIdx + 2]     // R <- B
                rgbaData[dstIdx + 1] = bgraData[srcIdx + 1] // G <- G
                rgbaData[dstIdx + 2] = bgraData[srcIdx]     // B <- R
                rgbaData[dstIdx + 3] = bgraData[srcIdx + 3] // A <- A
            }
        }

        return DefFrame(frameName, storedW, storedH, fullWidth, fullHeight, x, y, rgbaData)
    }
}
