package com.heroes3.livewallpaper.parser

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.heroes3.livewallpaper.parser.formats.Def
import com.heroes3.livewallpaper.parser.formats.Lod
import com.heroes3.livewallpaper.parser.formats.PngWriter

class AssetsPacker(private val packer: PixmapPacker) {
    private val transparent = byteArrayOf(
        0x00.toByte(), 0x40.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x80.toByte(), 0xff.toByte(), 0x80.toByte(), 0x40.toByte()
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


    internal fun packFrames(assets: List<Def.Frame>): PackedFrames {
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
}