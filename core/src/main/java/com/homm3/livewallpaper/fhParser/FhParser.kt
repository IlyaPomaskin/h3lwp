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

fun repackTerrainFolder(folder: File) {
    GdxNativesLoader.load()

    val packer = PixmapPacker(2048, 2048, Pixmap.Format.RGBA4444, 0, false)

    thread {
//        folder.resolve("../../terrain").mkdirs()
        val assetsWriter = AssetsWriter(packer, folder.resolve("../../"), "terrain")
//        val spritesToGroup = mutableMapOf<String, Group>()

        println("repackTerrainFolder START")

        folder
            .listFiles { it -> it.extension == "json" }
            ?.forEach { file ->
                val json = parseSpriteJson(file.readText())
                val defName = file.name.replace(".fhsprite.json", "")
                val srcPixmap = Pixmap(FileHandle(folder.resolve("${defName}.png")))

                json.groups.forEachIndexed { groupIndex, group ->
                    group.value.frames.forEachIndexed { index, it ->
                        val newPixmap =
                            Pixmap(json.boundarySize.w, json.boundarySize.w, Pixmap.Format.RGBA4444)

                        newPixmap.drawPixmap(
                            srcPixmap,
                            it.bitmapOffset?.x ?: 0,
                            it.bitmapOffset?.y ?: 0,
                            it.bitmapSize.w,
                            it.bitmapSize.h,
                            it.padding?.x ?: 0,
                            it.padding?.y ?: 0,
                            it.bitmapSize.w,
                            it.bitmapSize.h,
                        )

                        packer.pack("$defName/$groupIndex/$index", newPixmap)
                    }
//                    spritesToGroup[defName] = group
                }
            }

        assetsWriter.writePackerContent(packer)
        println("repackTerrainFolder DONE")
    }
}


fun repackObjectsFolder(folder: File) {
    GdxNativesLoader.load()

    val packer = PixmapPacker(2048, 2048, Pixmap.Format.RGBA4444, 0, false)

    thread {
        println("repackObjectsFolder START")
//        folder.resolve("../objects").mkdirs()
        val assetsWriter = AssetsWriter(packer, folder.resolve("../"), "objects")
//        val spritesToGroup = mutableMapOf<String, Group>()

        val files = mutableListOf<File>()

        folder.resolve("./Artifact").listFiles { it -> it.extension == "json" }
            ?.let { it1 -> files.addAll(it1) }
        folder.resolve("./Hero").listFiles { it -> it.extension == "json" }
            ?.let { it1 -> files.addAll(it1) }
        folder.resolve("./Creature/Castle").listFiles { it -> it.extension == "json" }
            ?.let { it1 -> files.addAll(it1) }
        folder.resolve("./Creature/Conflux").listFiles { it -> it.extension == "json" }
            ?.let { it1 -> files.addAll(it1) }
        folder.resolve("./Creature/Dungeon").listFiles { it -> it.extension == "json" }
            ?.let { it1 -> files.addAll(it1) }
        folder.resolve("./Creature/Fortress").listFiles { it -> it.extension == "json" }
            ?.let { it1 -> files.addAll(it1) }
        folder.resolve("./Creature/Inferno").listFiles { it -> it.extension == "json" }
            ?.let { it1 -> files.addAll(it1) }
        folder.resolve("./Creature/Necropolis").listFiles { it -> it.extension == "json" }
            ?.let { it1 -> files.addAll(it1) }
        folder.resolve("./Creature/Neutral").listFiles { it -> it.extension == "json" }
            ?.let { it1 -> files.addAll(it1) }
        folder.resolve("./Creature/Rampart").listFiles { it -> it.extension == "json" }
            ?.let { it1 -> files.addAll(it1) }
        folder.resolve("./Creature/Stronghold").listFiles { it -> it.extension == "json" }
            ?.let { it1 -> files.addAll(it1) }
        folder.resolve("./Creature/Tower").listFiles { it -> it.extension == "json" }
            ?.let { it1 -> files.addAll(it1) }
        folder.listFiles { it -> it.extension == "json" }?.let { it1 -> files.addAll(it1) }

        files.forEach { file ->
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
                    it.bitmapSize.w,
                    it.bitmapSize.h,
                    it.padding?.x ?: 0,
                    it.padding?.y ?: 0,
                    it.bitmapSize.w,
                    it.bitmapSize.h,
                )

                packer.pack("$defName/$index", newPixmap)
            }
        }

        assetsWriter.writePackerContent(packer)
        println("repackObjectsFolder DONE")
    }
}
