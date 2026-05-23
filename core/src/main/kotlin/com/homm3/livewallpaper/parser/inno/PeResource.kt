package com.homm3.livewallpaper.parser.inno

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object PeResource {
    private const val RT_RCDATA = 10

    fun readRcData(raf: RandomAccessFile, resourceId: Int): ByteArray {
        // DOS header: e_lfanew at offset 0x3C
        raf.seek(0x3CL)
        val peOffset = readU32(raf).toLong()

        // PE signature
        raf.seek(peOffset)
        val sig = ByteArray(4); raf.readFully(sig)
        require(sig.contentEquals(byteArrayOf(0x50, 0x45, 0, 0))) { "Bad PE signature" }

        // COFF header
        raf.skipBytes(2) // Machine
        val numSections = readU16(raf)
        raf.skipBytes(12) // TimeDateStamp + PointerToSymTab + NumSyms
        val optHeaderSize = readU16(raf)
        raf.skipBytes(2) // Characteristics

        // Optional header — first u16 = magic (0x10b = PE32, 0x20b = PE32+)
        val optHeaderStart = raf.filePointer
        val optMagic = readU16(raf)
        val isPE32Plus = (optMagic == 0x20b)
        // Skip to DataDirectories. Resource is index 2 of DataDirectory array.
        // PE32: data directories start at offset 96 from optHeaderStart
        // PE32+: data directories start at offset 112
        val dataDirOffset = optHeaderStart + (if (isPE32Plus) 112 else 96)
        raf.seek(dataDirOffset + 2 * 8) // skip Export (idx 0), Import (idx 1), reach Resource (idx 2)
        val rsrcRva = readU32(raf).toLong()
        val rsrcSize = readU32(raf).toLong()
        require(rsrcRva != 0L) { "PE has no resource directory" }

        // Section table starts after optional header
        val sectionTableStart = optHeaderStart + optHeaderSize
        raf.seek(sectionTableStart)
        var rsrcVa = 0L; var rsrcRaw = 0L
        for (i in 0 until numSections) {
            raf.skipBytes(8) // Name
            val virtSize = readU32(raf).toLong()
            val va = readU32(raf).toLong()
            val rawSize = readU32(raf).toLong()
            val raw = readU32(raf).toLong()
            raf.skipBytes(16) // ptr to relocs/linenums + nums + characteristics
            // Use section's own virtual size (fall back to raw size) to determine RVA membership
            val secSize = if (virtSize != 0L) virtSize else rawSize
            if (rsrcRva in va until (va + secSize.coerceAtLeast(1))) {
                rsrcVa = va; rsrcRaw = raw; break
            }
        }
        require(rsrcRaw != 0L) { "Resource section not found" }
        val rvaToFile = { rva: Long -> rsrcRaw + (rva - rsrcVa) }
        val rsrcBase = rsrcRaw

        // Resource directory: walk Type (RT_RCDATA = 10) → Name (resourceId) → Language (any, take first)
        val typeEntry = findResourceEntry(raf, rsrcBase, RT_RCDATA) ?: error("RT_RCDATA not found")
        require(typeEntry.isSubdir) { "RT_RCDATA entry is not a subdirectory" }
        val nameEntry = findResourceEntry(raf, rsrcBase + typeEntry.offset, resourceId)
            ?: error("Resource id=$resourceId not found")
        require(nameEntry.isSubdir) { "Resource id entry is not a subdirectory" }
        // Take the first language entry
        raf.seek(rsrcBase + nameEntry.offset + 12) // skip Characteristics+TimeDateStamp+Major+MinorVer
        val numNamed = readU16(raf); val numIds = readU16(raf)
        require(numNamed + numIds > 0) { "Resource id=$resourceId has no language entries" }
        // Each entry: name/id (u32) + offset_to_data (u32)
        raf.skipBytes(4) // skip name/id
        val dataEntryOffset = readU32(raf).toLong() and 0x7FFFFFFFL

        // DataEntry: RVA + Size + Codepage + Reserved
        raf.seek(rsrcBase + dataEntryOffset)
        val dataRva = readU32(raf).toLong()
        val dataSize = readU32(raf).toLong()

        raf.seek(rvaToFile(dataRva))
        val out = ByteArray(dataSize.toInt())
        raf.readFully(out)
        return out
    }

    private data class ResourceEntry(val id: Int, val isSubdir: Boolean, val offset: Long)

    /** Walks one directory level looking for an entry with the given id (named entries are skipped). */
    private fun findResourceEntry(raf: RandomAccessFile, dirOffset: Long, wantedId: Int): ResourceEntry? {
        raf.seek(dirOffset + 12)
        val numNamed = readU16(raf); val numIds = readU16(raf)
        // Skip named entries
        raf.skipBytes(numNamed * 8)
        for (i in 0 until numIds) {
            val id = readU32(raf)
            val rawOffset = readU32(raf).toLong()
            if (id == wantedId) {
                val isSubdir = (rawOffset and 0x80000000.toInt().toLong()) != 0L
                return ResourceEntry(id, isSubdir, rawOffset and 0x7FFFFFFFL)
            }
        }
        return null
    }

    private fun readU16(raf: RandomAccessFile): Int {
        val b = ByteArray(2); raf.readFully(b)
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
    }

    private fun readU32(raf: RandomAccessFile): Int {
        val b = ByteArray(4); raf.readFully(b)
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int
    }
}
