package com.homm3.livewallpaper.parser

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.homm3.livewallpaper.parser.formats.Def
import java.io.File
import java.io.InputStream
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

internal typealias PackedFrames = MutableMap<String, Def.Frame>

class InvalidFileException(msg: String) : Exception(msg)
class OutputFileWriteException(msg: String) : Exception(msg)

class AssetsConverter(lodFileInputStream: InputStream, outputDirectory: File, atlasName: String) {
    private val minimalDefCount = 1000
    private val packer = PixmapPacker(2048, 2048, Pixmap.Format.RGBA4444, 0, false)
    private val assetsReader = AssetsReader(lodFileInputStream)
    private val assetsPacker = AssetsPacker(packer)
    private val assetsWriter = AssetsWriter(packer, outputDirectory, atlasName)

    @OptIn(ExperimentalTime::class)
    fun <T> logTime(prefix: String, block: () -> T): T {
        var result: T

        val time = measureTime {
            result = block()
        }

        println("FN_TIME $prefix : $time")

        return result
    }

    @Throws(InvalidFileException::class, OutputFileWriteException::class)
    fun convertLodToTextureAtlas() {
        runCatching { logTime("readFilesList") { assetsReader.readFilesList() } }
            .onFailure { throw InvalidFileException("Can't parse. Try another file.") }
            .mapCatching { logTime("readDefs") { assetsReader.readDefs(it) } }
            .onFailure { throw InvalidFileException("Can't read content. Try another file.") }
            .also { defs ->
                if (defs.getOrDefault(emptyList()).size < minimalDefCount) {
                    throw InvalidFileException("Wrong file selected. Try another file.")
                }
            }
            .mapCatching { logTime("packFrames") { assetsPacker.packFrames(it) } }
            .mapCatching { logTime("writePackerContent") { assetsWriter.writePackerContent(it) } }
            .onFailure { throw OutputFileWriteException("Can't save files. Check free space.") }
    }
}