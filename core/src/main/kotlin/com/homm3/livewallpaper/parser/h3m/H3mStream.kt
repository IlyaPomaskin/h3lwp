package com.homm3.livewallpaper.parser.h3m

import com.homm3.livewallpaper.parser.readAtMost
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream

private const val GZIP_MAGIC_SIZE = 2
private const val BITS_PER_BYTE = 8

/** The largest real maps unpack to about 1.2 MB; the cap stops a corrupt or malicious file from filling the heap. */
const val MAX_UNPACKED_MAP_SIZE = 4 * 1024 * 1024

/** Maps are usually gzip-compressed, but the game also accepts uncompressed ones. */
fun InputStream.unpackH3m(): InputStream {
    val buffered = BufferedInputStream(this)
    buffered.mark(GZIP_MAGIC_SIZE)
    val magic = buffered.read() or (buffered.read() shl BITS_PER_BYTE)
    buffered.reset()
    val isGzip = magic == GZIPInputStream.GZIP_MAGIC
    if (isGzip) return GZIPInputStream(buffered)
    return buffered
}

/** Unpacks the whole map; throws [IOException] once it exceeds [MAX_UNPACKED_MAP_SIZE]. */
fun InputStream.readUnpackedH3m(): ByteArray =
    unpackH3m().readAtMost(MAX_UNPACKED_MAP_SIZE)
        ?: throw IOException("Map unpacks to more than $MAX_UNPACKED_MAP_SIZE bytes")
