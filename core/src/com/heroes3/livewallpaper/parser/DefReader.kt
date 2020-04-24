package com.heroes3.livewallpaper.parser

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

internal class DefReader(stream: InputStream) {
    private val def = Def()
    private val reader = Reader(stream)
    private var streamLength = stream.available()

    init {
        if (!stream.markSupported()) {
            throw IOException("Mark not supported.")
        }
        stream.mark(stream.available())
    }

    @Throws(IOException::class)
    fun read(): Def {
        readHeader()
        readPalette()
        readGroups()
        readFrames()
        return def
    }

    private fun readFrames() {
        for (group in def.groups) {
            group.frames = mutableListOf()
            for (i in 0 until group.framesCount) {
                reader.seek(group.framesOffsets[i].toLong())
                val frame = readFrame(group)
                frame.frameName = group.filenames[i]
                frame.parentGroup = group
                group.frames.add(frame)
            }
        }
    }

    private fun readGroups() {
        def.groups = mutableListOf()
        for (i in 0 until def.groupsCount) {
            val group = readGroup()
            group.parentDef = def
            def.groups.add(group)
        }
    }

    private fun readHeader() {
        def.type = reader.readInt()
        def.fullWidth = reader.readInt()
        def.fullHeight = reader.readInt()
        def.groupsCount = reader.readInt()
    }

    private fun readPalette() {
        def.rawPalette = reader.readBytes(def.rawPalette.size)
    }

    private fun readGroup(): Def.Group {
        val group = Def.Group()
        group.groupType = reader.readInt()
        group.framesCount = reader.readInt()
        group.unknown = reader.readBytes(group.unknown.size)
        group.filenames = mutableListOf()
        for (i in 0 until group.framesCount) {
            group.filenames.add(reader.readString(13))
        }
        group.framesOffsets = IntArray(group.framesCount)
        for (i in 0 until group.framesCount) {
            group.framesOffsets[i] = reader.readInt()
        }
        group.legacy = readIsLegacy(group.framesOffsets)
        return group
    }

    private fun readIsLegacy(framesOffsets: IntArray): Boolean {
        val initialPosition = reader.position

        for (framesOffset in framesOffsets) {
            reader.seek(framesOffset.toLong())
            val size = reader.readInt() + 32
            val frameEnd = size + framesOffset

            if (streamLength != 0 && frameEnd > streamLength) {
                reader.seek(initialPosition)
                return true
            }
        }

        reader.seek(initialPosition)
        return false
    }

    private fun readFrame(group: Def.Group): Def.Frame {
        val frame = Def.Frame()

        frame.size = reader.readInt()
        frame.compression = reader.readInt()
        frame.fullWidth = reader.readInt()
        frame.fullHeight = reader.readInt()

        if (group.legacy) {
            frame.width = frame.fullWidth
            frame.height = frame.fullHeight
            frame.x = 0
            frame.y = 0
        } else {
            frame.width = reader.readInt()
            frame.height = reader.readInt()
            frame.x = reader.readInt()
            frame.y = reader.readInt()
        }

        frame.dataOffset = reader.position

        when (frame.compression) {
            0 -> frame.data = frameCompression0(frame)
            1 -> frame.data = frameCompression1(frame)
            2 -> frame.data = frameCompression2(frame)
            3 -> frame.data = frameCompression3(frame)
        }

        return frame
    }

    private fun frameCompression0(frame: Def.Frame): ByteArray {
        return reader.readBytes(frame.size)
    }

    private fun frameCompression1(frame: Def.Frame): ByteArray {
        val offsets = IntArray(frame.height)

        for (i in 0 until frame.height) {
            offsets[i] = reader.readInt()
        }

        val output = ByteArrayOutputStream(frame.size)
        for (offset in offsets) {
            reader.seek(frame.dataOffset + offset)
            var left = frame.width
            do {
                val index = reader.readByte()
                val length = reader.readByte() + 1
                if (index == 0xFF) {
                    output.write(reader.readBytes(length))
                } else {
                    val array = ByteArray(length)
                    Arrays.fill(array, index.toByte())
                    output.write(array)
                }
                left -= length
            } while (left != 0)
        }

        return output.toByteArray()
    }

    private fun frameCompression2(frame: Def.Frame): ByteArray {
        val offsets = IntArray(frame.height)
        for (i in 0 until frame.height) {
            offsets[i] = reader.readShort()
        }
        reader.skip(2)

        val output = ByteArrayOutputStream(frame.size)
        for (offset in offsets) {
            reader.seek(frame.dataOffset + offset)
            var left = frame.width
            do {
                val code = reader.readByte()
                val index = code shr 5
                val length = (code and 0x1F) + 1
                if (index == 0x7) {
                    output.write(reader.readBytes(length))
                } else {
                    val line = ByteArray(length)
                    Arrays.fill(line, index.toByte())
                    output.write(line)
                }
                left -= length
            } while (left != 0)
        }

        return output.toByteArray()
    }

    private fun frameCompression3(frame: Def.Frame): ByteArray {
        val offsetsCount = frame.height * frame.width / 32
        val offsets = IntArray(offsetsCount)
        for (i in 0 until offsetsCount) {
            offsets[i] = reader.readShort()
        }

        val output = ByteArrayOutputStream(frame.size)
        for (offset in offsets) {
            reader.seek(frame.dataOffset + offset)
            var left = 32
            do {
                val code = reader.readByte()
                val index = code shr 5
                val length = (code and 0x1F) + 1
                if (index == 0x7) {
                    output.write(reader.readBytes(length))
                } else {
                    val line = ByteArray(length)
                    Arrays.fill(line, index.toByte())
                    output.write(line)
                }
                left -= length
            } while (left != 0)
        }

        return output.toByteArray()
    }
}