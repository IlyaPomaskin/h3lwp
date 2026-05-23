package com.homm3.livewallpaper.parser.inno

import java.io.InputStream

/**
 * Parsed Inno Setup TSetupHeader for version 5.5.7 unicode.
 *
 * Layout (after the 64-byte setup-0 identity string):
 *   - 31 PascalString fields (UTF-16LE, u32 length-prefix)
 *   - compiled_code blob (u32 length + raw bytes)
 *   - 16 u32 entry counts (languageCount .. uninstallRunEntryCount)
 *   - 93 bytes of trailing fixed fields (winver, colors, wizard params,
 *     password hash/salt, disk space, slices, enum bytes, option flags)
 *   → cursor is positioned at the start of language entries (consumed by T7)
 *
 * Empirically verified against HotA 1.8.0 installer (Inno Setup 5.5.7u).
 */
data class SetupHeader(
    val appName: String,
    val languageCount: Int,
    val messageCount: Int,
    val permissionCount: Int,
    val typeCount: Int,
    val componentCount: Int,
    val taskCount: Int,
    val directoryCount: Int,
    val fileCount: Int,
    val dataEntryCount: Int,
    val iconCount: Int,
    val iniEntryCount: Int,
    val registryEntryCount: Int,
    val deleteEntryCount: Int,
    val uninstallDeleteEntryCount: Int,
    val runEntryCount: Int,
    val uninstallRunEntryCount: Int,
) {
    companion object {
        /** Number of PascalString fields in TSetupHeader for Inno Setup 5.5.7u. */
        private const val STRING_FIELD_COUNT = 31

        /**
         * Number of bytes in the trailing fixed-size section after the 16 entry counts.
         * Covers: winver (u64), colors, wizard params, password hash+salt, disk space,
         * slices per disk, enum bytes, and option flag bytes.
         * Calibrated empirically against HotA 1.8.0 (5.5.7u): language entries start
         * at position 7633 = 7540 + 93.
         */
        private const val TRAILING_FIXED_BYTES = 93

        fun parse(stream: InputStream): SetupHeader {
            // --- String fields ---
            // Field 0 is app_name; remaining 30 strings are skipped.
            val appName = PascalString.read(stream)
            for (i in 1 until STRING_FIELD_COUNT) {
                PascalString.skip(stream)
            }

            // --- compiled_code blob (u32 length + raw bytes) ---
            val ccLen = readU32(stream)
            if (ccLen > 0) {
                stream.skipNBytes(ccLen.toLong())
            }

            // --- 16 u32 entry counts ---
            val languageCount              = readU32(stream)
            val messageCount               = readU32(stream)
            val permissionCount            = readU32(stream)
            val typeCount                  = readU32(stream)
            val componentCount             = readU32(stream)
            val taskCount                  = readU32(stream)
            val directoryCount             = readU32(stream)
            val fileCount                  = readU32(stream)
            val dataEntryCount             = readU32(stream)
            val iconCount                  = readU32(stream)
            val iniEntryCount              = readU32(stream)
            val registryEntryCount         = readU32(stream)
            val deleteEntryCount           = readU32(stream)
            val uninstallDeleteEntryCount  = readU32(stream)
            val runEntryCount              = readU32(stream)
            val uninstallRunEntryCount     = readU32(stream)

            // --- Trailing fixed fields ---
            // winver (u64) + colors (2×u32) + wizard image/small image params +
            // password.sha1 (u32) + salt bytes + extra_disk_space (u64) +
            // slices_per_disk (u32) + various enum/flag bytes.
            stream.skipNBytes(TRAILING_FIXED_BYTES.toLong())

            // Cursor is now positioned at the first language entry.
            return SetupHeader(
                appName                    = appName,
                languageCount              = languageCount,
                messageCount               = messageCount,
                permissionCount            = permissionCount,
                typeCount                  = typeCount,
                componentCount             = componentCount,
                taskCount                  = taskCount,
                directoryCount             = directoryCount,
                fileCount                  = fileCount,
                dataEntryCount             = dataEntryCount,
                iconCount                  = iconCount,
                iniEntryCount              = iniEntryCount,
                registryEntryCount         = registryEntryCount,
                deleteEntryCount           = deleteEntryCount,
                uninstallDeleteEntryCount  = uninstallDeleteEntryCount,
                runEntryCount              = runEntryCount,
                uninstallRunEntryCount     = uninstallRunEntryCount,
            )
        }

        private fun readU32(stream: InputStream): Int {
            val b0 = stream.read(); val b1 = stream.read()
            val b2 = stream.read(); val b3 = stream.read()
            require(b3 != -1) { "Unexpected EOF reading u32" }
            return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        }
    }
}
