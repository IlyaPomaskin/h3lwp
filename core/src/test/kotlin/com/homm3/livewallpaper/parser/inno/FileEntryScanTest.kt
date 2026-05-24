package com.homm3.livewallpaper.parser.inno

import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import java.io.ByteArrayInputStream
import java.io.RandomAccessFile

class FileEntryScanTest {
    @Test fun finds_hota_lod_destination_in_hota_180() {
        TestFixtures.requireInstallers()
        val installer = TestFixtures.installer180
        val setup0 = RandomAccessFile(installer, "r").use { raf ->
            val table = SetupLdrOffsetTable.parse(PeResource.readRcData(raf, 11111))
            CompressedBlockReader.readSetup0(raf, table.offset0)
        }
        val stream = ByteArrayInputStream(setup0)
        val header = SetupHeader.parse(stream)
        SetupEntries.skipUpToFileEntries(stream, header)

        val target = FileEntries.findByDestinationSuffix(
            stream, header.fileCount, suffix = "data\\hota.lod", caseInsensitive = true
        )
        assertNotNull("Did not find a file entry with destination ending in 'Data\\HotA.lod'", target)
        assertTrue(target!!.locationIndex < header.dataEntryCount)
        assertTrue(target.locationIndex >= 0)
    }
}
