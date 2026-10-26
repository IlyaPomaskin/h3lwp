package com.homm3.livewallpaper.parser.lod

import com.homm3.livewallpaper.parser.BinaryReader
import org.tukaani.xz.LZMAInputStream
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.util.zip.Inflater

class LodReader(private val source: LodDataSource) : Closeable {
    private val magicHeader = byteArrayOf(
        0x4c.toByte(), 0x4f.toByte(), 0x44.toByte(), 0x00.toByte(),
        0xc8.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
    )

    fun read(): LodArchive {
        val header = BinaryReader(source.read(0, ENTRIES_OFFSET.toInt()))
        val magic = header.readBytes(magicHeader.size)
        if (!magic.contentEquals(magicHeader)) {
            throw IllegalArgumentException("Wrong file selected: invalid LOD magic header")
        }
        val filesCount = header.readInt()
        val key = header.readInt()
        val isHashed = key != 0 && key != PLAIN_KEY_MAGIC
        require(filesCount in 0..MAX_FILES_COUNT) { "Invalid LOD entry count: $filesCount" }

        val entries = BinaryReader(source.read(ENTRIES_OFFSET, filesCount * ENTRY_SIZE))
        val files = (0 until filesCount).map {
            if (isHashed) readHashedEntry(entries, key) else readEntry(entries)
        }
        return LodArchive(files, isHota18 = isHashed)
    }

    override fun close() = source.close()

    fun readFileContent(entry: LodEntry): ByteArrayInputStream {
        // Python dispatch: if csize != 0 → (2=LZMA, 3=zlib, else=raw) ; csize==0 → raw(size)
        if (entry.compressedSize == 0) {
            return source.read(entry.offset.toLong(), entry.size).inputStream()
        }
        require(entry.size in 0..MAX_ENTRY_SIZE) { "Invalid LOD entry size: ${entry.name} ${entry.size}" }

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
                        require(output.size() <= MAX_ENTRY_SIZE) {
                            "LOD entry unpacks to more than $MAX_ENTRY_SIZE bytes: ${entry.name}"
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    throw e
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
        private const val MAX_FILES_COUNT = Int.MAX_VALUE / ENTRY_SIZE

        /** The largest real entry (in HotA.lod) unpacks to about 7.5 MB. */
        private const val MAX_ENTRY_SIZE = 64 * 1024 * 1024
    }
}
