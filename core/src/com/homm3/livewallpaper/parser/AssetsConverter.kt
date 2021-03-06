package com.homm3.livewallpaper.parser

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.homm3.livewallpaper.parser.formats.Def
import java.io.*
import kotlin.Exception

internal typealias PackedFrames = MutableMap<String, Def.Frame>

class InvalidFileException(msg: String) : Exception(msg)
class OutputFileWriteException(msg: String) : Exception(msg)

class AssetsConverter(lodFileInputStream: InputStream, outputDirectory: File, atlasName: String) {
    private val minimalDefCount = 1000
    private val packer = PixmapPacker(2048, 2048, Pixmap.Format.RGBA4444, 0, false)
    private val assetsReader = AssetsReader(lodFileInputStream)
    private val assetsPacker = AssetsPacker(packer)
    private val assetsWriter = AssetsWriter(packer, outputDirectory, atlasName)

    @Throws(InvalidFileException::class, OutputFileWriteException::class)
    fun convertLodToTextureAtlas() {
        runCatching(assetsReader::readFilesList)
            .onFailure { throw InvalidFileException("Can't parse. Try another file.") }
            .mapCatching(assetsReader::readDefs)
            .onFailure { throw InvalidFileException("Can't read content. Try another file.") }
            .also { defs ->
                if (defs.getOrDefault(emptyList()).size < minimalDefCount) {
                    throw InvalidFileException("Wrong file selected. Try another file.")
                }
            }
            .mapCatching(assetsPacker::packFrames)
            .mapCatching(assetsWriter::writePackerContent)
            .onFailure { throw OutputFileWriteException("Can't save files. Check free space.") }
    }
}