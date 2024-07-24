package com.homm3.livewallpaper.fhParser

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.badlogic.gdx.utils.GdxNativesLoader
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.homm3.livewallpaper.parser.AssetsWriter
import java.io.File
import java.lang.reflect.Type
import kotlin.concurrent.thread

val gson = Gson()
val fhSpriteRootType: Type = object : TypeToken<FhSpriteRoot>() {}.type

fun parseSpriteJson(json: String): FhSpriteRoot {
    return gson.fromJson(json, fhSpriteRootType)
}

fun addFiles(list: MutableList<File>, directory: File) {
    directory.listFiles { it -> it.isDirectory }?.forEach { addFiles(list, it) }

    directory
        .listFiles { it -> it.isFile && it.extension == "json" }
        ?.forEach { nextFile ->
            val existingFile = list.find { listFile -> listFile.name == nextFile.name }
            if (existingFile != null) {
                println("DOUBLE ${existingFile.name} NEW ${nextFile.name}")
                list.remove(existingFile)
            }
            list.add(nextFile)
        }
}

fun getFilesListFromFolder(folders: List<File>): MutableList<File> {
    val files = mutableListOf<File>()

    folders.forEach { folder ->
        folder
            .listFiles { it -> it.isDirectory }
            ?.forEach { directory -> addFiles(files, directory) }

        addFiles(files, folder)
    }

    return files
}

fun repackTerrainFolder(folders: List<File>, output: File, onDone: () -> Unit) {
    GdxNativesLoader.load()

    val packer = PixmapPacker(2048, 2048, Pixmap.Format.RGBA4444, 0, false)

    thread {
        val assetsWriter = AssetsWriter(packer, output, "terrain")

        println("repackTerrainFolder START")

        getFilesListFromFolder(folders)
            .forEach { file ->
                val json = parseSpriteJson(file.readText())
                val defName = file.name.replace(".fhsprite.json", "")
                val srcPixmap = Pixmap(FileHandle(file.parentFile.resolve("${defName}.png")))

                json.groups.forEachIndexed { groupIndex, group ->
                    group.value.frames.forEachIndexed { index, it ->
                        val newPixmap =
                            Pixmap(json.boundarySize.w, json.boundarySize.w, Pixmap.Format.RGBA4444)

                        newPixmap.drawPixmap(
                            srcPixmap,
                            it.bitmapOffset?.x ?: 0,
                            it.bitmapOffset?.y ?: 0,
                            it.bitmapSize?.w ?: 0,
                            it.bitmapSize?.h ?: 0,
                            it.padding?.x ?: 0,
                            it.padding?.y ?: 0,
                            it.bitmapSize?.w ?: 0,
                            it.bitmapSize?.h ?: 0,
                        )

                        packer.pack("$defName/$groupIndex/$index", newPixmap)
                    }
                }
            }

        assetsWriter.writePackerContent(packer)
        println("repackTerrainFolder DONE")
        onDone()
    }
}

fun repackObjectsFolder(folders: List<File>, output: File, onDone: () -> Unit) {
    GdxNativesLoader.load()

    val packer = PixmapPacker(2048, 2048, Pixmap.Format.RGBA4444, 0, false)

    thread {
        println("repackObjectsFolder START")
        val assetsWriter = AssetsWriter(packer, output, "objects")

        getFilesListFromFolder(folders).forEach { file ->
            val json = parseSpriteJson(file.readText())
            val defName = file.name.replace(".fhsprite.json", "")
            val srcPixmap = Pixmap(FileHandle(file.parentFile.resolve("${defName}.png")))

            val group = json.groups[0]
            group.value.frames.forEachIndexed { index, it ->
                val newPixmap =
                    Pixmap(json.boundarySize.w, json.boundarySize.w, Pixmap.Format.RGBA4444)

                newPixmap.drawPixmap(
                    srcPixmap,
                    it.bitmapOffset?.x ?: 0,
                    it.bitmapOffset?.y ?: 0,
                    it.bitmapSize?.w ?: 0,
                    it.bitmapSize?.h ?: 0,
                    it.padding?.x ?: 0,
                    it.padding?.y ?: 0,
                    it.bitmapSize?.w ?: 0,
                    it.bitmapSize?.h ?: 0,
                )

                packer.pack("$defName/$index", newPixmap)
            }
        }

        assetsWriter.writePackerContent(packer)
        println("repackObjectsFolder DONE")
        onDone()
    }
}
