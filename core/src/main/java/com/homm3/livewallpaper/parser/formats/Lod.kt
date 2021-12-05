package com.homm3.livewallpaper.parser.formats

internal class Lod {
    var magic = ByteArray(8)
    var filesCount = 0
    var unknown = ByteArray(80)
    var files: MutableList<File> = mutableListOf()

    internal class File {
        lateinit var name: String
        var offset = 0
        var size = 0
        var fileType: FileType? = null
        var compressedSize = 0
    }

    enum class FileType(val fileType: Int) {
        SPELL(0x40),
        SPRITE(0x41),
        CREATURE(0x42),
        MAP(0x43),
        MAP_HERO(0x44),
        TERRAIN(0x45),
        CURSOR(0x46),
        INTERFACE(0x47),
        SPRITE_FRAME(0x48),
        BATTLE_HERO(0x49);

        companion object {
            fun getByValue(value: Int) : FileType? {
                return values().find { it.fileType == value }
            }
        }

    }
}