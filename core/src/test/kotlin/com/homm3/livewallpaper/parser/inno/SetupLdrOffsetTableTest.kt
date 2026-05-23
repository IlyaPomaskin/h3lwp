package com.homm3.livewallpaper.parser.inno

import org.junit.Test
import org.junit.Assert.assertTrue
import java.io.RandomAccessFile

class SetupLdrOffsetTableTest {
    @Test fun parses_hota_180_loader_table() {
        TestFixtures.requireInstallers()
        val raw = RandomAccessFile(TestFixtures.installer180, "r").use {
            PeResource.readRcData(it, 11111)
        }
        val table = SetupLdrOffsetTable.parse(raw)
        assertTrue(table.offset0 > 0)
        assertTrue(table.offset1 > table.offset0)
        assertTrue(table.totalSize >= TestFixtures.installer180.length() - 1024)
    }
}
