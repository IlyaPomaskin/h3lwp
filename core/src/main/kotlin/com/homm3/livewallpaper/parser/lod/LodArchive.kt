package com.homm3.livewallpaper.parser.lod

import java.util.Locale

private const val FNV_OFFSET_BASIS = 0x811C9DC5.toInt()
private const val FNV_PRIME = 0x01000193
private const val BYTE_MASK = 0xFF
private const val ASCII_CASE_OFFSET = 'a'.code - 'A'.code

data class LodArchive(val files: List<LodEntry>, val isHota18: Boolean = false) {
    private val entriesByName: Map<String, LodEntry> by lazy {
        files.distinctBy { it.name.lowercase(Locale.ROOT) }.associateBy { it.name.lowercase(Locale.ROOT) }
    }

    /** Case-insensitive lookup; in a hashed archive the name is matched by its FNV-1a hash. First entry wins. */
    fun find(name: String): LodEntry? {
        val entryName = if (isHota18) hashedEntryName(lodNameHash(name)) else name
        return entriesByName[entryName.lowercase(Locale.ROOT)]
    }
}

data class LodEntry(
    val name: String,
    val offset: Int,
    val size: Int,
    val fileType: LodFileType?,
    val compressedSize: Int,
    val compressionMethod: Int = 0
)

internal fun hashedEntryName(nameHash: Int): String = "%08x".format(nameHash)

/** FNV-1a-32 of the name lowercased like the C runtime (ASCII A-Z only), as HotA stores it. */
fun lodNameHash(name: String): Int =
    name.toByteArray(Charsets.ISO_8859_1).fold(FNV_OFFSET_BASIS) { hash, byte ->
        (hash xor asciiLowercase(byte.toInt() and BYTE_MASK)) * FNV_PRIME
    }

private fun asciiLowercase(code: Int): Int {
    val isUppercaseAscii = code in 'A'.code..'Z'.code
    return if (isUppercaseAscii) code + ASCII_CASE_OFFSET else code
}

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
