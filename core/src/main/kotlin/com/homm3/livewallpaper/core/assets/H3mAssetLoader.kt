package com.homm3.livewallpaper.core.assets

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.homm3.livewallpaper.core.AssetPaths
import com.homm3.livewallpaper.parser.h3m.H3mMap
import com.homm3.livewallpaper.parser.h3m.H3mReader
import ktx.collections.gdxArrayOf
import ktx.log.logger

class H3mLoaderParams : AssetLoaderParameters<H3mMap>()

class H3mAssetLoader(private val resolver: FileHandleResolver) :
    AsynchronousAssetLoader<H3mMap, H3mLoaderParams>(resolver) {

    @Volatile
    private var map: H3mMap? = null

    override fun getDependencies(
        fileName: String?,
        file: FileHandle?,
        parameter: H3mLoaderParams?
    ): Array<AssetDescriptor<Any>> {
        return gdxArrayOf()
    }

    override fun loadAsync(
        manager: AssetManager?,
        fileName: String?,
        file: FileHandle?,
        parameter: H3mLoaderParams?
    ) {
        try {
            map = loadMap(manager, fileName)
        } catch (e: Exception) {
            log.error { "Failed to load map $fileName: ${e.message}" }
        }
    }

    override fun loadSync(
        manager: AssetManager?,
        fileName: String?,
        file: FileHandle?,
        parameter: H3mLoaderParams?
    ): H3mMap {
        val loadedMap = map
        if (loadedMap != null) {
            if (parameter?.loadedCallback != null) {
                parameter.loadedCallback.finishedLoading(
                    manager,
                    fileName,
                    H3mAssetLoader::class.java
                )
            }
            return loadedMap
        }

        val freshMap = loadMap(manager, fileName)
        map = freshMap
        if (parameter?.loadedCallback != null) {
            parameter.loadedCallback.finishedLoading(
                manager,
                fileName,
                H3mAssetLoader::class.java
            )
        }
        return freshMap
    }

    private fun loadMap(manager: AssetManager?, fileName: String?): H3mMap {
        manager ?: throw IllegalStateException("Asset manager not found")

        val fileHandle = resolver.resolve("${AssetPaths.USER_MAPS_FOLDER}/$fileName")

        try {
            return H3mReader(fileHandle.read()).read()
        } catch (ex: Exception) {
            throw IllegalStateException("Can't read map $fileName", ex)
        }
    }

    companion object {
        private val log = logger<H3mAssetLoader>()
    }
}
