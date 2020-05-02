package com.heroes3.livewallpaper.parser

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.heroes3.livewallpaper.parser.formats.Def
import java.io.*
import kotlin.Exception

internal typealias PackedFrames = MutableMap<String, Def.Frame>

class AssetsConverter(lodFileInputStream: InputStream, outputDirectory: File, atlasName: String) {
    private val minimalDefCount = 1000
    private val packer = PixmapPacker(2048, 2048, Pixmap.Format.RGBA4444, 0, false)
    private val assetsReader = AssetsReader(lodFileInputStream)
    private val assetsPacker = AssetsPacker(packer)
    private val assetsWriter = AssetsWriter(packer, outputDirectory, atlasName)

    @Throws(Exception::class)
    fun convertLodToTextureAtlas() {
        runCatching(assetsReader::readFilesList)
            .onFailure { throw Exception("Can't parse file") }
            .mapCatching(assetsReader::readDefs)
            .onFailure { throw Exception("Can't read images from file") }
            .also { defs ->
                if (defs.getOrDefault(emptyList()).size < minimalDefCount) {
                    throw Exception("Wrong file selected")
                }
            }
            .mapCatching(assetsPacker::packFrames)
            .onFailure { throw Exception("Can't save files") }
            .mapCatching(assetsWriter::writePackerContent)
            .onFailure { throw Exception("Can't write files") }
    }
}