package com.homm3.livewallpaper.parser.atlas

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.homm3.livewallpaper.parser.lod.LodFileType
import java.io.BufferedOutputStream
import java.io.File
import java.io.OutputStream
import java.util.Locale
import java.util.zip.Deflater

class AtlasWriter(
    private val packer: PixmapPacker,
    private val outputDirectory: File,
    private val atlasName: String
) {
    private val atlas = StringBuilder()

    private fun atlasPage(filename: String) {
        atlas.append('\n')
        atlas.append(filename).append('\n')
        atlas.append("size: ").append(packer.pageWidth).append(',').append(packer.pageHeight).append('\n')
        atlas.append("format: ").append(packer.pageFormat.name).append('\n')
        atlas.append("filter: Nearest,Nearest\n")
        atlas.append("repeat: none\n")
    }

    private fun atlasFrame(name: String, index: String, rect: PixmapPacker.PixmapPackerRectangle, pf: PackableFrame, isTerrain: Boolean) {
        val spriteName = name.lowercase(Locale.ROOT).replace(".def", "")
        atlas.append(spriteName).append('\n')
        atlas.append("  rotate: false\n")
        atlas.append("  xy: ").append(rect.x.toInt()).append(", ").append(rect.y.toInt()).append('\n')
        if (isTerrain) {
            atlas.append("  size: ").append(pf.frame.fullWidth).append(", ").append(pf.frame.fullHeight).append('\n')
            atlas.append("  orig: ").append(pf.frame.fullWidth).append(", ").append(pf.frame.fullHeight).append('\n')
            atlas.append("  offset: 0, 0\n")
        } else {
            atlas.append("  size: ").append(pf.frame.width).append(", ").append(pf.frame.height).append('\n')
            atlas.append("  orig: ").append(pf.frame.fullWidth).append(", ").append(pf.frame.fullHeight).append('\n')
            atlas.append("  offset: ").append(pf.frame.x).append(", ").append(pf.frame.y).append('\n')
        }
        atlas.append("  index: ").append(index).append('\n')
    }

    private fun writePng(stream: OutputStream, pixmap: Pixmap) {
        val pngFile = PixmapIO.PNG((pixmap.width * pixmap.height * 1.5f).toInt())
        try {
            pngFile.setFlipY(false)
            pngFile.setCompression(Deflater.BEST_SPEED)
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
            atlasFrame(defName, index.toString(), rectangle, pf, isTerrain = false)
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
            atlasFrame("$defName/$index", rotationIndex, rectangle, pf, isTerrain = true)
        }
    }

    fun writePackerContent(sprites: PackedFrames, onProgress: (Int, Int) -> Unit = { _, _ -> }) {
        val totalPages = packer.pages.size
        packer.pages.forEachIndexed { index, page ->
            onProgress(index + 1, totalPages)
            val pngName = "${atlasName}_${index}.png"
            atlasPage(pngName)
            writePng(BufferedOutputStream(outputDirectory.resolve(pngName).outputStream()), page.pixmap)

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

        outputDirectory.resolve("${atlasName}.atlas").bufferedWriter().use {
            it.write(atlas.toString())
        }
    }
}
