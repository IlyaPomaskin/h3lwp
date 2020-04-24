package com.heroes3.livewallpaper.parser

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.badlogic.gdx.math.Rectangle
import java.io.*
import java.util.*

class AssetsParser(private val lodFileInputStream: FileInputStream) {
    companion object {
        private val transparent = byteArrayOf(
            0x00.toByte(),
            0x40.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x80.toByte(),
            0xff.toByte(),
            0x80.toByte(),
            0x40.toByte()
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
    }

    private val lodReader = LodReader(BufferedInputStream(lodFileInputStream))
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

    private fun readDefFile(fileStream: FileInputStream, file: Lod.File): Def {
        val defContentStream = LodReader.readFileContent(fileStream, file)
        val defReader = DefReader(defContentStream)
        val def = defReader.read()
        def.lodFile = file
        System.arraycopy(fixedPalette, 0, def.rawPalette, 0, fixedPalette.size)
        return def
    }

    private fun readFrames(files: List<Lod.File>): List<Def.Frame> {
        return files
            .map { file -> readDefFile(lodFileInputStream, file) }
            .flatMap { def -> def.groups.flatMap { group -> group.frames } }
            .distinctBy { it.frameName }
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

    @Throws(IOException::class)
    fun parseLodToAtlas(outputDirectory: FileHandle, atlasName: String) {
        val defList = lodReader
            .read()
            .files
            .filter { lodFile ->
                val isDef = lodFile.name.endsWith(".def", true)
                val isIgnored = ignoredFiles.any { it.equals(lodFile.name, true) }
                isDef && !isIgnored
            }

        val terrains = defList
            .filter { it.fileType == Lod.FileType.TERRAIN }
            .run(::readFrames)
            .foldRight(
                mutableMapOf<String, Def.Frame>(),
                { frame, acc ->
                    val index = 0
                    val frameName = "${frame.frameName}_${index}"
                    packer.pack(frameName, makeTerrainPixmap(frame))
                    acc[frameName] = frame
                    acc
                }
            )

        val sprites = defList
            .filter {
                val isExtraSprite = it.fileType == Lod.FileType.SPRITE && it.name.startsWith("av", true)
                val isMapSprite = it.fileType == Lod.FileType.MAP
                isExtraSprite || isMapSprite
            }
            .run(::readFrames)
            .foldRight(
                mutableMapOf<String, Def.Frame>(),
                { frame, acc ->
                    val frameName = frame.frameName
                    packer.pack(frameName, makePixmap(frame))
                    acc[frameName] = frame
                    acc
                }
            )

        pack((sprites + terrains) as MutableMap<String, Def.Frame>, outputDirectory, atlasName)
    }

    private fun pack(
        sprites: MutableMap<String, Def.Frame>,
        outputDirectory: FileHandle,
        atlasName: String
    ) {
        val writer = outputDirectory
            .child("${atlasName}.atlas")
            .writer(false)
        val terrainRegex = Regex("([a-z]+[0-9]+.pcx)_([0-1]+)", RegexOption.IGNORE_CASE)

        packer.pages.forEachIndexed { index, page ->
            val pngName = "${atlasName}_${index}.png"
            writePageHeader(writer, pngName, packer)
            PixmapIO.writePNG(outputDirectory.child(pngName), page.pixmap)

            page.rects.forEach { entry ->
                val rectName = entry.key
                val rect = entry.value
                val frame = sprites[rectName] ?: return@forEach
                frame
                    .parentGroup
                    .filenames
                    .forEachIndexed(fun(index, fileName) {
                        val defName = frame.parentGroup.parentDef.lodFile.name
                        val matchResult = terrainRegex.findAll(rectName).toList()
                        val isTerrain = matchResult.isNotEmpty()
                        if (isTerrain) {
                            val name = matchResult[0].groupValues[1]
                            val rotationIndex = matchResult[0].groupValues[2]

                            if (fileName == name) {
                                writeFrame(writer, "$defName/$index", rotationIndex, rect, frame)
                            }
                        } else {
                            if (fileName == rectName) {
                                writeFrame(writer, defName, index.toString(), rect, frame)
                            }
                        }
                    })
            }
        }

        writer.close()
    }
}