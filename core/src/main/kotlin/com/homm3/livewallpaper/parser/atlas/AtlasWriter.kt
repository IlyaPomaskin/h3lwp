package com.homm3.livewallpaper.parser.atlas

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.homm3.livewallpaper.parser.lod.LodFileType
import java.io.File
import java.io.OutputStream
import java.util.Locale
import java.util.zip.Deflater

class AtlasWriter(
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

    private fun atlasFrame(name: String, index: String, rect: PixmapPacker.PixmapPackerRectangle, pf: PackableFrame) {
        val spriteName = name.lowercase(Locale.ROOT).replace(".def", "")
        atlas += "$spriteName\n"
        atlas += "  rotate: false\n"
        atlas += "  xy: ${rect.x.toInt()}, ${rect.y.toInt()}\n"
        atlas += "  size: ${pf.frame.width}, ${pf.frame.height}\n"
        atlas += "  orig: ${pf.frame.fullWidth}, ${pf.frame.fullHeight}\n"
        atlas += "  offset: ${pf.frame.x}, ${pf.frame.y}\n"
        atlas += "  index: $index\n"
    }

    private fun writePng(stream: OutputStream, pixmap: Pixmap) {
        val pngFile = PixmapIO.PNG((pixmap.width * pixmap.height * 1.5f).toInt())
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
        pf: PackableFrame
    ) {
        val parts = rectangleName.split("/")
        val defName = parts[0]
        val frameName = parts[1]

        pf.groupFilenames.forEachIndexed { index, fileName ->
            if (fileName != frameName) return@forEachIndexed
            atlasFrame(defName, index.toString(), rectangle, pf)
        }
    }

    private fun writeTerrainInfo(
        rectangleName: String,
        rectangle: PixmapPacker.PixmapPackerRectangle,
        pf: PackableFrame
    ) {
        val parts = rectangleName.split("/")
        val defName = parts[0]
        val frameName = parts[1]
        val rotationIndex = parts[2]

        pf.groupFilenames.forEachIndexed { index, fileName ->
            if (fileName != frameName) return@forEachIndexed
            atlasFrame("$defName/$index", rotationIndex, rectangle, pf)
        }
    }

    fun writePackerContent(sprites: PackedFrames) {
        packer.pages.forEachIndexed { index, page ->
            val pngName = "${atlasName}_${index}.png"
            atlasPage(pngName)
            writePng(outputDirectory.resolve(pngName).outputStream(), page.pixmap)

            page.rects.forEach { entry ->
                val rectangleName = entry.key
                val rectangle = entry.value
                val pf = sprites[rectangleName] ?: return@forEach

                when (pf.fileType) {
                    LodFileType.TERRAIN -> writeTerrainInfo(rectangleName, rectangle, pf)
                    LodFileType.SPRITE -> writeSpriteInfo(rectangleName, rectangle, pf)
                    LodFileType.MAP -> writeSpriteInfo(rectangleName, rectangle, pf)
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
