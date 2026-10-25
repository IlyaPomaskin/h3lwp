package com.homm3.livewallpaper.parser.h3m

import com.homm3.livewallpaper.parser.BinaryReader
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

/**
 * Parses every map in the folder named by the H3M_SCAN_DIR environment variable and its subfolders, in parallel.
 * Writes one line per map to `<dir>-scan.txt` and copies failed or suspect maps to `<dir>-failed/`,
 * keeping their relative paths.
 * Run: H3M_SCAN_DIR=/abs/path/maps ./gradlew :core:test --tests '*MapCorpusScanTest*'
 */
class MapCorpusScanTest {
    private enum class Status { FAIL, SUSPECT, OK }

    private class ScanResult(val file: File, val status: Status, val details: String) {
        lateinit var relativePath: String
    }

    private class ParseStats(
        val map: H3mMap,
        val bytesRead: Long,
        val bytesTotal: Int,
        val warnings: List<String>,
    )

    @Test
    fun scanCorpus() {
        val scanDirPath = System.getenv(SCAN_DIR_ENV)
        assumeTrue("$SCAN_DIR_ENV is not set", !scanDirPath.isNullOrBlank())
        val scanDir = File(scanDirPath).canonicalFile
        require(scanDir.isDirectory) { "$scanDir is not a directory" }

        val mapFiles = scanDir.walkTopDown()
            .filter { it.isFile && it.extension.equals(MAP_EXTENSION, ignoreCase = true) }
            .sortedBy { it.path.lowercase() }
            .toList()
        val warningsByThread = installWarningCollector()
        val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        val results = mapFiles
            .map { file -> pool.submit<ScanResult> { scanWithTimeout(file, warningsByThread) } }
            .map { it.get().apply { relativePath = file.relativeTo(scanDir).path } }
        pool.shutdown()
        pool.awaitTermination(1, TimeUnit.MINUTES)

        writeReport(scanDir, results)
        copyFailedMaps(scanDir, results)
        val countsByStatus = results.groupingBy { it.status }.eachCount()
        println("SCANNED ${results.size}: $countsByStatus")
    }

    private fun scanWithTimeout(file: File, warningsByThread: ThreadLocal<MutableList<String>>): ScanResult {
        var result: ScanResult? = null
        val worker = Thread {
            result = scan(file, warningsByThread)
        }
        worker.isDaemon = true
        worker.start()
        worker.join(TIMEOUT_MS)
        return result ?: ScanResult(file, Status.FAIL, "error=Timeout after ${TIMEOUT_MS}ms")
    }

    private fun scan(file: File, warningsByThread: ThreadLocal<MutableList<String>>): ScanResult {
        val startedAt = System.currentTimeMillis()
        return try {
            warningsByThread.get().clear()
            val stats = parse(file, warningsByThread.get())
            val elapsedMs = System.currentTimeMillis() - startedAt
            describe(file, stats, elapsedMs)
        } catch (throwable: Throwable) {
            val message = throwable.message.orEmpty().lineSequence().first().take(MAX_ERROR_LENGTH)
            ScanResult(file, Status.FAIL, "error=${throwable.javaClass.simpleName}: $message")
        }
    }

    private fun parse(file: File, warnings: List<String>): ParseStats {
        val parser = file.inputStream().use { H3mReader(it) }
        val map = parser.read()
        val binaryReader = readerField.get(parser) as BinaryReader
        val data = dataField.get(binaryReader) as ByteArray
        return ParseStats(map, binaryReader.position, data.size, warnings.toList())
    }

