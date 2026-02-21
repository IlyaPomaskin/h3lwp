package com.homm3.livewallpaper.parser.def

import com.homm3.livewallpaper.parser.BinaryReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Arrays

class DefReader(stream: InputStream) {
    private val reader = BinaryReader(stream)
    private val streamLength = stream.available()

    init {
        require(stream.markSupported()) { "Mark not supported on input stream" }
        stream.mark(stream.available())
    }

    fun read(): DefSprite {
        val type = reader.readInt()
        val fullWidth = reader.readInt()
        val fullHeight = reader.readInt()
        val groupsCount = reader.readInt()
        val rawPalette = reader.readBytes(256 * 3)

        val groupHeaders = (0 until groupsCount).map { readGroupHeader() }
        val groups = groupHeaders.map { header -> readGroupFrames(header) }

        return DefSprite(type, fullWidth, fullHeight, rawPalette, groups)
    }

    private data class GroupHeader(
        val groupType: Int,
        val filenames: List<String>,
        val framesOffsets: IntArray,
        val isLegacy: Boolean
    )

    private fun readGroupHeader(): GroupHeader {
        val groupType = reader.readInt()
        val framesCount = reader.readInt()
        reader.readBytes(8) // unknown
        val filenames = (0 until framesCount).map { reader.readString(13) }
        val framesOffsets = IntArray(framesCount) { reader.readInt() }
        val isLegacy = detectLegacyFormat(framesOffsets)
        return GroupHeader(groupType, filenames, framesOffsets, isLegacy)
    }

    private fun readGroupFrames(header: GroupHeader): DefGroup {
        val frames = header.framesOffsets.mapIndexed { i, offset ->
            reader.seek(offset.toLong())
            readFrame(header.isLegacy, header.filenames[i])
        }
        return DefGroup(header.groupType, header.filenames, frames, header.isLegacy)
    }

    private fun detectLegacyFormat(framesOffsets: IntArray): Boolean {
        val initialPosition = reader.position

        for (offset in framesOffsets) {
            reader.seek(offset.toLong())
            val size = reader.readInt() + 32
            val frameEnd = size + offset

            if (streamLength != 0 && frameEnd > streamLength) {
                reader.seek(initialPosition)
                return true
            }
        }

        reader.seek(initialPosition)
        return false
    }

    private fun readFrame(isLegacy: Boolean, frameName: String): DefFrame {
        val size = reader.readInt()
        val compression = reader.readInt()
        val fullWidth = reader.readInt()
        val fullHeight = reader.readInt()

        val width: Int
        val height: Int
        val x: Int
        val y: Int

        if (isLegacy) {
            width = fullWidth
            height = fullHeight
            x = 0
            y = 0
        } else {
            width = reader.readInt()
            height = reader.readInt()
            x = reader.readInt()
            y = reader.readInt()
        }

        val dataOffset = reader.position

        val data = when (compression) {
            0 -> decompressType0(size)
            1 -> decompressType1(width, height, dataOffset)
            2 -> decompressType2(width, height, dataOffset)
            3 -> decompressType3(width, height, dataOffset)
            else -> decompressType0(size)
        }

        return DefFrame(frameName, width, height, fullWidth, fullHeight, x, y, data)
    }

    private fun decompressType0(size: Int): ByteArray {
        return reader.readBytes(size)
    }

    private fun decompressType1(width: Int, height: Int, dataOffset: Long): ByteArray {
        val offsets = IntArray(height) { reader.readInt() }

        val output = ByteArrayOutputStream()
        for (offset in offsets) {
            reader.seek(dataOffset + offset)
            var left = width
            do {
                val index = reader.readByte()
                val length = reader.readByte() + 1
                if (index == 0xFF) {
                    output.write(reader.readBytes(length))
                } else {
                    val fill = ByteArray(length)
                    Arrays.fill(fill, index.toByte())
                    output.write(fill)
                }
                left -= length
            } while (left != 0)
        }

        return output.toByteArray()
    }

    private fun decodePackedRLE(offsets: IntArray, blockWidth: Int, dataOffset: Long): ByteArray {
        val output = ByteArrayOutputStream()
        for (offset in offsets) {
            reader.seek(dataOffset + offset)
            var left = blockWidth
            do {
                val code = reader.readByte()
                val index = code shr 5
                val length = (code and 0x1F) + 1
                if (index == 0x7) {
                    output.write(reader.readBytes(length))
                } else {
                    val fill = ByteArray(length)
                    Arrays.fill(fill, index.toByte())
                    output.write(fill)
                }
                left -= length
            } while (left != 0)
        }
        return output.toByteArray()
    }

    private fun decompressType2(width: Int, height: Int, dataOffset: Long): ByteArray {
        val offsets = IntArray(height) { reader.readShort() }
        reader.skip(2)
        return decodePackedRLE(offsets, width, dataOffset)
    }

    private fun decompressType3(width: Int, height: Int, dataOffset: Long): ByteArray {
        val blocksCount = height * width / 32
        val offsets = IntArray(blocksCount) { reader.readShort() }
        return decodePackedRLE(offsets, 32, dataOffset)
    }
}
