package com.homm3.livewallpaper.parser

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.badlogic.gdx.math.Rectangle
import com.homm3.livewallpaper.parser.formats.Def
import com.homm3.livewallpaper.parser.formats.Lod
import java.io.File
import java.io.OutputStream
import java.util.*
import java.util.zip.Deflater

class AssetsWriter(
    private val packer: PixmapPacker,
    private val outputDirectory: File,
    private val atlasName: String
) {
    private var atlas = String()

    private fun atlasPage(filename: String) {
        atlas += "\n"
        atlas += "$filename\n"
        atlas += "size: ${packer.pageWidth},${packer.pageHeight}\n"
        atlas += "format: ${packer.pageFormat.name}\n"
        atlas += "filter: Nearest,Nearest\n"
        atlas += "repeat: none\n"
    }

    private fun atlasFrame(name: String, index: String, rect: Rectangle, frame: Def.Frame) {
        val spriteName = name.lowercase(Locale.ROOT).replace(".def", "")
        atlas += "$spriteName\n"
        atlas += "  rotate: false\n"
        atlas += "  xy: ${rect.x.toInt()}, ${rect.y.toInt()}\n"
        atlas += "  size: ${frame.width}, ${frame.height}\n"
        atlas += "  orig: ${frame.fullWidth}, ${frame.fullHeight}\n"
        atlas += "  offset: ${frame.x}, ${frame.y}\n"
        atlas += "  index: ${index}\n"
    }

    private fun writePng(stream: OutputStream, pixmap: Pixmap) {
        val pngFile = PixmapIO.PNG(pixmap.width * pixmap.height * 1.5f.toInt())
        try {
            pngFile.setFlipY(false)
            pngFile.setCompression(Deflater.DEFAULT_COMPRESSION)
            pngFile.write(stream, pixmap)
        } finally {
            pngFile.dispose()
        }
    }

    private fun writeSpriteInfo(
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

                atlasFrame(defName, index.toString(), rectangle, frame)
            }
    }

    private fun writeTerrainInfo(
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

                atlasFrame("$defName/$index", rotationIndex, rectangle, frame)
            }
    }

    internal fun writePackerContent(sprites: PackedFrames) {
        packer.pages.forEachIndexed { index, page ->
            val pngName = "${atlasName}_${index}.png"
            atlasPage(pngName)
            writePng(outputDirectory.resolve(pngName).outputStream(), page.pixmap)

            page
                .rects
                .forEach { entry ->
                    val rectangleName = entry.key
                    val rectangle = entry.value
                    val frame = sprites[rectangleName] ?: return@forEach

                    when (frame.parentGroup.parentDef.lodFile.fileType) {
                        Lod.FileType.TERRAIN -> writeTerrainInfo(rectangleName, rectangle, frame)
                        Lod.FileType.SPRITE -> writeSpriteInfo(rectangleName, rectangle, frame)
                        Lod.FileType.MAP -> writeSpriteInfo(rectangleName, rectangle, frame)
                        else -> {}
                    }
                }
        }

        packer.dispose()

        val writer = outputDirectory.resolve("${atlasName}.atlas").writer()
        writer.write(atlas)
        writer.close()

    }
}