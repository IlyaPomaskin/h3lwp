package com.heroes3.livewallpaper.parser

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.heroes3.livewallpaper.parser.formats.Def
import com.heroes3.livewallpaper.parser.formats.Lod
import com.heroes3.livewallpaper.parser.formats.PngWriter
import java.util.*

class AssetsPacker(private val packer: PixmapPacker) {
    private val transparent = byteArrayOf(
        0x00.toByte(), 0x40.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x80.toByte(), 0xff.toByte(), 0x80.toByte(), 0x40.toByte()
    )
    private val paletteRotations = hashMapOf(
        Pair("lavatl.def", listOf(Pair(246, 254))),
        Pair("clrrvr.def", listOf(Pair(183, 195), Pair(195, 201))),
        Pair("mudrvr.def", listOf(Pair(183, 189), Pair(240, 246))),
        Pair("watrtl.def", listOf(Pair(229, 241), Pair(242, 254))),
        Pair("lavrvr.def", listOf(Pair(240, 248)))
    )

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
        return fullImage
    }

    private fun rotatePalette(array: ByteArray, from: Int, to: Int) {
        val stepSize = 3
        val fromStep = from * stepSize
        val toStep = to * stepSize
        val left = array.copyOfRange(fromStep, fromStep + stepSize)
        val right = array.copyOfRange(fromStep + stepSize, toStep)
        System.arraycopy(right, 0, array, fromStep, right.size)
        System.arraycopy(left, 0, array, fromStep + right.size, left.size)
    }

    private fun terrainFrame(frame: Def.Frame, acc: PackedFrames): PackedFrames {
        val def = frame.parentGroup.parentDef
        val initialPalette = def.rawPalette.clone()
        val defName = def.lodFile.name.toLowerCase(Locale.ROOT)
        val rotations = paletteRotations.getOrDefault(defName, emptyList())
        var rotationStep = 0
        do {
            val frameName = frame.parentGroup.parentDef.lodFile.name + "/" + frame.frameName + "/" + rotationStep
            packer.pack(frameName, makeTerrainPixmap(frame))
            acc[frameName] = frame
            rotations.forEach { rotatePalette(def.rawPalette, it.first, it.second) }
            rotationStep++
        } while (!initialPalette.contentEquals(def.rawPalette))

        frame.x = 0
        frame.y = 0
        frame.width = frame.fullWidth
        frame.height = frame.fullHeight

        return acc
    }

    private fun objectFrame(frame: Def.Frame, acc: PackedFrames): PackedFrames {
        val frameName = frame.parentGroup.parentDef.lodFile.name + "/" + frame.frameName
        packer.pack(frameName, makePixmap(frame))
        acc[frameName] = frame
        return acc
    }

    internal fun packFrames(frames: List<Def.Frame>): PackedFrames {
        return frames.foldRight(
            mutableMapOf(),
            { frame, acc ->
                when (frame.parentGroup.parentDef.lodFile.fileType) {
                    Lod.FileType.TERRAIN -> terrainFrame(frame, acc)
                    Lod.FileType.SPRITE -> objectFrame(frame, acc)
                    Lod.FileType.MAP -> objectFrame(frame, acc)
                    else -> acc
                }
            }
        )
    }
}