    private fun describe(file: File, stats: ParseStats, elapsedMs: Long): ScanResult {
        val map = stats.map
        val size = map.header.size
        val levels = if (map.header.hasUnderground) 2 else 1
        val maxZ = levels - 1
        val maxCoordinate = size + COORDINATE_SLACK
        val expectedTiles = size * size * levels
        val bytesLeft = stats.bytesTotal - stats.bytesRead
        val outOfBoundsObjects = map.objects.count { it.x > maxCoordinate || it.y > maxCoordinate || it.z > maxZ }
        val unknownTypeObjects = map.objects.count { it.objectType == H3mObjectType.NO_OBJ }
        val emptyDefNames = map.defs.count { it.spriteName.isBlank() }

        val hasValidSize = size in VALID_SIZE_RANGE
        val hasExpectedTiles = map.tiles.size == expectedTiles
        val hasReachedEnd = bytesLeft <= 0
        val hasWarnings = stats.warnings.isNotEmpty()
        val hasObjects = map.objects.isNotEmpty()
        val isSuspect = !hasValidSize || !hasExpectedTiles || hasReachedEnd || hasWarnings || !hasObjects ||
            outOfBoundsObjects > 0 || emptyDefNames > 0
        val status = if (isSuspect) Status.SUSPECT else Status.OK

        val firstWarning = stats.warnings.firstOrNull()?.lineSequence()?.first()?.take(MAX_ERROR_LENGTH)
        val details = buildString {
            append("v=${map.version} sub=${map.hotaSubVersion} size=$size ug=${map.header.hasUnderground}")
            append(" tiles=${map.tiles.size}/$expectedTiles defs=${map.defs.size} objs=${map.objects.size}")
            append(" badPos=$outOfBoundsObjects noType=$unknownTypeObjects emptyDef=$emptyDefNames")
            append(" read=${stats.bytesRead}/${stats.bytesTotal} left=$bytesLeft warn=${stats.warnings.size}")
            append(" ms=$elapsedMs")
            if (firstWarning != null) append(" firstWarn=\"$firstWarning\"")
        }
        return ScanResult(file, status, details)
    }

    private fun writeReport(scanDir: File, results: List<ScanResult>) {
        val reportFile = scanDir.resolveSibling("${scanDir.name}$REPORT_SUFFIX")
        val lines = results
            .sortedWith(compareBy<ScanResult> { it.status }.thenBy { it.relativePath.lowercase() })
            .map { "${it.status.name.padEnd(STATUS_WIDTH)} ${it.relativePath}\t${it.details}" }
        reportFile.writeText(lines.joinToString("\n", postfix = "\n"))
        println("REPORT ${reportFile.absolutePath}")
    }

    private fun copyFailedMaps(scanDir: File, results: List<ScanResult>) {
        val failedDir = scanDir.resolveSibling("${scanDir.name}$FAILED_SUFFIX")
        failedDir.deleteRecursively()
        results
            .filter { it.status != Status.OK }
            .forEach { result ->
                val target = failedDir.resolve(result.status.name.lowercase()).resolve(result.relativePath)
                result.file.copyTo(target, overwrite = true)
            }
        println("FAILED_DIR ${failedDir.absolutePath}")
    }

    private fun installWarningCollector(): ThreadLocal<MutableList<String>> {
        val warningsByThread = ThreadLocal.withInitial { mutableListOf<String>() }
        val logger = Logger.getLogger(H3mReader::class.java.name)
        logger.useParentHandlers = false
        logger.addHandler(object : Handler() {
            override fun publish(record: LogRecord) {
                if (record.level.intValue() < Level.WARNING.intValue()) return
                warningsByThread.get().add(record.message.orEmpty())
            }

            override fun flush() = Unit
            override fun close() = Unit
        })
        return warningsByThread
    }

    private companion object {
        const val SCAN_DIR_ENV = "H3M_SCAN_DIR"
        const val MAP_EXTENSION = "h3m"
        const val REPORT_SUFFIX = "-scan.txt"
        const val FAILED_SUFFIX = "-failed"
        const val TIMEOUT_MS = 20_000L
        const val MAX_ERROR_LENGTH = 160
        const val STATUS_WIDTH = 7
        const val COORDINATE_SLACK = 8
        val VALID_SIZE_RANGE = 36..256

        val readerField = H3mReader::class.java.getDeclaredField("reader").apply { isAccessible = true }
        val dataField = BinaryReader::class.java.getDeclaredField("data").apply { isAccessible = true }
    }
}
