package com.homm3.livewallpaper.core.assets

import com.homm3.livewallpaper.parser.lod.LodReader
import java.io.InputStream
import java.util.Locale

object LodValidator {
    // DEFs that must exist in H3sprite.lod
    private val h3spriteMarkers = setOf(
        "watrtl.def",
        "grastl.def",
        "edg.def",
        "avwpike.def",
        "avccast0.def"
    )

    // DEFs that must exist in HotA.lod
    private val hotaMarkers = setOf(
        "avccovx0.def",
        "avcfacx0.def",
        "avgflh00.def"
    )

    /**
     * Validates a LOD file by reading its header/index and checking for marker DEFs.
     * Returns null on success, or an error message on failure.
     */
    fun validate(stream: InputStream, isHota: Boolean): String? {
        val archive = try {
            LodReader(stream).read()
        } catch (e: IllegalArgumentException) {
            return e.message ?: "Invalid LOD file"
        } catch (e: Exception) {
            return "Failed to read file: ${e.message}"
        }

        val entryNames = archive.files.map { it.name.lowercase(Locale.ROOT) }.toSet()
        val markers = if (isHota) hotaMarkers else h3spriteMarkers
        val missing = markers.filter { it !in entryNames }

        if (missing.isNotEmpty()) {
            val expected = if (isHota) "HotA.lod" else "H3sprite.lod"
            return "Not a valid $expected — missing: ${missing.joinToString()}"
        }

        // Cross-check: reject if file matches the other type
        val wrongMarkers = if (isHota) h3spriteMarkers else hotaMarkers
        val wrongPresent = wrongMarkers.filter { it in entryNames }
        if (wrongPresent.size == wrongMarkers.size) {
            val actual = if (isHota) "H3sprite.lod" else "HotA.lod"
            val expected = if (isHota) "HotA.lod" else "H3sprite.lod"
            return "This looks like $actual, not $expected"
        }

        return null
    }
}
