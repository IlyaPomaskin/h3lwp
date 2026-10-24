package com.homm3.livewallpaper.parser

import com.badlogic.gdx.utils.GdxNativesLoader
import com.homm3.livewallpaper.benchmark.ParserBenchmarkInputs
import com.homm3.livewallpaper.benchmark.ParserBenchmarkSuite
import com.homm3.livewallpaper.benchmark.ResourceMeter
import com.homm3.livewallpaper.benchmark.SpriteLods
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.lang.management.ManagementFactory

/**
 * Desktop run of [ParserBenchmarkSuite] over real game data. Run with:
 *   PARSER_BENCHMARK=1 ./gradlew :core:test --tests '*ParserBenchmark*' -i --rerun
 */
class ParserBenchmark {
    private val isEnabled = System.getenv(ENABLE_ENV) == "1"

    private val projectRoot: File by lazy {
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "data").isDirectory && File(it, "user-maps").isDirectory }
    }

    @Test
    fun benchmarkParsers() {
        assumeTrue("set $ENABLE_ENV=1 to run", isEnabled)
        GdxNativesLoader.load()

        val inputs = ParserBenchmarkInputs(
            mapFiles = projectRoot.resolve("user-maps").listFiles { file -> file.extension == MAP_EXTENSION }!!.toList(),
            defLods = DEF_LODS.map(projectRoot::resolve),
            indexLods = INDEX_LODS.map(projectRoot::resolve),
            spriteLods = SpriteLods(hota = projectRoot.resolve(SPRITE_HOTA_LOD), base = projectRoot.resolve(SPRITE_BASE_LOD)),
        )
        ParserBenchmarkSuite(inputs = inputs, meter = JvmResourceMeter(), report = ::println).run()
    }

    private class JvmResourceMeter : ResourceMeter {
        private val threadBean = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        private val osBean = ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean

        override fun cpuNanos(): Long = osBean.processCpuTime

        override fun allocatedBytes(): Long = threadBean.totalThreadAllocatedBytes
    }

    private companion object {
        const val ENABLE_ENV = "PARSER_BENCHMARK"
        const val MAP_EXTENSION = "h3m"
        const val SPRITE_HOTA_LOD = "assets-desktop/HotA.lod"
        const val SPRITE_BASE_LOD = "assets-desktop/H3sprite.lod"
        val DEF_LODS = listOf("data/H3sprite.lod", "data/hota_1.7.3/app/Data/HotA.lod")
        val INDEX_LODS = listOf("data/H3sprite.lod", "data/hota_1.7.3/app/Data/HotA.lod", "assets-desktop/HotA.lod")
    }
}
