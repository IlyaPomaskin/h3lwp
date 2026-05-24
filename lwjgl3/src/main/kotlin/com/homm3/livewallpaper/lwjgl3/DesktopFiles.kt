package com.homm3.livewallpaper.lwjgl3

import com.badlogic.gdx.Files
import com.badlogic.gdx.Files.FileType
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3FileHandle
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Files
import com.badlogic.gdx.files.FileHandle
import java.io.File

/** Same as [Lwjgl3Files] but routes every `local(path)` lookup to [localBase]
 *  instead of the JVM cwd. `internal`, `classpath`, `external`, `absolute` are
 *  delegated unchanged so bundled-asset reading still works. */
class DesktopFiles(localBase: File) : Files {
    private val base = Lwjgl3Files()
    private val localBaseAbs: String = localBase.absoluteFile.apply { mkdirs() }.path + File.separator

    init {
        java.util.logging.Logger.getLogger("DesktopFiles").info("local routed to: $localBaseAbs")
    }

    /** Construct a Local-typed handle whose `file()` returns an absolute path
     *  under [localBaseAbs]. We use [FileType.Absolute] so `Lwjgl3FileHandle.file()`
     *  returns the file as-is without prepending [Lwjgl3Files.localPath]. */
    override fun local(path: String): FileHandle =
        Lwjgl3FileHandle(File(localBaseAbs, path), FileType.Absolute)

    override fun getFileHandle(fileName: String, type: FileType): FileHandle =
        if (type == FileType.Local) local(fileName) else base.getFileHandle(fileName, type)

    override fun classpath(path: String): FileHandle = base.classpath(path)
    override fun internal(path: String): FileHandle = base.internal(path)
    override fun external(path: String): FileHandle = base.external(path)
    override fun absolute(path: String): FileHandle = base.absolute(path)

    override fun getExternalStoragePath(): String = base.externalStoragePath
    override fun isExternalStorageAvailable(): Boolean = base.isExternalStorageAvailable

    override fun getLocalStoragePath(): String = localBaseAbs
    override fun isLocalStorageAvailable(): Boolean = true
}
