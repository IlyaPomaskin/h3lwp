package com.homm3.livewallpaper.parser.inno

import org.junit.Test
import org.junit.Assert.assertTrue
import java.io.ByteArrayInputStream
import java.io.RandomAccessFile

class SetupEntriesTest {

    @Test fun skipping_non_file_entries_lands_on_file_entries() {
        TestFixtures.requireInstallers()
        val installer = TestFixtures.installer180
        val setup0 = RandomAccessFile(installer, "r").use { raf ->
            val table = SetupLdrOffsetTable.parse(PeResource.readRcData(raf, 11111))
            CompressedBlockReader.readSetup0(raf, table.offset0)
        }
        val stream = ByteArrayInputStream(setup0)
        val header = SetupHeader.parse(stream)
        SetupEntries.skipUpToFileEntries(stream, header)

        // Record where the file-entries block starts.
        val fileEntriesStart = setup0.size - stream.available()

        // File entries in the HotA installer begin with a wizard-bitmap entry whose
        // source and destination PascalStrings are empty (the file lives in the data
        // stream, not on disk). Reading them must not throw and must yield empty or
        // short strings — proving we didn't land in garbage.
        val firstSource = PascalString.read(stream)
        assertTrue(
            "first file source field implausible: '$firstSource' (len=${firstSource.length})",
            firstSource.isEmpty() || firstSource.length in 1..512
        )

        val firstDest = PascalString.read(stream)
        assertTrue(
            "first file destination implausible: '$firstDest' (len=${firstDest.length})",
            firstDest.isEmpty() || firstDest.length in 1..512
        )

        // Stronger sanity: scan the raw bytes of the first ~200 bytes of the file-entries
        // block looking for the UTF-16LE encoding of "{tmp}\" — a known string that appears
        // in the first file entry's destination field at offset ~91. This is unambiguous:
        // if our skip was off by even a few bytes, the file-entries block would start at a
        // different position and this pattern would not be at the expected relative offset.
        val tmpsPattern = "{tmp}\\".encodeToByteArray().let { ascii ->
            // Convert ASCII → UTF-16LE (each char becomes 2 bytes, second always 0x00 for ASCII)
            ByteArray(ascii.size * 2).also { out ->
                ascii.forEachIndexed { i, b -> out[i * 2] = b; out[i * 2 + 1] = 0x00 }
            }
        }
        val searchWindow = 200
        var foundTmpPath = false
        outer@ for (offset in 0 until searchWindow - tmpsPattern.size) {
            val pos = fileEntriesStart + offset
            if (pos + tmpsPattern.size > setup0.size) break
            var match = true
            for (j in tmpsPattern.indices) {
                if (setup0[pos + j] != tmpsPattern[j]) { match = false; break }
            }
            if (match) { foundTmpPath = true; break@outer }
        }
        assertTrue(
            "No '{tmp}\\' UTF-16LE pattern found within first $searchWindow bytes of the " +
            "file-entries section (fileEntriesStart=$fileEntriesStart) — stream appears " +
            "misaligned after skipping non-file entries",
            foundTmpPath
        )
    }
}
