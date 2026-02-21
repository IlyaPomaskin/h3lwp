@file:JvmName("Lwjgl3Launcher")

package com.homm3.livewallpaper.lwjgl3

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.core.WallpaperPreferences
import com.homm3.livewallpaper.parser.atlas.AtlasConverter
import kotlinx.coroutines.flow.MutableStateFlow
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.io.File
import kotlin.concurrent.thread

/** Launches the desktop (LWJGL3) application. */
fun main() {
    // This handles macOS support and helps on Windows.
    if (StartupHelper.startNewJvmIfRequired())
        return
    Lwjgl3Application(
        Engine(
            prefs = MutableStateFlow(WallpaperPreferences()),
            onSettingsButtonClick = ::selectLodFile
        ),
        Lwjgl3ApplicationConfiguration().apply {
            setTitle("Heroes 3 LiveWallpaper")
            //// Vsync limits the frames per second to what your hardware can display, and helps eliminate
            //// screen tearing. This setting doesn't always work on Linux, so the line after is a safeguard.
            useVsync(true)
            //// Limits FPS to the refresh rate of the currently active monitor, plus 1 to try to match fractional
            //// refresh rates. The Vsync setting above should limit the actual FPS to match the monitor.
            setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1)
            //// If you remove the above line and set Vsync to false, you can get unlimited FPS, which can be
            //// useful for testing performance, but can also be very stressful to some hardware.
            //// You may also need to configure GPU drivers to fully disable Vsync; this can cause screen tearing.


            setWindowedMode(640, 480)
            //// You can change these files; they are in lwjgl3/src/main/resources/ .
            //// They can also be loaded from the root of assets/ .
            setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))

            //// This could improve compatibility with Windows machines with buggy OpenGL drivers, Macs
            //// with Apple Silicon that have to emulate compatibility with OpenGL anyway, and more.
            //// This uses the dependency `com.badlogicgames.gdx:gdx-lwjgl3-angle` to function.
            //// You would need to add this line to lwjgl3/build.gradle , below the dependency on `gdx-backend-lwjgl3`:
            ////     implementation "com.badlogicgames.gdx:gdx-lwjgl3-angle:$gdxVersion"
            //// You can choose to add the following line and the mentioned dependency if you want; they
            //// are not intended for games that use GL30 (which is compatibility with OpenGL ES 3.0).
            //// Know that it might not work well in some cases.
//        setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.ANGLE_GLES20, 0, 0)

        })
}

private fun selectLodFile(onProgress: (String) -> Unit, onDone: () -> Unit) {
    val path = MemoryStack.stackPush().use { stack ->
        val filter: PointerBuffer = stack.mallocPointer(1)
        filter.put(stack.UTF8("*.lod"))
        filter.flip()
        TinyFileDialogs.tinyfd_openFileDialog(
            "Select h3sprite.lod",
            null,
            filter,
            "HoMM3 LOD files (*.lod)",
            false
        )
    } ?: return

    thread(isDaemon = true) {
        val selected = File(path)
        val outputDir = Gdx.files.local(AssetPaths.ATLAS_FOLDER).file()
        outputDir.mkdirs()
        try {
            AtlasConverter(selected.inputStream(), outputDir, AssetPaths.ATLAS_NAME)
                .convert(onProgress)
            onDone()
        } catch (e: Exception) {
            onProgress(e.message ?: "Conversion failed")
        }
    }
}
