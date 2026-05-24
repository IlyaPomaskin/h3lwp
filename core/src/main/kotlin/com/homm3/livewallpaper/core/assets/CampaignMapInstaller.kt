package com.homm3.livewallpaper.core.assets

import com.homm3.livewallpaper.parser.h3c.H3cExtractor
import com.homm3.livewallpaper.parser.lod.LodReader
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

object CampaignMapInstaller {
    private val log = Logger.getLogger(CampaignMapInstaller::class.java.name)

    data class Result(
        val campaignsFound: Int,
        val mapsWritten: Int,
        /** Entry names whose H3cExtractor.extract threw. */
        val skipped: List<String>,
    )

    /**
     * Reads every .h3c entry from [lod], runs each through H3cExtractor, and
     * writes the per-scenario .h3m files to [outputDir]. Existing target files
     * are left untouched (no overwrite). Throws nothing on per-campaign
     * failure: the offending entry name lands in [Result.skipped] and
     * extraction continues.
     */
    fun installFromLod(
        lod: File,
        outputDir: File,
        onProgress: ((current: Int, total: Int, message: String) -> Unit)? = null,
    ): Result {
        log.info("installFromLod: lod=${lod.absolutePath} size=${lod.length()} outputDir=$outputDir")
        outputDir.mkdirs()

        val archive = LodReader(lod.inputStream()).read()
        val h3cEntries = archive.files.filter { it.name.endsWith(".h3c", ignoreCase = true) }
        log.info("archive: ${archive.files.size} entries, ${h3cEntries.size} .h3c: ${h3cEntries.map { it.name }}")

        var mapsWritten = 0
        val skipped = ArrayList<String>()

        for ((index, entry) in h3cEntries.withIndex()) {
            log.info(
                "[${index + 1}/${h3cEntries.size}] entry='${entry.name}' " +
                    "offset=${entry.offset} compressedSize=${entry.compressedSize} size=${entry.size} " +
                    "method=${entry.compressionMethod}"
            )
            onProgress?.invoke(index + 1, h3cEntries.size, "Extracting ${entry.name}...")

            val bytes = try {
                val t0 = System.nanoTime()
                val data = LodReader(lod.inputStream()).readFileContent(entry).readBytes()
                val ms = (System.nanoTime() - t0) / 1_000_000
                log.info("  readFileContent('${entry.name}'): ${data.size} bytes in ${ms}ms")
                data
            } catch (e: Exception) {
                log.log(Level.WARNING, "  readFileContent failed for '${entry.name}'", e)
                skipped += entry.name
                continue
            }

            val extracted = try {
                val t0 = System.nanoTime()
                val maps = H3cExtractor.extract(bytes)
                val ms = (System.nanoTime() - t0) / 1_000_000
                log.info("  H3cExtractor.extract('${entry.name}'): ${maps.size} maps in ${ms}ms; names=${maps.map { it.name }}")
                maps
            } catch (e: Exception) {
                log.log(Level.WARNING, "  H3cExtractor.extract failed for '${entry.name}'", e)
                skipped += entry.name
                continue
            }

            val campaignBase = if (entry.name.endsWith(".h3c", ignoreCase = true)) {
                entry.name.dropLast(4)
            } else entry.name

            var writtenThisCampaign = 0
            var skippedExisting = 0
            for ((mapIndex, map) in extracted.withIndex()) {
                val targetName = "$campaignBase-map-$mapIndex.h3m"
                val target = outputDir.resolve(targetName)
                if (target.exists()) {
                    skippedExisting++
                    continue
                }
                target.outputStream().use { it.write(map.bytes) }
                mapsWritten++
                writtenThisCampaign++
            }
            log.info(
                "  wrote $writtenThisCampaign maps as '$campaignBase-map-*.h3m' " +
                    "(skipped $skippedExisting existing) from '${entry.name}'"
            )
        }

        log.info(
            "installFromLod done: campaignsFound=${h3cEntries.size} mapsWritten=$mapsWritten " +
                "skipped=$skipped"
        )
        return Result(
            campaignsFound = h3cEntries.size,
            mapsWritten = mapsWritten,
            skipped = skipped,
        )
    }
}
