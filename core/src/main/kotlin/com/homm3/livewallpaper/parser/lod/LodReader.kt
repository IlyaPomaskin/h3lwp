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
        val key = reader.readBytes(4)

        val isHota18 = (key[0].toInt() and 0xFF) == 135

        if (isHota18) {
            // Skip to offset 80 (we've read 16 bytes so far)
            reader.skip(64)
            val files = (0 until filesCount).map { readHota18Entry(key) }
            return LodArchive(files, isHota18 = true)
        } else {
            // Standard mode: skip remaining 76 bytes to reach offset 92
            reader.skip(76)
            val files = (0 until filesCount).map { readEntry() }
            return LodArchive(files)
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

    private fun readHota18Entry(key: ByteArray): LodEntry {
        val nameBytes = reader.readBytes(16)
        val hexName = nameBytes.joinToString("") { "%02x".format(it) }
        val encrypted = reader.readBytes(16)

        // XOR decrypt with 4-byte key
        val decrypted = ByteArray(16)
        for (i in 0 until 16) {
            decrypted[i] = (encrypted[i].toInt() xor key[i % 4].toInt()).toByte()
        }

        val offset = readLittleEndianInt(decrypted, 0)
        val size = readLittleEndianInt(decrypted, 4)
        val compressedSize = readLittleEndianInt(decrypted, 8)
        // Compression method comes from the encrypted byte (NOT decrypted)
        val compressionMethod = encrypted[12].toInt() and 0xFF

        return LodEntry(
            name = hexName,
            offset = offset,
            size = size,
            fileType = null,
            compressedSize = compressedSize,
            compressionMethod = compressionMethod
        )
    }

    private fun readLittleEndianInt(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
    }
}
