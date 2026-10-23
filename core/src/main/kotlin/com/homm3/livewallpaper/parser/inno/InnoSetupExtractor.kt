package com.homm3.livewallpaper.parser.inno

import java.io.ByteArrayInputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.logging.Level
import java.util.logging.Logger

object InnoSetupExtractor {
    private val log = Logger.getLogger(InnoSetupExtractor::class.java.name)
    private const val HOTA_LOD_DEST_SUFFIX = "data\\hota.lod"
    private const val EXTERNAL_DATA_OFFSET = 0L
    private val SLICE_MAGIC = "idska32\u001a".toByteArray(Charsets.US_ASCII)

    /** Caller-supplied target. [matcher] runs against an entry's `destination`
     *  (backslashes preserved as authored in Inno Setup). [outputFor] resolves
     *  each matched destination to an output File. */
    data class Target(
        val matcher: (destination: String) -> Boolean,
        val outputFor: (destination: String) -> File,
    )

    data class ExtractedFile(val destination: String, val output: File)

    /** Extracts every file entry matched by any [Target] in a single installer pass.
     *  Entries matched by multiple targets are written once per target.
     *  Installers that keep their data in external `-N.bin` files (e.g. GOG) need those files
     *  as [dataSlices], ordered by slice number; the caller owns and closes the channels. */
    fun extract(
        installer: File,
        targets: List<Target>,
        dataSlices: List<FileChannel> = emptyList(),
        onProgress: ((written: Long, total: Long, currentDestination: String) -> Unit)? = null,
    ): List<ExtractedFile> {
        log.info("extract: installer=${installer.absolutePath} size=${installer.length()} targets=${targets.size}")

        val table = RandomAccessFile(installer, "r").use {
            SetupLdrOffsetTable.parse(PeResource.readRcData(it, 11111))
        }
        log.info("offset table: offset0=${table.offset0} offset1=${table.offset1}")

        val (version, setup0, dataEntriesBytes) = RandomAccessFile(installer, "r").use { raf ->
            val v = CompressedBlockReader.readVersion(raf, table.offset0)
            val s0 = CompressedBlockReader.readSetup0(raf, table.offset0)
            val de = CompressedBlockReader.readDataEntriesBlock(raf)
            Triple(v, s0, de)
        }
        log.info("version=$version setup0=${setup0.size} bytes, dataEntries=${dataEntriesBytes.size} bytes")

        val s0Stream = ByteArrayInputStream(setup0)
        val header = SetupHeader.parse(s0Stream, version)
        SetupEntries.skipUpToFileEntries(s0Stream, header)
        log.info(
            "header: fileCount=${header.fileCount} dataEntryCount=${header.dataEntryCount} " +
                "compression=${header.compression} slicesPerDisk=${header.slicesPerDisk}"
        )
        val isDataExternal = table.offset1 == EXTERNAL_DATA_OFFSET
        if (isDataExternal) {
            require(dataSlices.isNotEmpty()) { "Installer data is stored in separate .bin files; none were provided" }
            require(header.slicesPerDisk == 1) { "Multiple slices per disk are not supported" }
            dataSlices.forEach(::verifySliceHeader)
        }

        // A single file may match multiple targets — for each file part we keep one
        // (part, target) pair per match so the same source bytes get fanned out
        // to each target's output file. Parts of one file are kept in order.
        data class Pending(val destination: String, val part: FilePart, val partNumber: Int, val target: Target)
        val entries = FileEntries.readAll(s0Stream, header.fileCount)
        val files = GogGalaxyFiles.resolve(entries)
            ?: entries.map { LogicalFile(it.destination, listOf(FilePart(it.locationIndex, isZlibCompressed = false))) }
        val matched = files.filter { file -> targets.any { it.matcher(file.destination) } }
        val pending: List<Pending> = matched.flatMap { file ->
            targets.filter { it.matcher(file.destination) }.flatMap { target ->
                file.parts.mapIndexed { partNumber, part -> Pending(file.destination, part, partNumber, target) }
            }
        }
        log.info("matched=${matched.size} files, pending=${pending.size} (part,target) pairs")

        val dataStream = ByteArrayInputStream(dataEntriesBytes)
        val locations = FileLocationEntries.readAll(dataStream, header.dataEntryCount)

        val totalBytes = pending.sumOf { locations[it.part.locationIndex].fileSize }

        // Group by chunk so one LZMA2 stream produces all files within that chunk in a single
        // forward pass. Inno Setup's solid compression puts many files in one chunk; opening a
        // fresh LZMA2 stream per file would re-decompress the whole prefix each time.
        val byChunk: Map<Pair<Int, Long>, List<Pending>> = pending.groupBy {
            val location = locations[it.part.locationIndex]
            location.firstSlice to location.chunkOffset
        }
        log.info("grouped ${pending.size} pending into ${byChunk.size} chunk(s)")

        val results = ArrayList<ExtractedFile>(pending.size)
        var bytesSoFar = 0L
        RandomAccessFile(installer, "r").use { installerFile ->
            for ((chunkOffset, group) in byChunk) {
                val chunkLoc = locations[group.first().part.locationIndex]
                require(chunkLoc.firstSlice == chunkLoc.lastSlice) { "Chunks spanning several slices are not supported" }
                val source = if (isDataExternal) sliceChannel(dataSlices, chunkLoc.firstSlice) else installerFile.channel
                val chunkStart = if (isDataExternal) chunkLoc.chunkOffset else table.offset1 + chunkLoc.chunkOffset
                val slices = group.map { p ->
                    val loc = locations[p.part.locationIndex]
                    val output = p.target.outputFor(p.destination)
                    ChunkDecompressor.Slice(
                        fileOffset = loc.fileOffset,
                        fileSize = loc.fileSize,
                        output = output,
                        tag = p.destination,
                        append = p.partNumber > 0,
                        inflate = p.part.isZlibCompressed,
                    )
                }
                log.info("chunk @ $chunkOffset: ${slices.size} slice(s), total ${slices.sumOf { it.fileSize }} bytes")
                try {
                    ChunkDecompressor.extractMultiple(
                        source = source,
                        chunkStart = chunkStart,
                        compression = header.compression,
                        chunkLocation = chunkLoc,
                        slices = slices,
                    ) { written, _, currentTag ->
                        onProgress?.invoke(bytesSoFar + written, totalBytes, currentTag)
                    }
                } catch (e: Exception) {
                    log.log(Level.SEVERE, "chunk extraction failed: chunkOffset=$chunkOffset slices=${slices.size}", e)
                    throw e
                }
                bytesSoFar += slices.sumOf { it.fileSize }
                for ((p, slice) in group.zip(slices)) {
                    if (p.partNumber == 0) results += ExtractedFile(p.destination, slice.output)
                }
            }
        }
        log.info("extract: done, wrote ${results.size} files, $bytesSoFar bytes total")
        return results
    }

    private fun sliceChannel(dataSlices: List<FileChannel>, sliceIndex: Int): FileChannel {
        require(sliceIndex in dataSlices.indices) {
            "Data slice ${sliceIndex + 1} is missing; provided ${dataSlices.size} .bin file(s)"
        }
        return dataSlices[sliceIndex]
    }

    private fun verifySliceHeader(slice: FileChannel) {
        val header = ByteBuffer.allocate(SLICE_MAGIC.size)
        slice.position(0)
        while (header.hasRemaining()) {
            require(slice.read(header) > 0) { "Data slice is too small" }
        }
        require(header.array().contentEquals(SLICE_MAGIC)) { "Not an Inno Setup data slice (.bin)" }
    }

    /** Back-compat wrapper. Extracts just `data\HotA.lod` into [output]. */
    fun extractHotaLod(installer: File, output: File, onProgress: ((Long, Long) -> Unit)? = null) {
        val targets = listOf(
            Target(
                matcher = { it.endsWith(HOTA_LOD_DEST_SUFFIX, ignoreCase = true) },
                outputFor = { _ -> output },
            )
        )
        extract(installer, targets, onProgress = onProgress?.let { cb ->
            { written, total, _ -> cb(written, total) }
        })
    }
}
