package com.homm3.livewallpaper.parser.inno

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GogGalaxyFilesTest {
    @Test fun groups_parts_under_the_start_entry_destination() {
        val entries = listOf(
            FileEntry(
                destination = "{tmp}/e0/9d\\e09d",
                locationIndex = 1,
                afterInstall = "after_install('3edc', 57741, 126976)",
                beforeInstall = "before_install('3edc', 'BINKW32.DLL', 1)",
            ),
            FileEntry(
                destination = "{tmp}/31/1d\\311d",
                locationIndex = 42,
                afterInstall = "after_install('3230', 10143274, 10485760)",
                beforeInstall = "before_install('2be0', 'Data\\H3sprite.lod', 2)",
            ),
            FileEntry(
                destination = "{tmp}/47/78\\4778",
                locationIndex = 43,
                afterInstall = "after_install('9c37', 10488585, 12544359)",
            ),
        )

        val files = GogGalaxyFiles.resolve(entries)

        assertEquals(
            listOf(
                LogicalFile("BINKW32.DLL", listOf(FilePart(1, isZlibCompressed = true))),
                LogicalFile(
                    "Data\\H3sprite.lod",
                    listOf(FilePart(42, isZlibCompressed = true), FilePart(43, isZlibCompressed = true)),
                ),
            ),
            files,
        )
    }

    @Test fun reads_dependency_variants() {
        val entries = listOf(
            FileEntry(
                destination = "{tmp}/2c/5f\\2c5f",
                locationIndex = 0,
                afterInstall = "after_install_dependency('c8aa', 1167849, 1253424)",
                beforeInstall = "before_install_dependency('c8aa', '__redist/ISI/scriptinterpreter.exe', 1)",
            ),
        )

        val files = GogGalaxyFiles.resolve(entries)

        assertEquals("__redist/ISI/scriptinterpreter.exe", files?.single()?.destination)
    }

    @Test fun returns_null_for_regular_installers() {
        val entries = listOf(FileEntry(destination = "{app}\\Data\\HotA.lod", locationIndex = 5))

        assertNull(GogGalaxyFiles.resolve(entries))
    }
}
