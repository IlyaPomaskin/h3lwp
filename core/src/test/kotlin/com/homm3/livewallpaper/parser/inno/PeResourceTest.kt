package com.homm3.livewallpaper.parser.inno

import org.junit.Test
import org.junit.Assert.assertEquals
import java.io.RandomAccessFile

class PeResourceTest {
    @Test fun finds_setup_ldr_offset_table_in_hota_180() {
        TestFixtures.requireInstallers()
        RandomAccessFile(TestFixtures.installer180, "r").use { raf ->
            val bytes = PeResource.readRcData(raf, resourceId = 11111)
            assertEquals(44, bytes.size)
        }
    }

    @Test fun finds_setup_ldr_offset_table_in_hota_173() {
        TestFixtures.requireInstallers()
        RandomAccessFile(TestFixtures.installer173, "r").use { raf ->
            val bytes = PeResource.readRcData(raf, resourceId = 11111)
            assertEquals(44, bytes.size)
        }
    }
}
