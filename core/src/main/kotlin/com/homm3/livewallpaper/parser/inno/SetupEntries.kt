package com.homm3.livewallpaper.parser.inno

import java.io.InputStream

/**
 * Skips all non-file setup entries in the decompressed setup-0 stream.
 *
 * Inno Setup 5.5.7u entry layouts (empirically confirmed against HotA 1.8.0):
 *
 *   Language:  10 PascalStrings + 21 fixed bytes (5×u32 + 1×u8)
 *   Message:    2 PascalStrings +  4 fixed bytes (1×i32 language_id)
 *   Permission: 1 PascalString  +  0 fixed bytes (raw length-prefixed blob)
 *   Type:       4 PascalStrings + 30 fixed bytes
 *   Component:  5 PascalStrings + 42 fixed bytes
 *     (extra_disk_space u64=8, level i32=4, used bool=1,
 *      winver_range=20, options flags=1, size u64=8)
 *   Task:       6 PascalStrings + 26 fixed bytes
 *     (level i32=4, used bool=1, winver_range=20, options flags=1)
 *   Directory:  7 PascalStrings + 27 fixed bytes
 *     (name + 6 from item.load_condition_data;
 *      attributes u32=4, winver_range=20, permission i16=2, options flags=1)
 *
 * After calling [skipUpToFileEntries], the stream is positioned at the first
 * file entry (TSetupFileEntry) where the consuming code can begin reading.
 */
object SetupEntries {

    fun skipUpToFileEntries(stream: InputStream, header: SetupHeader) {
        repeat(header.languageCount)   { skipLanguageEntry(stream) }
        repeat(header.messageCount)    { skipMessageEntry(stream) }
        repeat(header.permissionCount) { skipPermissionEntry(stream) }
        repeat(header.typeCount)       { skipTypeEntry(stream) }
        repeat(header.componentCount)  { skipComponentEntry(stream) }
        repeat(header.taskCount)       { skipTaskEntry(stream) }
        repeat(header.directoryCount)  { skipDirectoryEntry(stream) }
    }

    // 10 PascalStrings + 21 fixed bytes
    private fun skipLanguageEntry(s: InputStream) {
        repeat(10) { PascalString.skip(s) }
        s.skipNBytes(21)
    }

    // 2 PascalStrings + 4 fixed bytes (i32 language_id)
    private fun skipMessageEntry(s: InputStream) {
        PascalString.skip(s)
        PascalString.skip(s)
        s.skipNBytes(4)
    }

    // 1 length-prefixed blob (treated as a single PascalString skip)
    private fun skipPermissionEntry(s: InputStream) {
        PascalString.skip(s)
    }

    // 4 PascalStrings + 30 fixed bytes
    private fun skipTypeEntry(s: InputStream) {
        repeat(4) { PascalString.skip(s) }
        s.skipNBytes(30)
    }

    // 5 PascalStrings + 42 fixed bytes
    // Fields: name, description, types, languages, check,
    //         extra_disk_space(8), level(4), used(1), winver(20), options(1), size(8)
    private fun skipComponentEntry(s: InputStream) {
        repeat(5) { PascalString.skip(s) }
        s.skipNBytes(42)
    }

    // 6 PascalStrings + 26 fixed bytes
    // Fields: name, description, group_description, components, languages, check,
    //         level(4), used(1), winver(20), options(1)
    private fun skipTaskEntry(s: InputStream) {
        repeat(6) { PascalString.skip(s) }
        s.skipNBytes(26)
    }

    // 7 PascalStrings + 27 fixed bytes
    // Fields: name, (6 from item condition_data: components, tasks, languages,
    //                check, after_install, before_install),
    //         attributes(4), winver(20), permission(2), options(1)
    private fun skipDirectoryEntry(s: InputStream) {
        repeat(7) { PascalString.skip(s) }
        s.skipNBytes(27)
    }
}
