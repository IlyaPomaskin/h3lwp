package com.homm3.livewallpaper.core.assets

import com.homm3.livewallpaper.parser.h3m.H3mReader
import java.io.File

enum class MapValidity { VALID, INVALID }

/**
 * Remembers whether each map file parsed, in an append-only file shared by the wallpaper and the settings screen.
 * Entries are keyed by name, length and modification time, so a replaced file counts as unchecked again.
 */
class MapValidityStore(private val storeFile: File) {
    private data class Entry(val fileName: String, val length: Long, val lastModified: Long, val validity: MapValidity) {
        fun matches(mapFile: File): Boolean = length == mapFile.length() && lastModified == mapFile.lastModified()

        fun serialize(): String = listOf(fileName, length, lastModified, validity).joinToString(FIELD_SEPARATOR)
    }

    /** The recorded validity of each of [mapFiles] whose entry still matches the file; unchecked files are left out. */
    fun knownValidities(mapFiles: Collection<File>): Map<String, MapValidity> {
        val entries = readEntries()
        return mapFiles
            .mapNotNull { mapFile -> entries[mapFile.name]?.takeIf { it.matches(mapFile) } }
            .associate { it.fileName to it.validity }
    }

    fun record(mapFile: File, validity: MapValidity) {
        val entry = Entry(mapFile.name, mapFile.length(), mapFile.lastModified(), validity)
        synchronized(fileLock) {
            storeFile.parentFile?.mkdirs()
            storeFile.appendText(entry.serialize() + LINE_SEPARATOR, Charsets.UTF_8)
        }
    }

    /** Parses [mapFile] unless its validity is already recorded, and records the result. */
    fun check(mapFile: File): MapValidity {
        val known = knownValidities(listOf(mapFile))[mapFile.name]
        if (known != null) return known
        val validity = parseValidity(mapFile)
        record(mapFile, validity)
        return validity
    }

    private fun readEntries(): Map<String, Entry> {
        val lines = synchronized(fileLock) {
            if (!storeFile.exists()) return emptyMap()
            storeFile.readLines(Charsets.UTF_8)
        }
        return lines.mapNotNull(::parseEntry).associateBy { it.fileName }
    }

    private fun parseEntry(line: String): Entry? {
        val fields = line.split(FIELD_SEPARATOR)
        if (fields.size != FIELD_COUNT) return null
        val (fileName, length, lastModified, validity) = fields
        return Entry(
            fileName = fileName,
            length = length.toLongOrNull() ?: return null,
            lastModified = lastModified.toLongOrNull() ?: return null,
            validity = MapValidity.entries.find { it.name == validity } ?: return null,
        )
    }

    companion object {
        private const val FIELD_SEPARATOR = "\t"
        private const val LINE_SEPARATOR = "\n"
        private const val FIELD_COUNT = 4
        private val fileLock = Any()

        fun parseValidity(mapFile: File): MapValidity = try {
            mapFile.inputStream().use { H3mReader.read(it) }
            MapValidity.VALID
        } catch (e: Exception) {
            MapValidity.INVALID
        }
    }
}
