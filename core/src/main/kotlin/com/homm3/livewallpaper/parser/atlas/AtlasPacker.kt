package com.homm3.livewallpaper.parser.atlas

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.homm3.livewallpaper.parser.def.DefFrame
import com.homm3.livewallpaper.parser.lod.LodFileType
import java.util.Locale
import java.util.logging.Level
import java.util.logging.Logger

data class PackableFrame(
    val frame: DefFrame,
    val defName: String,
    val fileType: LodFileType?,
    val palette: ByteArray?,
    val groupFilenames: List<String>
)

internal typealias PackedFrames = MutableMap<String, PackableFrame>

class AtlasPacker(private val packer: PixmapPacker) {
    private val transparent = byteArrayOf(
        0x00.toByte(), 0x40.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x80.toByte(), 0xff.toByte(), 0x80.toByte(), 0x40.toByte()
    )

    private val paletteRotations = hashMapOf(
        "watrtl.def" to listOf(229 to 241, 242 to 254),
        "lavatl.def" to listOf(246 to 254),
        "clrrvr.def" to listOf(183 to 195, 195 to 201),
        "mudrvr.def" to listOf(183 to 189, 240 to 246),
        "lavrvr.def" to listOf(240 to 248)
    )

    private fun makePixmap(pf: PackableFrame): Pixmap {
        if (pf.palette == null) {
            return makeRgbPixmap(pf)
        }
        val expectedSize = pf.frame.width * pf.frame.height
        require(pf.frame.data.size >= expectedSize) {
            "Data too small: ${pf.frame.data.size} < ${expectedSize} (${pf.frame.width}x${pf.frame.height})"
        }
        require(pf.frame.width > 0 && pf.frame.height > 0) {
            "Zero dimensions: ${pf.frame.width}x${pf.frame.height}"
        }
        val encoder = PngEncoder()
        val pngData = encoder.create(
            pf.frame.width,
            pf.frame.height,
            pf.palette,
            transparent,
            pf.frame.data
        )
        if (pf.frame.frameName.endsWith(".wgd", true)) {
            log.info("PNG for ${pf.defName}/${pf.frame.frameName}: ${pf.frame.width}x${pf.frame.height} full=${pf.frame.fullWidth}x${pf.frame.fullHeight} data=${pf.frame.data.size} png=${pngData.size}")
        }
        return Pixmap(pngData, 0, pngData.size)
    }

    private fun makeRgbPixmap(pf: PackableFrame): Pixmap {
        val pixmap = Pixmap(pf.frame.width, pf.frame.height, Pixmap.Format.RGB888)
        val buffer = pixmap.pixels
        buffer.put(pf.frame.data)
        buffer.flip()
        return pixmap
    }

    private fun makeTerrainPixmap(pf: PackableFrame): Pixmap {
        val image = makePixmap(pf)
        val fullImage = Pixmap(pf.frame.fullWidth, pf.frame.fullHeight, Pixmap.Format.RGBA4444)
        fullImage.drawPixmap(image, pf.frame.x, pf.frame.y)
        image.dispose()
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

    private fun terrainFrame(pf: PackableFrame, acc: PackedFrames): PackedFrames {
        val palette = pf.palette ?: return objectFrame(pf, acc)
        val initialPalette = palette.clone()
        val defName = pf.defName.lowercase(Locale.ROOT)
        val rotations = paletteRotations[defName] ?: emptyList()
        var rotationStep = 0
        do {
            val frameName = pf.defName + "/" + pf.frame.frameName + "/" + rotationStep
            val pixmap = makeTerrainPixmap(pf)
            packer.pack(frameName, pixmap)
            pixmap.dispose()
            acc[frameName] = pf
            rotations.forEach { rotatePalette(palette, it.first, it.second) }
            rotationStep++
        } while (!initialPalette.contentEquals(palette))

        return acc
    }

    private fun objectFrame(pf: PackableFrame, acc: PackedFrames): PackedFrames {
        val frameName = pf.defName + "/" + pf.frame.frameName
        if (pf.frame.width == 0 || pf.frame.height == 0) {
            if (pf.frame.fullWidth == 0 || pf.frame.fullHeight == 0) return acc
            val pixmap = Pixmap(pf.frame.fullWidth, pf.frame.fullHeight, Pixmap.Format.RGBA4444)
            packer.pack(frameName, pixmap)
            pixmap.dispose()
        } else {
            val pixmap = makePixmap(pf)
            packer.pack(frameName, pixmap)
            pixmap.dispose()
        }
        acc[frameName] = pf
        return acc
    }

    fun packFrames(frames: List<PackableFrame>): PackedFrames {
        return frames.foldRight(mutableMapOf()) { pf, acc ->
            try {
                when (pf.fileType) {
                    LodFileType.TERRAIN -> terrainFrame(pf, acc)
                    LodFileType.SPRITE -> objectFrame(pf, acc)
                    LodFileType.MAP -> objectFrame(pf, acc)
                    null -> objectFrame(pf, acc)
                    else -> acc
                }
            } catch (e: Throwable) {
                log.log(Level.WARNING, "Failed to pack ${pf.defName}/${pf.frame.frameName}", e)
                acc
            }
        }
    }

    companion object {
        private val log = Logger.getLogger(AtlasPacker::class.java.name)
    }
}
