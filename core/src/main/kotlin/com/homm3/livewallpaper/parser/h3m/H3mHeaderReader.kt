package com.homm3.livewallpaper.parser.h3m

import com.homm3.livewallpaper.parser.BinaryReader
import com.homm3.livewallpaper.parser.readUpTo
import java.io.InputStream
import java.util.zip.GZIPInputStream

object H3mHeaderReader {
    private const val DEFAULT_SIZE = 36

    // Covers the version ints, the largest HotA header block and the map size field.
    private const val HEADER_PREFIX_SIZE = 64

    private fun readerForHeader(stream: InputStream) =
        BinaryReader(GZIPInputStream(stream).readUpTo(HEADER_PREFIX_SIZE))

    fun readVersion(stream: InputStream): H3mVersion? {
        return try {
            val reader = readerForHeader(stream)
            H3mVersion.fromInt(reader.readInt())
        } catch (e: Exception) {
            null
        }
    }

    fun readMapSize(stream: InputStream): Int {
        return try {
            val reader = readerForHeader(stream)
            val version = H3mVersion.fromInt(reader.readInt())
            var hotaSubVersion = 0
            if (version == H3mVersion.HOTA) {
                hotaSubVersion = reader.readInt()
            }
            if (version == H3mVersion.HOTA) {
                skipHotaHeaderFields(reader, hotaSubVersion)
            }
            reader.readByte() // hasPlayers
            reader.readInt() // size
        } catch (e: Exception) {
            DEFAULT_SIZE
        }
    }

    private fun skipHotaHeaderFields(reader: BinaryReader, hotaSubVersion: Int) {
        if (hotaSubVersion >= 8) {
            reader.skip(12) // version triplet: 3 x uint32
        }
        if (hotaSubVersion >= 1) {
            reader.skip(2) // isMirrorMap + isArenaMap
        }
        if (hotaSubVersion >= 2) {
            reader.skip(4) // terrainTypesCount
        }
        if (hotaSubVersion >= 5) {
            reader.skip(4) // townTypesCount
            reader.skip(1) // allowedDifficultiesMask
        }
        if (hotaSubVersion >= 7) {
            reader.skip(1) // canHireDefeatedHeroes
        }
        if (hotaSubVersion >= 8) {
            reader.skip(1) // forceMatchingVersion
        }
        if (hotaSubVersion >= 9) {
            reader.skip(4) // unknown int32
        }
    }
}
