package com.homm3.livewallpaper.parser.atlas

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.homm3.livewallpaper.parser.def.DefReader
import com.homm3.livewallpaper.parser.def.DefSprite
import com.homm3.livewallpaper.parser.lod.LodEntry
import com.homm3.livewallpaper.parser.lod.LodFileType
import com.homm3.livewallpaper.parser.lod.LodReader
import java.io.File
import java.io.InputStream

class InvalidFileException(msg: String) : Exception(msg)
class OutputFileWriteException(msg: String) : Exception(msg)

class AtlasConverter(
    lodFileInputStream: InputStream,
    private val outputDirectory: File,
    private val atlasName: String
) {
    private val minimalDefCount = 1000
    private val packer = PixmapPacker(2048, 2048, Pixmap.Format.RGBA4444, 0, false)
    private val lodReader = LodReader(lodFileInputStream)
    private val atlasPacker = AtlasPacker(packer)
    private val atlasWriter = AtlasWriter(packer, outputDirectory, atlasName)

    private val fixedPalette = byteArrayOf(
        0, 0, 0,
        0, 0, 0,
        0, 0, 0,
        0, 0, 0,
        0, 0, 0,
        0x80.toByte(), 0x80.toByte(), 0x80.toByte(),
        0, 0, 0,
        0, 0, 0
    )

    private val ignoredFiles = listOf(
        "arrow.def", "avwattack.def", "adag.def",
        "avwmon1.def", "avwmon2.def", "avwmon3.def", "avwmon4.def", "avwmon5.def", "avwmon6.def",
        "avarnd1.def", "avarnd2.def", "avarnd3.def", "avarnd4.def", "avarnd5.def", "avtrndm0.def"
    )

    @Throws(InvalidFileException::class, OutputFileWriteException::class)
    fun convert() {
        runCatching(::readFilesList)
            .onFailure { throw InvalidFileException("Can't parse. Try another file.") }
            .mapCatching(::readDefs)
            .onFailure { throw InvalidFileException("Can't read content. Try another file.") }
            .also { defs ->
                if (defs.getOrDefault(emptyList()).size < minimalDefCount) {
                    throw InvalidFileException("Wrong file selected. Try another file.")
                }
            }
            .mapCatching(atlasPacker::packFrames)
            .mapCatching(atlasWriter::writePackerContent)
            .onFailure { throw OutputFileWriteException("Can't save files. Check free space.") }
    }

    private fun readFilesList(): List<LodEntry> {
        return lodReader
            .read()
            .files
            .filter { lodFile ->
                val isDef = lodFile.name.endsWith(".def", true)
                val isIgnored = ignoredFiles.any { it.equals(lodFile.name, true) }
                isDef && !isIgnored
            }
    }

    private fun readDefs(lodFiles: List<LodEntry>): List<PackableFrame> {
        val defs = mutableListOf<LodEntry>()

        lodFiles.filterTo(defs) { file ->
            file.fileType == LodFileType.TERRAIN
        }
        lodFiles.filterTo(defs) { file ->
            val isExtraSprite = file.fileType == LodFileType.SPRITE
                && file.name.startsWith("av", true)
            val isMapSprite = file.fileType == LodFileType.MAP
            isExtraSprite || isMapSprite
        }

        return defs
            .sortedBy { it.offset }
            .flatMap { entry -> readDefFromLod(entry) }
            .distinctBy { it.defName + it.frame.frameName }
    }

    private fun readDefFromLod(entry: LodEntry): List<PackableFrame> {
        val stream = lodReader.readFileContent(entry)
        val defReader = DefReader(stream)
        val def = defReader.read()
        val palette = def.rawPalette.clone()
        System.arraycopy(fixedPalette, 0, palette, 0, fixedPalette.size)

        return def.groups.flatMap { group ->
            group.frames.map { frame ->
                PackableFrame(frame, entry.name, entry.fileType, palette.clone(), group.filenames)
            }
        }
    }
}
