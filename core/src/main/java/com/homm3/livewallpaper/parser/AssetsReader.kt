package com.homm3.livewallpaper.parser

import com.homm3.livewallpaper.parser.formats.Def
import com.homm3.livewallpaper.parser.formats.DefReader
import com.homm3.livewallpaper.parser.formats.Lod
import com.homm3.livewallpaper.parser.formats.LodReader
import java.io.InputStream

class AssetsReader(lodFileInputStream: InputStream) {
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
        // TODO add event, random dwelling, random town
        "arrow.def", "avwattack.def", "adag.def",
        "avwmon1.def", "avwmon2.def", "avwmon3.def", "avwmon4.def", "avwmon5.def", "avwmon6.def",
        "avarnd1.def", "avarnd2.def", "avarnd3.def", "avarnd4.def", "avarnd5.def", "avtrndm0.def"
    )
    private val lodReader = LodReader(lodFileInputStream)

    private fun readDefFromLod(file: Lod.File): Def {
        val defContentStream = lodReader.readFileContent(file)
        val defReader = DefReader(defContentStream)
        val def = defReader.read()
        def.lodFile = file
        System.arraycopy(fixedPalette, 0, def.rawPalette, 0, fixedPalette.size)
        return def
    }

    internal fun readFilesList(): List<Lod.File> {
        return lodReader
            .read()
            .files
            .filter { lodFile ->
                val isDef = lodFile.name.endsWith(".def", true)
                val isIgnored = ignoredFiles.any { it.equals(lodFile.name, true) }
                isDef && !isIgnored
            }
    }

    internal fun readDefs(lodFiles: List<Lod.File>): List<Def.Frame> {
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
            .map(::readDefFromLod)
            .flatMap { def -> def.groups.flatMap { group -> group.frames } }
            .distinctBy { it.parentGroup.parentDef.lodFile.name + it.frameName }
    }

}