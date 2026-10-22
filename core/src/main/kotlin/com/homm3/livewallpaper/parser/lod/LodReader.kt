package com.homm3.livewallpaper.parser.lod

import com.homm3.livewallpaper.parser.BinaryReader
import org.tukaani.xz.LZMAInputStream
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.InputStream
import java.util.zip.Inflater

class LodReader(private val source: LodDataSource) : Closeable {
    constructor(stream: InputStream) : this(ByteArrayLodDataSource(stream.readBytes()))

    private val magicHeader = byteArrayOf(
        0x4c.toByte(), 0x4f.toByte(), 0x44.toByte(), 0x00.toByte(),
        0xc8.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
    )

    fun read(): LodArchive {
        val header = BinaryReader(source.read(0, ENTRIES_OFFSET.toInt()).inputStream())
        val magic = header.readBytes(magicHeader.size)
        if (!magic.contentEquals(magicHeader)) {
            throw IllegalArgumentException("Wrong file selected: invalid LOD magic header")
        }
        val filesCount = header.readInt()
        val key = header.readInt()
        val isHashed = key != 0 && key != PLAIN_KEY_MAGIC
        require(filesCount >= 0) { "Invalid LOD entry count: $filesCount" }

        val entries = BinaryReader(source.read(ENTRIES_OFFSET, filesCount * ENTRY_SIZE).inputStream())
        val files = (0 until filesCount).map {
            if (isHashed) readHashedEntry(entries, key) else readEntry(entries)
        }
        return LodArchive(files, isHota18 = isHashed)
    }

    override fun close() = source.close()

    /**
     * Decompress only the first [limit] bytes of an entry.
     * Used for header identification without full decompression.
     */
    fun readFileContentPartial(entry: LodEntry, limit: Int): ByteArray {
        if (entry.compressedSize == 0) {
            return source.read(entry.offset.toLong(), minOf(entry.size, limit))
        }

        val raw = source.read(entry.offset.toLong(), entry.compressedSize)

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
        // Python dispatch: if csize != 0 → (2=LZMA, 3=zlib, else=raw) ; csize==0 → raw(size)
        if (entry.compressedSize == 0) {
            return source.read(entry.offset.toLong(), entry.size).inputStream()
        }

        val raw = source.read(entry.offset.toLong(), entry.compressedSize)

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

    private fun readEntry(reader: BinaryReader): LodEntry {
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
    private fun readHashedEntry(reader: BinaryReader, key: Int): LodEntry {
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
        private const val ENTRY_SIZE = 32
    }
}
