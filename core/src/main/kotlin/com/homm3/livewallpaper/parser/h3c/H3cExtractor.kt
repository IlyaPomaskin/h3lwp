package com.homm3.livewallpaper.parser.h3c

object H3cExtractor {

    data class ExtractedMap(
        /** Sanitized filename including `.h3m` suffix. */
        val name: String,
        /** Gzipped .h3m bytes — write to disk verbatim. */
        val bytes: ByteArray,
    )

    /**
     * Splits the input into N+1 gzip streams. Stream 0 yields scenario names
     * (falling back to numeric names on parse failure). Streams 1..N are
     * returned verbatim as gzipped .h3m bytes — no recompression.
     *
     * @throws IllegalArgumentException if [input] is not a gzip-stream sequence.
     */
    fun extract(input: ByteArray): List<ExtractedMap> {
        val streams = GzipStreamSplitter.split(input)
        require(streams.size >= 2) { "Not an h3c file: need >=2 gzip streams, got ${streams.size}" }

        val mapStreams = streams.drop(1)
        val names = parseNamesOrFallback(streams[0].decompressed, mapStreams.size)

        val used = HashSet<String>()
        return mapStreams.mapIndexed { i, s ->
            val base = names.getOrNull(i)?.let(::sanitizeMapName) ?: "map_$i.h3m"
            val unique = uniqueName(base, used, i)
            used += unique
            val gzBytes = input.copyOfRange(s.compressedStart, s.compressedEndExclusive)
            ExtractedMap(unique, gzBytes)
        }
    }

    private fun parseNamesOrFallback(stream0: ByteArray, count: Int): List<String> {
        return try {
            CampaignHeaderReader.readScenarioNames(stream0)
        } catch (_: Exception) {
            (0 until count).map { "map_$it" }
        }
    }

    private fun uniqueName(base: String, used: Set<String>, index: Int): String {
        if (base !in used) return base
        val (stem, ext) = splitExt(base)
        return "${stem}_$index$ext"
    }

    private fun splitExt(name: String): Pair<String, String> {
        val dot = name.lastIndexOf('.')
        if (dot <= 0) return name to ""
        return name.substring(0, dot) to name.substring(dot)
    }

    /**
     * Conservative filename sanitizer: replace filesystem-illegal characters
     * (`:`, `/`, `\`, `?`, `*`, `<`, `>`, `|`, `"`, controls) with `_`.
     * Appends `.h3m` if absent. Trims whitespace. Yields `"unnamed.h3m"` if
     * the cleaned name is empty.
     */
    private fun sanitizeMapName(raw: String): String {
        val cleaned = buildString(raw.length) {
            for (c in raw.trim()) {
                if (c.code < 0x20 || c in ILLEGAL_CHARS) append('_') else append(c)
            }
        }
        val nonEmpty = if (cleaned.isBlank()) "unnamed" else cleaned
        return if (nonEmpty.endsWith(".h3m", ignoreCase = true)) nonEmpty else "$nonEmpty.h3m"
    }

    private val ILLEGAL_CHARS = setOf(':', '/', '\\', '?', '*', '<', '>', '|', '"')
}
