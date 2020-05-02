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

class AssetsWriter(private val packer: PixmapPacker, private val outputDirectory: File, private val atlasName: String) {
    private val writer = outputDirectory.resolve("${atlasName}.atlas").writer()

    private fun atlasPage(filename: String) {
        writer.append("\n")
        writer.append("$filename\n")
        writer.append("size: ${packer.pageWidth},${packer.pageHeight}\n")
        writer.append("format: ${packer.pageFormat.name}\n")
        writer.append("filter: Nearest,Nearest\n")
        writer.append("repeat: none\n")
    }

    private fun atlasFrame(name: String, index: String, rect: Rectangle, frame: Def.Frame) {
        val spriteName = name.toLowerCase(Locale.ROOT).replace(".def", "")
        writer.append("$spriteName\n")
        writer.append("  rotate: false\n")
        writer.append("  xy: ${rect.x.toInt()}, ${rect.y.toInt()}\n")
        writer.append("  size: ${frame.width}, ${frame.height}\n")
        writer.append("  orig: ${frame.fullWidth}, ${frame.fullHeight}\n")
        writer.append("  offset: ${frame.x}, ${frame.y}\n")
        writer.append("  index: ${index}\n")
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

                atlasFrame( "$defName/$index", rotationIndex, rectangle, frame)
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
                    }
                }
        }

        writer.close()
    }
}