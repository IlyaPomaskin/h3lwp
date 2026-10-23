package com.homm3.livewallpaper.core.assets

/**
 * One scanned version of a map file. Random objects resolve to different DEFs with and without HotA assets,
 * so [hasHotaAssets] is part of it: the same file is scanned again once HotA availability changes.
 */
data class MapFileStamp(val fileName: String, val length: Long, val lastModified: Long, val hasHotaAssets: Boolean) {
    fun serialize(): String = listOf(fileName, length, lastModified, hasHotaAssets).joinToString(FIELD_SEPARATOR)

    companion object {
        private const val FIELD_SEPARATOR = "\t"
        private const val FIELD_COUNT = 4

        /** Reads a [serialize]d line; `null` when it isn't one. */
        fun parse(line: String): MapFileStamp? {
            val fields = line.split(FIELD_SEPARATOR)
            if (fields.size != FIELD_COUNT) return null
            val (fileName, length, lastModified, hasHotaAssets) = fields
            return MapFileStamp(
                fileName = fileName,
                length = length.toLongOrNull() ?: return null,
                lastModified = lastModified.toLongOrNull() ?: return null,
                hasHotaAssets = hasHotaAssets.toBooleanStrictOrNull() ?: return null,
            )
        }
    }
}

/**
 * Every DEF (lowercase name) any scanned map has used, and the map versions already scanned. Both only grow:
 * DEFs of removed maps stay listed.
 */
data class MapDefList(val defNames: Set<String>, val scannedMaps: Set<MapFileStamp>) {
    /** What scanning the not yet scanned [stamps] with [collectDefs] adds; `null` from it (unreadable map) adds nothing. */
    suspend fun additionsFor(
        stamps: List<MapFileStamp>,
        collectDefs: suspend (MapFileStamp) -> Set<String>?,
    ): MapDefList {
        val newStamps = stamps.filterNot { it in scannedMaps }
        val scanned = newStamps.mapNotNull { stamp -> collectDefs(stamp)?.let { stamp to it } }
        val newDefNames = scanned.flatMap { (_, defNames) -> defNames }.toSet() - defNames
        return MapDefList(newDefNames, scanned.map { (stamp, _) -> stamp }.toSet())
    }

    operator fun plus(additions: MapDefList) =
        MapDefList(defNames + additions.defNames, scannedMaps + additions.scannedMaps)

    val isEmpty: Boolean get() = defNames.isEmpty() && scannedMaps.isEmpty()

    companion object {
        val EMPTY = MapDefList(emptySet(), emptySet())
    }
}
