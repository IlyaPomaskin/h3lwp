package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
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
        fileName: String?,
        file: FileHandle?,
        parameter: H3mLoaderParams?
    ): H3m {
        manager ?: throw Exception("Asset manager not found")

        var fileHandle: FileHandle? = file
        fileHandle = fileHandle ?: resolver.resolve(fileName)
        fileHandle ?: throw Exception("File not loaded $fileName")

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
        val filename = fileName ?: file?.name() ?: throw Exception("file not loaded")

        Gdx.app.log("h3mLoader", "load sync $filename")

        if (map == null) {
            Gdx.app.log("h3mLoader", "layer null")
            map = load(manager, fileName, file, parameter)
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
        val filename = fileName ?: file?.name() ?: throw Exception("file not loaded")

        Gdx.app.log("h3mLoader", "load async $filename start")

        try {
            map = load(manager, fileName, file, parameter)
        } catch (e: Exception) {
            Gdx.app.log("h3mLoader", "Failed to load $filename ")
            Gdx.app.log("h3mLoader", e.message)
            e.printStackTrace()
        }

        Gdx.app.log("h3mLayer", "load async $filename done")
    }
}