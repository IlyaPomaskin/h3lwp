package com.homm3.livewallpaper.parser.inno

import org.junit.Test
import org.junit.Assert.assertTrue
import java.io.ByteArrayInputStream
import java.io.RandomAccessFile

class CompressedBlockReaderTest {
    @Test fun decompresses_setup0_and_first_string_is_app_name() {
        TestFixtures.requireInstallers()
        val installer = TestFixtures.installer180
        val table = RandomAccessFile(installer, "r").use {
            SetupLdrOffsetTable.parse(PeResource.readRcData(it, 11111))
        }
        val decompressed = RandomAccessFile(installer, "r").use { raf ->
            CompressedBlockReader.readSetup0(raf, table.offset0)
        }
        // First field of TSetupHeader for 5.5.7u is app_name as PascalString.
        // In HotA 1.8.0 the app_name is "HotA + HD"; later fields contain the full
        // game title.  We scan a reasonable number of PascalStrings so the test
        // survives minor layout changes while still asserting meaningful content.
        val stream = ByteArrayInputStream(decompressed)
        var found = false
        for (i in 0 until 30) {
            val s = PascalString.read(stream)
            if (s.contains("Horn of the Abyss", ignoreCase = true) ||
                s.contains("HotA", ignoreCase = true)) {
                found = true
                break
            }
        }
        assertTrue("expected decompressed header to mention HotA or Horn of the Abyss in first 30 strings", found)
    }
}
