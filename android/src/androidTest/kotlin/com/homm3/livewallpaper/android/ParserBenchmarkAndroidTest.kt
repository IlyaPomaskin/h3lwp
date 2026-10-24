package com.homm3.livewallpaper.android

import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import android.os.Process
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.badlogic.gdx.utils.GdxNativesLoader
import com.homm3.livewallpaper.benchmark.ParserBenchmarkInputs
import com.homm3.livewallpaper.benchmark.ParserBenchmarkSuite
import com.homm3.livewallpaper.benchmark.ResourceMeter
import com.homm3.livewallpaper.benchmark.SpriteLods
import com.homm3.livewallpaper.core.AssetPaths
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * On-device run of [ParserBenchmarkSuite]. Needs the installed game data in the app's files dir and the maps pushed to
 * `<external files dir>/benchmark-maps`. Results go to logcat under the [TAG] tag.
 */
@RunWith(AndroidJUnit4::class)
class ParserBenchmarkAndroidTest {

    @Test
    fun benchmarkParsers() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val hotaLod = context.filesDir.resolve(AssetPaths.HOTA_LOD_FILE)
        val baseLod = context.filesDir.resolve(AssetPaths.LOD_FILE)
        val mapsDir = File(context.getExternalFilesDir(null), MAPS_FOLDER)
        val mapFiles = mapsDir.listFiles { file -> file.extension == MAP_EXTENSION }.orEmpty().toList()
        assumeTrue("game data not installed", hotaLod.isFile && baseLod.isFile)
        assumeTrue("no maps in $mapsDir", mapFiles.isNotEmpty())
        GdxNativesLoader.load()

        val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        Log.i(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}, API ${Build.VERSION.SDK_INT}, debuggable=$isDebuggable")

        val inputs = ParserBenchmarkInputs(
            mapFiles = mapFiles,
            defLods = listOf(baseLod),
            indexLods = listOf(baseLod, hotaLod),
            spriteLods = SpriteLods(hota = hotaLod, base = baseLod),
            defBytesBudget = DEF_BYTES_BUDGET,
        )
        ParserBenchmarkSuite(inputs = inputs, meter = ArtResourceMeter(), report = { line -> Log.i(TAG, line) }).run()
    }

    private class ArtResourceMeter : ResourceMeter {
        override fun cpuNanos(): Long = Process.getElapsedCpuTime() * NANOS_PER_MILLI

        override fun allocatedBytes(): Long = Debug.getRuntimeStat(BYTES_ALLOCATED_STAT).toLong()
    }

    private companion object {
        const val TAG = "ParserBenchmark"
        const val MAPS_FOLDER = "benchmark-maps"
        const val MAP_EXTENSION = "h3m"
        const val NANOS_PER_MILLI = 1_000_000L
        const val BYTES_ALLOCATED_STAT = "art.gc.bytes-allocated"
        // Keeps the preloaded DEFs well under the default 256 MB app heap.
        const val DEF_BYTES_BUDGET = 48L * 1024 * 1024
    }
}
