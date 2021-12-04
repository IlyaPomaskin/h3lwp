package com.homm3.livewallpaper.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.homm3.livewallpaper.parser.formats.H3mReader
import ktx.collections.gdxArrayOf
import kotlin.concurrent.thread

class H3mLayerLoaderParams : AssetLoaderParameters<H3mLayer>() {

}

class H3mLayerLoader(private val resolver: FileHandleResolver) :
    AsynchronousAssetLoader<H3mLayer, H3mLayerLoaderParams>(resolver) {
    private var layer: H3mLayer? = null

    override fun getDependencies(
        fileName: String?,
        file: FileHandle?,
        parameter: H3mLayerLoaderParams?
    ): Array<AssetDescriptor<Any>> {
        return gdxArrayOf()
    }

    private fun load(
        manager: AssetManager?,
        fileName: String?,
        file: FileHandle?,
        parameter: H3mLayerLoaderParams?
    ): H3mLayer {
        manager ?: throw Exception("Asset manager not found")

        var fileHandle: FileHandle? = file
        fileHandle = fileHandle ?: resolver.resolve(fileName)
        fileHandle ?: throw Exception("File not loaded $fileName")

        return H3mLayer(manager, H3mReader(fileHandle.read()).read())
    }

    override fun loadSync(
        manager: AssetManager?,
        fileName: String?,
        file: FileHandle?,
        parameter: H3mLayerLoaderParams?
    ): H3mLayer {
        val filename = fileName ?: file?.name() ?: "unknown"

        Gdx.app.log("h3mLayer", "load sync $filename")

        if (layer == null) {
            Gdx.app.log("h3mLayer", "layer null")
            layer = load(manager, fileName, file, parameter)
        }

        return layer as H3mLayer
    }

    override fun loadAsync(
        manager: AssetManager?,
        fileName: String?,
        file: FileHandle?,
        parameter: H3mLayerLoaderParams?
    ) {
        val filename = fileName ?: file?.name() ?: "unknown"
        Gdx.app.log("h3mLayer", "load async $filename start")
        try {
            layer = load(manager, fileName, file, parameter)

            parameter?.loadedCallback?.finishedLoading(manager, fileName, H3mLayer::class.java)
        } catch (e: Exception) {
            Gdx.app.log("h3mLayer", "Failed to load $filename ")
            Gdx.app.log("h3mLayer", e.message)
            e.printStackTrace()
        }
        Gdx.app.log("h3mLayer", "load async $filename done")
    }
}