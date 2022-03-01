package com.homm3.livewallpaper.core

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.homm3.livewallpaper.parser.formats.H3m
import com.homm3.livewallpaper.parser.formats.H3mReader
import ktx.collections.gdxArrayOf

class H3mLoaderParams : AssetLoaderParameters<H3m>() {}

class H3mLoader(private val resolver: FileHandleResolver) :
    AsynchronousAssetLoader<H3m, H3mLoaderParams>(resolver) {
    private var map: H3m? = null

    override fun getDependencies(
        fileName: String?,
        file: FileHandle?,
        parameter: H3mLoaderParams?
    ): Array<AssetDescriptor<Any>> {
        return gdxArrayOf()
    }

    private fun load(
        manager: AssetManager?,
        fileName: String?
    ): H3m {
        manager ?: throw Exception("Asset manager not found")

        val fileHandle = resolver.resolve("user-maps/$fileName")

        try {
            return H3mReader(fileHandle.read()).read()
        } catch (ex: Exception) {
            throw Exception("Can't read map $fileName")
        }
    }

    override fun loadSync(
        manager: AssetManager?,
        fileName: String?,
        file: FileHandle?,
        parameter: H3mLoaderParams?
    ): H3m {
        if (map == null) {
            map = load(manager, fileName)

            if (parameter?.loadedCallback != null) {
                parameter.loadedCallback.finishedLoading(
                    manager,
                    fileName,
                    H3mLoader::class.java
                )
            }
        } else if (map is H3m) {
            return map as H3m
        }

        throw Exception("Can't loadSync map $fileName")
    }

    override fun loadAsync(
        manager: AssetManager?,
        fileName: String?,
        file: FileHandle?,
        parameter: H3mLoaderParams?
    ) {
        try {
            map = load(manager, fileName)

            if (parameter?.loadedCallback != null) {
                parameter.loadedCallback.finishedLoading(
                    manager,
                    fileName,
                    H3mLoader::class.java
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}