package com.homm3.livewallpaper.parser.inno

import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import java.io.ByteArrayInputStream
import java.io.RandomAccessFile

class SetupHeaderTest {
    @Test fun parses_hota_180_header_counts() {
        TestFixtures.requireInstallers()
        val installer = TestFixtures.installer180
        val setup0 = RandomAccessFile(installer, "r").use { raf ->
            val table = SetupLdrOffsetTable.parse(PeResource.readRcData(raf, 11111))
            CompressedBlockReader.readSetup0(raf, table.offset0)
        }
        val stream = ByteArrayInputStream(setup0)
        val header = SetupHeader.parse(stream)

        assertEquals("HotA + HD", header.appName)
        assertTrue("languages: ${header.languageCount}", header.languageCount in 1..16)
        assertTrue("messages: ${header.messageCount}", header.messageCount in 1..1000)
        assertTrue("files: ${header.fileCount}", header.fileCount in 100..100_000)
        assertTrue("data entries: ${header.dataEntryCount}", header.dataEntryCount in 100..100_000)
        assertTrue("permission: ${header.permissionCount}", header.permissionCount in 0..1000)
        assertTrue("types: ${header.typeCount}", header.typeCount in 0..100)
        assertTrue("components: ${header.componentCount}", header.componentCount in 0..100)
        assertTrue("tasks: ${header.taskCount}", header.taskCount in 0..100)
        assertTrue("directories: ${header.directoryCount}", header.directoryCount in 0..10000)
    }
}
