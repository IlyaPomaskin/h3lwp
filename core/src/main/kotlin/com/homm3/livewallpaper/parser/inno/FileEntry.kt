package com.homm3.livewallpaper.parser.inno

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class FileEntry(
    val destination: String,
    val locationIndex: Int,
    val afterInstall: String = "",
    val beforeInstall: String = "",
)

object FileEntries {
    // Empirically confirmed against HotA 1.8.0 (Inno Setup 5.5.7u).
    // file_entry layout: 10 PascalStrings + 43 fixed bytes.
    //
    // Strings (10): source, destination, install_font_name, strong_assembly_name,
    //               + 6 condition_data: components, tasks, languages, check,
    //                                   after_install, before_install
    //
    // Fixed (43 bytes):
    //   min_version       10 bytes  (TWindowsVersion=4 + ServicePack=4 + Build=2)
    //   only_below_version 10 bytes
    //   location           4 bytes  u32 (index into data entries; 0xFFFFFFFF = no data)
    //   attributes         4 bytes  u32
    //   external_size      8 bytes  u64
    //   permission         2 bytes  i16 (-1 = no restriction)
    //   flags              4 bytes  stored_flags u32
    //   type               1 byte   enum
    private const val WINVER_BYTES = 10
    private const val PERMISSION_BYTES = 2  // i16 for 5.5.7u
    private const val FLAGS_BYTES = 4       // 4-byte stored_flags for 5.5.7u
    private const val CHECK_CONDITION_STRINGS = 4 // components, tasks, languages, check; then after_install, before_install

    /** Scans all [count] entries, collecting every one matching [predicate].
     *  Always consumes the full entry list so the cursor lands on data entries. */
    fun collectMatching(
        stream: InputStream,
        count: Int,
        predicate: (destination: String) -> Boolean,
    ): List<FileEntry> = readAll(stream, count).filter { predicate(it.destination) }

    /** Reads all [count] entries so the cursor lands on the entries that follow. */
    fun readAll(stream: InputStream, count: Int): List<FileEntry> = List(count) { readOne(stream) }

    private fun readOne(stream: InputStream): FileEntry {
        PascalString.skip(stream)                                          // source
        val destination = PascalString.read(stream)
        PascalString.skip(stream)                                          // install_font_name
        PascalString.skip(stream)                                          // strong_assembly_name
        repeat(CHECK_CONDITION_STRINGS) { PascalString.skip(stream) }      // condition_data
        val afterInstall = PascalString.read(stream)
        val beforeInstall = PascalString.read(stream)
        stream.skipNBytes(WINVER_BYTES.toLong())
        stream.skipNBytes(WINVER_BYTES.toLong())
        val location = readU32(stream)
        stream.skipNBytes(4L) // attributes
        stream.skipNBytes(8L) // external_size u64
        stream.skipNBytes(PERMISSION_BYTES.toLong())
        stream.skipNBytes(FLAGS_BYTES.toLong())
        stream.skipNBytes(1L) // type enum
        return FileEntry(destination, location, afterInstall, beforeInstall)
    }

    /** Scans up to [count] file entries, returns the first whose `destination` ends in [suffix].
     *  Always consumes the full entry list so the cursor lands on data entries. */
    fun findByDestinationSuffix(
        stream: InputStream,
        count: Int,
        suffix: String,
        caseInsensitive: Boolean,
    ): FileEntry? {
        var found = false
        val matches = collectMatching(stream, count) { dest ->
            if (!found && dest.endsWith(suffix, ignoreCase = caseInsensitive)) {
                found = true
                true
            } else false
        }
        return matches.firstOrNull()
    }

    private fun readU32(stream: InputStream): Int {
        val b = ByteArray(4)
        var off = 0
        while (off < 4) {
            val n = stream.read(b, off, 4 - off)
            require(n > 0) { "EOF reading u32" }
            off += n
        }
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int
    }
}
