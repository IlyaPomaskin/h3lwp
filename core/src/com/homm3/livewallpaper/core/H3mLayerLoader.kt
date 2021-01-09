package com.homm3.livewallpaper.core

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.homm3.livewallpaper.parser.formats.H3mReader
import ktx.collections.gdxArrayOf

class H3mLayerLoaderParams : AssetLoaderParameters<H3mLayer>()

class H3mLayerLoader(private val resolver: FileHandleResolver) : SynchronousAssetLoader<H3mLayer, H3mLayerLoaderParams>(resolver) {
    override fun getDependencies(fileName: String?, file: FileHandle?, parameter: H3mLayerLoaderParams?): Array<AssetDescriptor<Any>> {
        return gdxArrayOf()
    }

    override fun load(assetManager: AssetManager?, fileName: String?, file: FileHandle?, parameter: H3mLayerLoaderParams?): H3mLayer {
        if (assetManager == null) {
            throw Exception("Asset manager not found")
        }

        var fileHandle: FileHandle? = null

        if (fileName != null) {
            fileHandle = resolver.resolve(fileName)
        }

        if (file != null) {
            fileHandle = file
        }

        if (fileHandle == null) {
            throw Exception("File not loaded $fileName")
        }

        return H3mLayer(assetManager, H3mReader(fileHandle.read()).read())
    }
}