package com.heroes3.livewallpaper.parser

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.badlogic.gdx.math.Rectangle
import java.io.*
import java.util.*
import java.util.zip.Deflater
import kotlin.Exception

private typealias PackedFrames = MutableMap<String, Def.Frame>

class AssetsParser(lodFileInputStream: InputStream, private val outputDirectory: File, private val atlasName: String) {
    companion object {
        private val transparent = byteArrayOf(
            0x00.toByte(), 0x40.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x80.toByte(), 0xff.toByte(), 0x80.toByte(), 0x40.toByte()
        )
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
            // TODO event, random dwelling, random town
            "arrow.def", "avwattack.def", "adag.def",
            "avwmon1.def", "avwmon2.def", "avwmon3.def", "avwmon4.def", "avwmon5.def", "avwmon6.def",
            "avarnd1.def", "avarnd2.def", "avarnd3.def", "avarnd4.def", "avarnd5.def", "avtrndm0.def"
        )
        const val MINIMUM_DEFS_COUNT = 1000
    }

    private val lodReader = LodReader(lodFileInputStream)
    private val packer = PixmapPacker(2048, 2048, Pixmap.Format.RGBA4444, 0, false)

    private fun writePageHeader(writer: Writer, filename: String, packer: PixmapPacker) {
        writer.append("\n")
        writer.append("$filename\n")
        writer.append("size: ${packer.pageWidth},${packer.pageHeight}\n")
        writer.append("format: ${packer.pageFormat.name}\n")
        writer.append("filter: Nearest,Nearest\n")
        writer.append("repeat: none\n")
    }

    private fun writeFrame(
        writer: Writer,
        name: String,
        index: String,
        rect: Rectangle,
        frame: Def.Frame
    ) {
        val spriteName = name.toLowerCase(Locale.ROOT).replace(".def", "")
        writer.append("$spriteName\n")
        writer.append("  rotate: false\n")
        writer.append("  xy: ${rect.x.toInt()}, ${rect.y.toInt()}\n")
        writer.append("  size: ${frame.width}, ${frame.height}\n")
        writer.append("  orig: ${frame.fullWidth}, ${frame.fullHeight}\n")
        writer.append("  offset: ${frame.x}, ${frame.y}\n")
        writer.append("  index: ${index}\n")
    }

    private fun readDefFile(file: Lod.File): Def {
        val defContentStream = lodReader.readFileContent(file)
        val defReader = DefReader(defContentStream)
        val def = defReader.read()
        def.lodFile = file
        System.arraycopy(fixedPalette, 0, def.rawPalette, 0, fixedPalette.size)
        return def
    }

    private fun makePixmap(frame: Def.Frame): Pixmap {
        val pngWriter = PngWriter()
        val pngData = pngWriter.create(
            frame.width,
            frame.height,
            frame.parentGroup.parentDef.rawPalette,
            transparent,
            frame.data
        )
        return Pixmap(pngData, 0, pngData.size)
    }

    private fun makeTerrainPixmap(frame: Def.Frame): Pixmap {
        val image = makePixmap(frame)
        val fullImage = Pixmap(frame.fullWidth, frame.fullHeight, Pixmap.Format.RGBA4444)
        fullImage.drawPixmap(image, frame.x, frame.y)
        frame.x = 0
        frame.y = 0
        frame.width = frame.fullWidth
        frame.height = frame.fullHeight
        return fullImage
    }

    private fun packTerrainFrame(frame: Def.Frame, acc: PackedFrames): PackedFrames {
        val index = 0
        val frameName = frame.parentGroup.parentDef.lodFile.name + "/" + frame.frameName + "/" + index
        packer.pack(frameName, makeTerrainPixmap(frame))
        acc[frameName] = frame
        return acc
    }

    private fun packSpriteFrame(frame: Def.Frame, acc: PackedFrames): PackedFrames {
        val frameName = frame.parentGroup.parentDef.lodFile.name + "/" + frame.frameName
        packer.pack(frameName, makePixmap(frame))
        acc[frameName] = frame
        return acc
    }

    @Throws(Exception::class)
    fun parseLodToAtlas() {
        runCatching(::readLodFiles)
            .onFailure { throw Exception("Can't parse file") }
            .mapCatching(::readDefs)
            .onFailure { throw Exception("Can't read images from file") }
            .also { defs ->
                if (defs.getOrDefault(emptyList()).size < MINIMUM_DEFS_COUNT) {
                    throw Exception("Wrong file selected")
                }
            }
            .mapCatching(::packFrames)
            .onFailure { throw Exception("Can't save files") }
            .mapCatching(::writePackerContent)
            .onFailure { throw Exception("Can't write files") }
    }

    private fun readLodFiles(): List<Lod.File> {
        return lodReader
            .read()
            .files
            .filter { lodFile ->
                val isDef = lodFile.name.endsWith(".def", true)
                val isIgnored = ignoredFiles.any { it.equals(lodFile.name, true) }
                isDef && !isIgnored
            }
    }

    private fun readDefs(lodFiles: List<Lod.File>): List<Def.Frame> {
        val defs = mutableListOf<Lod.File>()

        lodFiles.filterTo(defs, fun(file): Boolean {
            return file.fileType == Lod.FileType.TERRAIN
        })
        lodFiles.filterTo(defs, fun(file): Boolean {
            val isExtraSprite = file.fileType == Lod.FileType.SPRITE
                && file.name.startsWith("av", true)
            val isMapSprite = file.fileType == Lod.FileType.MAP
            return isExtraSprite || isMapSprite
        })

        return defs
            .sortedBy { it.offset }
            .map(::readDefFile)
            .flatMap { def -> def.groups.flatMap { group -> group.frames } }
            .distinctBy { it.parentGroup.parentDef.lodFile.name + it.frameName }
    }

    private fun packFrames(assets: List<Def.Frame>): PackedFrames {
        return assets.foldRight(
            mutableMapOf(),
            { frame, acc ->
                when (frame.parentGroup.parentDef.lodFile.fileType) {
                    Lod.FileType.TERRAIN -> packTerrainFrame(frame, acc)
                    Lod.FileType.SPRITE -> packSpriteFrame(frame, acc)
                    Lod.FileType.MAP -> packSpriteFrame(frame, acc)
                    else -> acc
                }
            }
        )
    }

    private fun writePng(stream: OutputStream, pixmap: Pixmap) {
        val writer = PixmapIO.PNG(pixmap.width * pixmap.height * 1.5f.toInt())
        try {
            writer.setFlipY(false)
            writer.setCompression(Deflater.DEFAULT_COMPRESSION)
            writer.write(stream, pixmap)
        } finally {
            writer.dispose()
        }
    }

    private fun writeSprite(
        writer: Writer,
        rectangleName: String,
        rectangle: PixmapPacker.PixmapPackerRectangle,
        frame: Def.Frame
    ) {
        val rectangleParts = rectangleName.split("/")
        val defName = rectangleParts[0]
        val frameName = rectangleParts[1]

        frame
            .parentGroup
            .filenames
            .forEachIndexed { index, fileName ->
                if (fileName != frameName) return@forEachIndexed

                writeFrame(writer, defName, index.toString(), rectangle, frame)
            }
    }

    private fun writeTerrain(
        writer: Writer,
        rectangleName: String,
        rectangle: PixmapPacker.PixmapPackerRectangle,
        frame: Def.Frame
    ) {
        val rectangleParts = rectangleName.split("/")
        val defName = rectangleParts[0]
        val frameName = rectangleParts[1]
        val rotationIndex = rectangleParts[2]

        frame
            .parentGroup
            .filenames
            .forEachIndexed { index, fileName ->
                if (fileName != frameName) return@forEachIndexed

                writeFrame(writer, "$defName/$index", rotationIndex, rectangle, frame)
            }
    }

    private fun writePackerContent(sprites: PackedFrames) {
        val writer = outputDirectory.resolve("${atlasName}.atlas").writer()

        packer.pages.forEachIndexed { index, page ->
            val pngName = "${atlasName}_${index}.png"
            writePageHeader(writer, pngName, packer)
            writePng(outputDirectory.resolve(pngName).outputStream(), page.pixmap)

            page
                .rects
                .forEach { entry ->
                    val rectangleName = entry.key
                    val rectangle = entry.value
                    val frame = sprites[rectangleName] ?: return@forEach

                    when (frame.parentGroup.parentDef.lodFile.fileType) {
                        Lod.FileType.TERRAIN -> writeTerrain(writer, rectangleName, rectangle, frame)
                        Lod.FileType.SPRITE -> writeSprite(writer, rectangleName, rectangle, frame)
                        Lod.FileType.MAP -> writeSprite(writer, rectangleName, rectangle, frame)
                    }
                }
        }

        writer.close()
    }
}