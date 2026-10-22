package com.homm3.livewallpaper.parser.lod

import com.homm3.livewallpaper.parser.BinaryReader
import org.tukaani.xz.LZMAInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.Inflater

class LodReader(stream: InputStream) {
    private val reader: BinaryReader

    init {
        // Buffer fully to enable absolute positioning via mark/reset.
        // Matches Python's f.seek(offset) pattern.
        val data = stream.readBytes()
        val bis = ByteArrayInputStream(data)
        bis.mark(data.size)
        reader = BinaryReader(bis)
    }

    private val magicHeader = byteArrayOf(
        0x4c.toByte(), 0x4f.toByte(), 0x44.toByte(), 0x00.toByte(),
        0xc8.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
    )

    fun read(): LodArchive {
        val magic = reader.readBytes(8)
        if (!magic.contentEquals(magicHeader)) {
            throw IllegalArgumentException("Wrong file selected: invalid LOD magic header")
        }
        val filesCount = reader.readInt()
        val key = reader.readInt()
        val isHashed = key != 0 && key != PLAIN_KEY_MAGIC

        reader.skip(ENTRIES_OFFSET - reader.position)
        val files = (0 until filesCount).map {
            if (isHashed) readHashedEntry(key) else readEntry()
        }
        return LodArchive(files, isHota18 = isHashed)
    }

    /**
     * Decompress only the first [limit] bytes of an entry.
     * Used for header identification without full decompression.
     */
    fun readFileContentPartial(entry: LodEntry, limit: Int): ByteArray {
        reader.seek(entry.offset.toLong())

        if (entry.compressedSize == 0) {
            val toRead = minOf(entry.size, limit)
            return reader.readBytes(toRead)
        }

        val raw = reader.readBytes(entry.compressedSize)

        return when (entry.compressionMethod) {
            2 -> {
                val lzmaStream = ByteArrayInputStream(raw, 1, raw.size - 1)
                val lzma = LZMAInputStream(lzmaStream, -1, 0x5D.toByte(), 262144)
                val output = ByteArray(limit)
                var pos = 0
                try {
                    while (pos < limit) {
                        val n = lzma.read(output, pos, limit - pos)
                        if (n <= 0) break
                        pos += n
                    }
                } catch (_: Exception) {}
                try { lzma.close() } catch (_: Exception) {}
                if (pos < limit) output.copyOf(pos) else output
            }
            3 -> {
                val output = ByteArray(limit)
                val inflater = Inflater()
                inflater.setInput(raw)
                val n = inflater.inflate(output)
                inflater.end()
                if (n < limit) output.copyOf(n) else output
            }
            else -> {
                val toRead = minOf(raw.size, limit)
                raw.copyOf(toRead)
            }
        }
    }

    fun readFileContent(entry: LodEntry): ByteArrayInputStream {
        // Absolute seek — matches Python's f.seek(offset)
        reader.seek(entry.offset.toLong())

        // Python dispatch: if csize != 0 → (2=LZMA, 3=zlib, else=raw) ; csize==0 → raw(size)
        if (entry.compressedSize == 0) {
            return reader.readBytes(entry.size).inputStream()
        }

        val raw = reader.readBytes(entry.compressedSize)

        return when (entry.compressionMethod) {
            2 -> {
                // LZMA: skip first byte, raw LZMA1 stream with hardcoded props.
                // Use -1 for uncompressed size (unknown) — matches Python's
                // decompress(data[1:]) which doesn't specify output size.
                val lzmaStream = ByteArrayInputStream(raw, 1, raw.size - 1)
                val lzma = LZMAInputStream(lzmaStream, -1, 0x5D.toByte(), 262144)
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                try {
                    while (true) {
                        val n = lzma.read(buffer)
                        if (n <= 0) break
                        output.write(buffer, 0, n)
                    }
                } catch (_: Exception) {
                    // Stream may end without proper LZMA termination
                }
                try { lzma.close() } catch (_: Exception) {}
                output.toByteArray().inputStream()
            }
            3 -> {
                // zlib
                val unpackedData = ByteArray(entry.size)
                val inflater = Inflater()
                inflater.setInput(raw)
                inflater.inflate(unpackedData)
                inflater.end()
                unpackedData.inputStream()
            }
            else -> {
                // Unknown compression or stored with csize != 0 — return raw bytes
                raw.inputStream()
            }
        }
    }

    private fun readEntry(): LodEntry {
        return LodEntry(
            name = reader.readString(16),
            offset = reader.readInt(),
            size = reader.readInt(),
            fileType = LodFileType.fromInt(reader.readInt()),
            compressedSize = reader.readInt(),
            compressionMethod = 3
        )
    }

    // Layout per h3_re/lodread.py: u32 hash; u32 offset^key; u32 size^key; u32 csize^key; u8 codec; u8 junk[15]
    private fun readHashedEntry(key: Int): LodEntry {
        val nameHash = reader.readInt()
        val offset = reader.readInt() xor key
        val size = reader.readInt() xor key
        val compressedSize = reader.readInt() xor key
        val compressionMethod = reader.readByte()
        reader.skip(HASHED_ENTRY_JUNK_SIZE)

        return LodEntry(
            name = hashedEntryName(nameHash),
            offset = offset,
            size = size,
            fileType = null,
            compressedSize = compressedSize,
            compressionMethod = compressionMethod
        )
    }

    companion object {
        private const val ENTRIES_OFFSET = 0x5CL
        private const val PLAIN_KEY_MAGIC = 0x7E0213
        private const val HASHED_ENTRY_JUNK_SIZE = 15
    }
}
