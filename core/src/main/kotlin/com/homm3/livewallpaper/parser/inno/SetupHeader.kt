package com.homm3.livewallpaper.parser.inno

import com.homm3.livewallpaper.parser.skipExactly
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parsed Inno Setup TSetupHeader for setup data 5.5.7 and 5.6.2 unicode.
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
    val slicesPerDisk: Int,
    val compression: ChunkCompression,
) {
    companion object {
        /**
         * Number of bytes in the trailing fixed-size section after the 16 entry counts.
         * Covers: winver (u64), colors, wizard params, password hash+salt, disk space,
         * slices per disk, enum bytes, and option flag bytes.
         * Calibrated empirically against HotA 1.8.0 (5.5.7u): language entries start
         * at position 7633 = 7540 + 93.
         */
        private const val TRAILING_FIXED_BYTES = 93

        // Offsets inside the trailing block, per innoextract's header::load for 5.5.7u–5.6.2u:
        // winver(20) colors(8) alpha(1) sha1(20) salt(8) extra_disk_space(8) → slices_per_disk,
        // then log/dir/privileges/language enums(5) → compression method.
        private const val SLICES_PER_DISK_OFFSET = 65
        private const val COMPRESSION_OFFSET = 74

        fun parse(stream: InputStream, version: SetupDataVersion = SetupDataVersion.V5_5_7): SetupHeader {
            // --- String fields ---
            // Field 0 is app_name; the remaining strings are skipped.
            val appName = PascalString.read(stream)
            for (i in 1 until version.headerStringCount) {
                PascalString.skip(stream)
            }

            // --- compiled_code blob (u32 length + raw bytes) ---
            val ccLen = readU32(stream)
            if (ccLen > 0) {
                stream.skipExactly(ccLen.toLong())
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
            val trailing = ByteArray(TRAILING_FIXED_BYTES)
            readFully(stream, trailing)
            val trailingBuffer = ByteBuffer.wrap(trailing).order(ByteOrder.LITTLE_ENDIAN)
            val slicesPerDisk = trailingBuffer.getInt(SLICES_PER_DISK_OFFSET)
            val compression = ChunkCompression.fromStored(trailing[COMPRESSION_OFFSET].toInt())

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
                slicesPerDisk              = slicesPerDisk,
                compression                = compression,
            )
        }

        private fun readFully(stream: InputStream, target: ByteArray) {
            var offset = 0
            while (offset < target.size) {
                val read = stream.read(target, offset, target.size - offset)
                require(read > 0) { "Unexpected EOF reading setup header" }
                offset += read
            }
        }

        private fun readU32(stream: InputStream): Int {
            val b0 = stream.read(); val b1 = stream.read()
            val b2 = stream.read(); val b3 = stream.read()
            require(b3 != -1) { "Unexpected EOF reading u32" }
            return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        }
    }
}

/** Stored compression method for 5.3.9+ (innoextract's stored_compression_method_3). */
enum class ChunkCompression {
    STORED, ZLIB, BZIP2, LZMA1, LZMA2;

    companion object {
        fun fromStored(value: Int): ChunkCompression {
            require(value in entries.indices) { "Unknown compression method: $value" }
            return entries[value]
        }
    }
}
