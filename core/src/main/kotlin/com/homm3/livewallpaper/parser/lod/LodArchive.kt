package com.homm3.livewallpaper.parser.lod

data class LodArchive(val files: List<LodEntry>)

data class LodEntry(
    val name: String,
    val offset: Int,
    val size: Int,
    val fileType: LodFileType?,
    val compressedSize: Int
)

enum class LodFileType(val id: Int) {
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
        fun fromInt(value: Int): LodFileType? {
            return entries.find { it.id == value }
        }
    }
}
