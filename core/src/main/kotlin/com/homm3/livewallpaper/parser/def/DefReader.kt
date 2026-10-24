package com.homm3.livewallpaper.parser.def

import com.homm3.livewallpaper.parser.BinaryReader
import java.io.InputStream
import java.util.Arrays
import java.util.logging.Level
import java.util.logging.Logger

private const val MAX_DIMENSION = 4096
private const val MAX_GROUPS = 256
private const val MAX_FRAMES_PER_GROUP = 10000
private const val SEGMENT_WIDTH = 32

class DefReader(bytes: ByteArray) {
    constructor(stream: InputStream) : this(stream.readBytes())

    private val reader = BinaryReader(bytes)

    fun read(): DefSprite {
        val type = reader.readInt()
        val fullWidth = reader.readInt()
        val fullHeight = reader.readInt()
        val groupsCount = reader.readInt()

        require(groupsCount in 0..MAX_GROUPS) {
            "Invalid groupsCount: $groupsCount"
        }

        val rawPalette = reader.readBytes(256 * 3)

        val groupHeaders = (0 until groupsCount).map { readGroupHeader() }
        val groups = groupHeaders.map { header -> readGroupFrames(header) }

        return DefSprite(type, fullWidth, fullHeight, rawPalette, groups)
    }

    private data class GroupHeader(
        val groupType: Int,
        val filenames: List<String>,
        val framesOffsets: IntArray
    )

    private fun readGroupHeader(): GroupHeader {
        val groupType = reader.readInt()
        val framesCount = reader.readInt()

        require(framesCount in 0..MAX_FRAMES_PER_GROUP) {
            "Invalid framesCount: $framesCount"
        }

        reader.readBytes(8) // unknown
        val filenames = (0 until framesCount).map { reader.readString(13) }
        val framesOffsets = IntArray(framesCount) { reader.readInt() }
        return GroupHeader(groupType, filenames, framesOffsets)
    }

    private fun readGroupFrames(header: GroupHeader): DefGroup {
        val frames = header.framesOffsets.mapIndexed { i, offset ->
            reader.seek(offset.toLong())
            readFrame(header.filenames[i])
        }
        return DefGroup(header.groupType, header.filenames, frames)
    }

    private fun readFrame(frameName: String): DefFrame {
        val size = reader.readInt()
        val compression = reader.readInt()
        val fullWidth = reader.readInt()
        val fullHeight = reader.readInt()
        var width = reader.readInt()
        var height = reader.readInt()
        var x = reader.readInt()
        var y = reader.readInt()

        var dataOffset = reader.position

        // VCMI: detect old format where width/height are swapped with margins
        if (compression == 1 && width > fullWidth && height > fullHeight) {
            width = fullWidth
            height = fullHeight
            x = 0
            y = 0
            dataOffset = reader.position - 16
        }

        require(width in 0..MAX_DIMENSION && height in 0..MAX_DIMENSION) {
            "Invalid frame dimensions: ${width}x${height}"
        }

        reader.seek(dataOffset)

        val data = try {
            when (compression) {
                0 -> decompressType0(width * height)
                1 -> decompressType1(width, height, dataOffset)
                2 -> decompressType2(width, height, dataOffset)
                3 -> decompressType3(width, height, dataOffset)
                else -> decompressType0(width * height)
            }
        } catch (e: IllegalArgumentException) {
            log.log(Level.WARNING, "DEF frame $frameName is corrupt, drawing it transparent", e)
            ByteArray(width * height)
        }

        return DefFrame(frameName, width, height, fullWidth, fullHeight, x, y, data)
    }

    private fun decompressType0(size: Int): ByteArray {
        return reader.readBytes(size)
    }

    private fun decompressType1(width: Int, height: Int, dataOffset: Long): ByteArray {
        val offsets = IntArray(height) { reader.readInt() }

        val output = ByteArray(width * height)
        offsets.forEachIndexed { row, offset ->
            reader.seek(dataOffset + offset)
            var outputIndex = row * width
            var left = width
            while (left > 0) {
                val index = reader.readByte()
                val length = reader.readByte() + 1
                require(length <= left) { "DEF RLE: run overflows row" }
                if (index == 0xFF) {
                    reader.readInto(output, outputIndex, length)
                } else {
                    Arrays.fill(output, outputIndex, outputIndex + length, index.toByte())
                }
                outputIndex += length
                left -= length
            }
        }

        return output
    }

    private fun decodePackedRLELine(output: ByteArray, lineStart: Int, lineWidth: Int) {
        var outputIndex = lineStart
        var left = lineWidth
        while (left > 0) {
            val code = reader.readByte()
            val index = code shr 5
            val length = (code and 0x1F) + 1
            require(length <= left) { "DEF RLE: run overflows row" }
            if (index == 0x7) {
                reader.readInto(output, outputIndex, length)
            } else {
                Arrays.fill(output, outputIndex, outputIndex + length, index.toByte())
            }
            outputIndex += length
            left -= length
        }
    }

    private fun decompressType2(width: Int, height: Int, dataOffset: Long): ByteArray {
        val rowOffsets = IntArray(height) { reader.readShort() }

        val output = ByteArray(width * height)
        rowOffsets.forEachIndexed { row, rowOffset ->
            reader.seek(dataOffset + rowOffset)
            decodePackedRLELine(output, row * width, width)
        }
        return output
    }

    // Each row is split into 32-pixel segments with their own offsets; pixels past the last full segment stay transparent.
    private fun decompressType3(width: Int, height: Int, dataOffset: Long): ByteArray {
        val segmentsPerRow = width / SEGMENT_WIDTH
        val segmentOffsets = IntArray(height * segmentsPerRow) { reader.readShort() }

        val output = ByteArray(width * height)
        for (row in 0 until height) {
            for (segment in 0 until segmentsPerRow) {
                reader.seek(dataOffset + segmentOffsets[row * segmentsPerRow + segment])
                decodePackedRLELine(output, row * width + segment * SEGMENT_WIDTH, SEGMENT_WIDTH)
            }
        }
        return output
    }

    companion object {
        private val log = Logger.getLogger(DefReader::class.java.name)
    }
}
