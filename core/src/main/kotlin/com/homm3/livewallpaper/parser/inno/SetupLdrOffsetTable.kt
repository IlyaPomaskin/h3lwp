package com.homm3.livewallpaper.parser.inno

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class SetupLdrOffsetTable(
    val totalSize: Long,
    val offsetExe: Long,
    val uncompressedSizeExe: Long,
    val crcExe: Int,
    val offset0: Long,
    val offset1: Long,
    val tableCrc: Int,
) {
    companion object {
        fun parse(bytes: ByteArray): SetupLdrOffsetTable {
            require(bytes.size == 44) { "Expected 44-byte offset table, got ${bytes.size}" }
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            buf.position(12) // skip 12-byte ID
            // 8 × u32 fields following the 12-byte ID (Inno Setup 5.5.7u / 5.6.0u layout).
            // field[0] = version — skipped, not stored
            // field[1] = totalSize
            // field[2] = offsetExe
            // field[3] = uncompressedSizeExe
            // field[4] = crcExe
            // field[5] = offset0 (setup-0 / header stream offset — large value, near end of file)
            // field[6] = offset1 (setup-1 / data stream offset — small value, right after loader stub)
            // field[7] = tableCrc
            buf.int // skip version
            val totalSize = buf.int.toLong() and 0xFFFFFFFFL
            val offsetExe = buf.int.toLong() and 0xFFFFFFFFL
            val uncompressedSizeExe = buf.int.toLong() and 0xFFFFFFFFL
            val crcExe = buf.int
            val offset0 = buf.int.toLong() and 0xFFFFFFFFL  // setup-0 header stream
            val offset1 = buf.int.toLong() and 0xFFFFFFFFL  // setup-1 data stream
            val tableCrc = buf.int
            return SetupLdrOffsetTable(totalSize, offsetExe, uncompressedSizeExe, crcExe, offset0, offset1, tableCrc)
        }
    }
}
