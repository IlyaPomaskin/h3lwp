package com.heroes3.livewallpaper.parser

import com.heroes3.livewallpaper.parser.Lod.File
import java.io.*
import java.util.zip.Inflater

internal class LodReader(stream: InputStream) {
    private val lod = Lod()
    private var reader = Reader(stream)

    @Throws(IOException::class)
    fun read(): Lod {
        readHeader()
        readFiles()
        return lod
    }

    private fun readHeader() {
        lod.magic = reader.readBytes(lod.magic.size)
        lod.filesCount = reader.readInt()
        lod.unknown = reader.readBytes(lod.unknown.size)
        lod.files = mutableListOf()
    }

    private fun readFiles() {
        for (i in 0 until lod.filesCount) {
            lod.files.add(readFile())
        }
    }

    private fun readFile(): File {
        val file = File()
        file.name = reader.readString(16)
        file.offset = reader.readInt()
        file.size = reader.readInt()
        file.fileType = Lod.FileType.getByValue(reader.readInt())
        file.compressedSize = reader.readInt()
        return file
    }

    fun readFileContent(file: File): ByteArrayInputStream {
        reader.skip(file.offset - reader.position)

        return if (file.compressedSize > 0) {
            val packedData = reader.readBytes(file.compressedSize)
            val unpackedData = ByteArray(file.size)
            val inflater = Inflater()
            inflater.setInput(packedData)
            inflater.inflate(unpackedData)
            inflater.end()
            unpackedData.inputStream()
        } else {
            reader.readBytes(file.size).inputStream()
        }
    }

    companion object {
        fun readFileContent(fileStream: InputStream, file: File): ByteArrayInputStream {
            val fileContent = ByteArray(file.size)
            fileStream.reset()
            fileStream.skip(file.offset.toLong())
//            fileStream.skip(file.offset - fileStream.channel.position())

            if (file.compressedSize > 0) {
                val packedData = ByteArray(file.compressedSize)
                fileStream.read(packedData)
                val inflater = Inflater()
                inflater.setInput(packedData)
                inflater.inflate(fileContent)
                inflater.end()
            } else {
                fileStream.read(fileContent)
            }

            return fileContent.inputStream()
        }
    }
}