package com.homm3.livewallpaper.core.install

import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.parser.readUpTo
import com.homm3.livewallpaper.parser.sit.StuffIt5Reader
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Finds H3sprite.lod in a file the user picked: the LOD itself, a StuffIt 5 archive (the free Mac demo on archive.org)
 * or a ZIP holding either of them.
 */
internal object BaseLodArchive {
    private val ZIP_SIGNATURE = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
    private const val STUFFIT_EXTENSION = ".sit"
    private const val PERCENT = 100L

    /**
     * Writes H3sprite.lod from [source] to [target]. Returns false when [source] is an archive without it.
     * Anything that is not a known archive is copied as is, for the LOD validator to judge.
     */
    fun extract(source: InputStream, target: File, onProgress: (percent: Int) -> Unit): Boolean {
        val input = BufferedInputStream(source)
        val header = peekHeader(input)
        if (header.startsWith(ZIP_SIGNATURE)) return extractFromZip(ZipInputStream(input), target, onProgress)
        if (StuffIt5Reader.hasSignature(header)) return extractFromStuffIt(input, target, onProgress)

        target.outputStream().use { input.copyTo(it) }
        return true
    }

    private fun extractFromZip(zip: ZipInputStream, target: File, onProgress: (Int) -> Unit): Boolean =
        generateSequence { zip.nextEntry }
            .filterNot { it.isDirectory }
            .any { entry ->
                val fileName = baseName(entry.name)
                when {
                    isBaseLod(fileName) -> {
                        target.outputStream().use { zip.copyTo(it) }
                        true
                    }
                    fileName.endsWith(STUFFIT_EXTENSION, ignoreCase = true) -> extractFromStuffIt(zip, target, onProgress)
                    else -> false
                }
            }

    private fun extractFromStuffIt(input: InputStream, target: File, onProgress: (Int) -> Unit): Boolean =
        target.outputStream().buffered().use { output ->
            StuffIt5Reader.extract(input, ::isBaseLod, output) { written, total ->
                if (total > 0) onProgress((written * PERCENT / total).toInt())
            }
        }

    private fun isBaseLod(fileName: String): Boolean = fileName.equals(AssetPaths.LOD_FILE, ignoreCase = true)

    private fun peekHeader(input: BufferedInputStream): ByteArray {
        input.mark(StuffIt5Reader.SIGNATURE_LENGTH)
        val header = input.readUpTo(StuffIt5Reader.SIGNATURE_LENGTH)
        input.reset()
        return header
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && copyOf(prefix.size).contentEquals(prefix)
}
