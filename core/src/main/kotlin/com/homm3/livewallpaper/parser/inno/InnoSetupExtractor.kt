package com.homm3.livewallpaper.parser.inno

import java.io.ByteArrayInputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.logging.Level
import java.util.logging.Logger

object InnoSetupExtractor {
    private val log = Logger.getLogger(InnoSetupExtractor::class.java.name)
    private const val HOTA_LOD_DEST_SUFFIX = "data\\hota.lod"

    /** Caller-supplied target. [matcher] runs against an entry's `destination`
     *  (backslashes preserved as authored in Inno Setup). [outputFor] resolves
     *  each matched destination to an output File. */
    data class Target(
        val matcher: (destination: String) -> Boolean,
        val outputFor: (destination: String) -> File,
    )

    data class ExtractedFile(val destination: String, val output: File)

    /** Extracts every file entry matched by any [Target] in a single installer pass.
     *  Entries matched by multiple targets are written once per target. */
    fun extract(
        installer: File,
        targets: List<Target>,
        onProgress: ((written: Long, total: Long, currentDestination: String) -> Unit)? = null,
    ): List<ExtractedFile> {
        log.info("extract: installer=${installer.absolutePath} size=${installer.length()} targets=${targets.size}")

        val table = RandomAccessFile(installer, "r").use {
            SetupLdrOffsetTable.parse(PeResource.readRcData(it, 11111))
        }
        log.info("offset table: offset0=${table.offset0} offset1=${table.offset1}")

        val (setup0, dataEntriesBytes) = RandomAccessFile(installer, "r").use { raf ->
            val s0 = CompressedBlockReader.readSetup0(raf, table.offset0)
            val de = CompressedBlockReader.readDataEntriesBlock(raf)
            Pair(s0, de)
        }
        log.info("setup0=${setup0.size} bytes, dataEntries=${dataEntriesBytes.size} bytes")

        val s0Stream = ByteArrayInputStream(setup0)
        val header = SetupHeader.parse(s0Stream)
        SetupEntries.skipUpToFileEntries(s0Stream, header)
        log.info("header: fileCount=${header.fileCount} dataEntryCount=${header.dataEntryCount}")

        // A single entry may match multiple targets — for each entry we keep one
        // (entry, target) pair per match so the same source bytes get fanned out
        // to each target's output file.
        data class Pending(val entry: FileEntry, val target: Target)
        val matched = FileEntries.collectMatching(s0Stream, header.fileCount) { destination ->
            targets.any { it.matcher(destination) }
        }
        val pending: List<Pending> = matched.flatMap { entry ->
            targets.filter { it.matcher(entry.destination) }.map { Pending(entry, it) }
        }
        log.info("matched=${matched.size} entries, pending=${pending.size} (entry,target) pairs")

        val dataStream = ByteArrayInputStream(dataEntriesBytes)
        val locations = FileLocationEntries.readAll(dataStream, header.dataEntryCount)

        val results = ArrayList<ExtractedFile>(pending.size)
        val totalBytes = pending.sumOf { locations[it.entry.locationIndex].fileSize }
        var bytesSoFar = 0L

        for ((i, p) in pending.withIndex()) {
            val loc = locations[p.entry.locationIndex]
            val output = p.target.outputFor(p.entry.destination)
            log.info("[${i + 1}/${pending.size}] dest='${p.entry.destination}' locIdx=${p.entry.locationIndex} → $output")
            output.parentFile?.mkdirs()
            try {
                output.outputStream().buffered().use { out ->
                    ChunkDecompressor.extract(installer, table.offset1, loc, out) { written, _ ->
                        onProgress?.invoke(bytesSoFar + written, totalBytes, p.entry.destination)
                    }
                }
            } catch (e: Exception) {
                log.log(
                    Level.SEVERE,
                    "extraction failed for dest='${p.entry.destination}' locIdx=${p.entry.locationIndex}",
                    e,
                )
                throw e
            }
            bytesSoFar += loc.fileSize
            results += ExtractedFile(p.entry.destination, output)
        }
        log.info("extract: done, wrote ${results.size} files, $bytesSoFar bytes total")
        return results
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
