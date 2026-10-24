package com.homm3.livewallpaper.core.assets

import org.junit.Test
import kotlin.test.assertEquals

class ExcludeDisabledMapsTest {
    @Test
    fun dropsDisabledMapsKeepingOrder() {
        val maps = excludeDisabledMaps(listOf("c.h3m", "a.h3m", "b.h3m"), setOf("a.h3m", "gone.h3m"))

        assertEquals(listOf("c.h3m", "b.h3m"), maps)
    }

    @Test
    fun keepsAllMapsWhenEveryMapIsDisabled() {
        val maps = excludeDisabledMaps(listOf("a.h3m", "b.h3m"), setOf("a.h3m", "b.h3m"))

        assertEquals(listOf("a.h3m", "b.h3m"), maps)
    }
}
