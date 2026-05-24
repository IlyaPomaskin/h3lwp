package com.homm3.livewallpaper.parser.inno

import java.io.ByteArrayInputStream
import java.io.File
import java.io.RandomAccessFile

object InnoSetupExtractor {
    private const val HOTA_LOD_DEST_SUFFIX = "data\\hota.lod"

    fun extractHotaLod(installer: File, output: File, onProgress: ((Long, Long) -> Unit)? = null) {
        val table = RandomAccessFile(installer, "r").use {
            SetupLdrOffsetTable.parse(PeResource.readRcData(it, 11111))
        }

        // Read primary block (setup-0): header + all non-data entries.
        // After readSetup0() returns, the raf's file pointer is positioned at the secondary block.
        val (setup0, dataEntriesBytes) = RandomAccessFile(installer, "r").use { raf ->
            val s0 = CompressedBlockReader.readSetup0(raf, table.offset0)
            // raf is now positioned at the secondary block (data entries block)
            val de = CompressedBlockReader.readDataEntriesBlock(raf)
            Pair(s0, de)
        }

        val stream = ByteArrayInputStream(setup0)
        val header = SetupHeader.parse(stream)
        SetupEntries.skipUpToFileEntries(stream, header)
        val target = FileEntries.findByDestinationSuffix(
            stream, header.fileCount, HOTA_LOD_DEST_SUFFIX, caseInsensitive = true
        ) ?: error("Installer does not contain a file ending in $HOTA_LOD_DEST_SUFFIX")

        // Data entries are in the secondary block, not in setup0
        val dataStream = ByteArrayInputStream(dataEntriesBytes)
        val locations = FileLocationEntries.readAll(dataStream, header.dataEntryCount)
        val loc = locations[target.locationIndex]

        output.outputStream().buffered().use { out ->
            ChunkDecompressor.extract(installer, table.offset1, loc, out, onProgress)
        }
    }
}
