package com.homm3.livewallpaper.parser.inno

/** One installed file, assembled from one or more data locations in order. */
data class LogicalFile(val destination: String, val parts: List<FilePart>)

data class FilePart(val locationIndex: Int, val isZlibCompressed: Boolean)

/**
 * GOG Galaxy installers split every file into parts. The first part's file entry carries
 * `before_install('<md5>', '<real path>', <part count>)`, every part carries
 * `after_install('<md5>', <compressed size>, <uncompressed size>)`, and each part's data is a
 * separate zlib stream. Mirrors innoextract's `gog::parse_galaxy_files`.
 */
object GogGalaxyFiles {
    private val BEFORE_INSTALL_FUNCTIONS = listOf("before_install", "before_install_dependency")
    private val AFTER_INSTALL_FUNCTIONS = listOf("after_install", "after_install_dependency")
    private const val DESTINATION_ARGUMENT = 1
    private const val PART_COUNT_ARGUMENT = 2

    /** Returns the logical files, or `null` if [entries] don't come from a GOG Galaxy installer. */
    fun resolve(entries: List<FileEntry>): List<LogicalFile>? {
        val isGalaxyInstaller = entries.any { callArguments(it.beforeInstall, BEFORE_INSTALL_FUNCTIONS) != null }
        if (!isGalaxyInstaller) return null

        val files = mutableListOf<LogicalFile>()
        var remainingParts = 0
        for (entry in entries) {
            val start = callArguments(entry.beforeInstall, BEFORE_INSTALL_FUNCTIONS)
            if (start != null) {
                val destination = start.getOrNull(DESTINATION_ARGUMENT)?.takeIf { it.isNotEmpty() } ?: entry.destination
                files += LogicalFile(destination, emptyList())
                remainingParts = start.getOrNull(PART_COUNT_ARGUMENT)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            }

            val isPart = callArguments(entry.afterInstall, AFTER_INSTALL_FUNCTIONS) != null
            if (isPart && remainingParts > 0) {
                val current = files.removeAt(files.lastIndex)
                files += current.copy(parts = current.parts + FilePart(entry.locationIndex, isZlibCompressed = true))
                remainingParts--
            }
        }
        return files.filter { it.parts.isNotEmpty() }
    }

    private fun callArguments(code: String, functionNames: List<String>): List<String>? {
        val trimmed = code.trim().removeSuffix(";").trim()
        val openParenthesis = trimmed.indexOf('(')
        val isCall = openParenthesis > 0 && trimmed.endsWith(")")
        if (!isCall) return null

        val name = trimmed.substring(0, openParenthesis).trim()
        if (name !in functionNames) return null

        return parseArguments(trimmed.substring(openParenthesis + 1, trimmed.length - 1))
    }

    private fun parseArguments(argumentList: String): List<String> {
        val arguments = mutableListOf<String>()
        val current = StringBuilder()
        var isInsideString = false
        var i = 0
        while (i < argumentList.length) {
            val char = argumentList[i]
            val isEscapedQuote = isInsideString && char == '\'' && argumentList.getOrNull(i + 1) == '\''
            when {
                isEscapedQuote -> { current.append('\''); i++ }
                char == '\'' -> isInsideString = !isInsideString
                char == ',' && !isInsideString -> { arguments += current.toString().trim(); current.clear() }
                else -> current.append(char)
            }
            i++
        }
        arguments += current.toString().trim()
        return arguments
    }
}
