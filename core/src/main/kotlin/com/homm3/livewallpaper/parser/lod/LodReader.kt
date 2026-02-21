package com.homm3.livewallpaper.parser.lod

import com.homm3.livewallpaper.parser.BinaryReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.Inflater

class LodReader(stream: InputStream) {
    private val reader = BinaryReader(stream)

    private val magicHeader = byteArrayOf(
        0x4c.toByte(), 0x4f.toByte(), 0x44.toByte(), 0x00.toByte(),
        0xc8.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
    )

    fun read(): LodArchive {
        val magic = reader.readBytes(8)
        if (!magic.contentEquals(magicHeader)) {
            throw IllegalArgumentException("Wrong file selected: invalid LOD magic header")
        }
        val filesCount = reader.readInt()
        reader.readBytes(80) // unknown

        val files = (0 until filesCount).map { readEntry() }
        return LodArchive(files)
    }

    fun readFileContent(entry: LodEntry): ByteArrayInputStream {
        reader.skip(entry.offset - reader.position)

        return if (entry.compressedSize > 0) {
            val packedData = reader.readBytes(entry.compressedSize)
            val unpackedData = ByteArray(entry.size)
            val inflater = Inflater()
            inflater.setInput(packedData)
            inflater.inflate(unpackedData)
            inflater.end()
            unpackedData.inputStream()
        } else {
            reader.readBytes(entry.size).inputStream()
        }
    }

    private fun readEntry(): LodEntry {
        return LodEntry(
            name = reader.readString(16),
            offset = reader.readInt(),
            size = reader.readInt(),
            fileType = LodFileType.fromInt(reader.readInt()),
            compressedSize = reader.readInt()
        )
    }
}
