package com.homm3.livewallpaper.core.assets

import com.homm3.livewallpaper.parser.h3c.H3cExtractor
import com.homm3.livewallpaper.parser.lod.LodReader
import java.io.File

object CampaignMapInstaller {

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
        outputDir.mkdirs()

        val archive = LodReader(lod.inputStream()).read()
        val h3cEntries = archive.files.filter { it.name.endsWith(".h3c", ignoreCase = true) }

        var mapsWritten = 0
        val skipped = ArrayList<String>()

        for ((index, entry) in h3cEntries.withIndex()) {
            onProgress?.invoke(index + 1, h3cEntries.size, "Extracting ${entry.name}...")

            val bytes = try {
                LodReader(lod.inputStream()).readFileContent(entry).readBytes()
            } catch (e: Exception) {
                skipped += entry.name
                continue
            }

            val extracted = try {
                H3cExtractor.extract(bytes)
            } catch (e: Exception) {
                skipped += entry.name
                continue
            }

            for (map in extracted) {
                val target = outputDir.resolve(map.name)
                if (target.exists()) continue
                target.outputStream().use { it.write(map.bytes) }
                mapsWritten++
            }
        }

        return Result(
            campaignsFound = h3cEntries.size,
            mapsWritten = mapsWritten,
            skipped = skipped,
        )
    }
}
