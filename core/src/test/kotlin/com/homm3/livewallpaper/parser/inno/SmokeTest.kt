package com.homm3.livewallpaper.parser.inno

import org.junit.Assert.assertTrue
import org.junit.Test

class SmokeTest {
    @Test
    fun installers_are_accessible() {
        TestFixtures.requireInstallers()
        assertTrue(TestFixtures.installer180.length() > 100_000_000L)
    }
}
