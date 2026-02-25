package com.homm3.livewallpaper.core

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.homm3.livewallpaper.core.assets.GameAssets
import com.homm3.livewallpaper.core.map.GameMap
import com.homm3.livewallpaper.parser.h3m.H3mMap
import com.homm3.livewallpaper.core.render.MapCamera
import com.homm3.livewallpaper.core.screen.AssetSetupScreen
import com.homm3.livewallpaper.core.screen.GameScreen
import com.homm3.livewallpaper.core.screen.LoadingScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ktx.app.KtxGame
import ktx.app.clearScreen
import ktx.async.KtxAsync

open class Engine(
    private val prefs: Flow<WallpaperPreferences>,
    private val onSettingsButtonClick: (onProgress: (String) -> Unit, onDone: () -> Unit) -> Unit = { _, _ -> },
    private val onHotaButtonClick: (onProgress: (String) -> Unit, onDone: () -> Unit) -> Unit = { _, _ -> }
) : KtxGame<Screen>(null, false) {
    private var assets: GameAssets? = null
    private lateinit var camera: MapCamera
    private var allMapFiles = emptyList<String>()
    private val loadedMapFiles = mutableSetOf<String>()
    private var allMapsIndex = 0
    private var isLoadingMap = false

    fun moveCameraByOffset(offset: Float) {
        camera.moveByScrollOffset(offset)
    }

    override fun create() {
        KtxAsync.initiate()

        camera = MapCamera()
        val gameAssets = GameAssets()
        assets = gameAssets
        gameAssets.loadUiAssets()

        addScreen(LoadingScreen(gameAssets))
        addScreen(AssetSetupScreen(gameAssets, onSettingsButtonClick, onHotaButtonClick, onConversionDone = ::loadAndStart))
        addScreen(GameScreen(camera, prefs))

        loadAndStart()
    }

    override fun resume() {
        super.resume()

        val a = assets ?: return
        if (a.isGameAssetsLoaded()) {
            refreshMaps()
        } else {
            loadAndStart()
        }
    }

    fun onVisibilityChanged(visible: Boolean) {
        val a = assets ?: return
        if (visible && a.isGameAssetsLoaded()) {
            Gdx.app.postRunnable { refreshMaps() }
        }
    }

    override fun render() {
        clearScreen(0f,0f,0f,0f)
        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            when {
                Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) -> { Gdx.app.exit(); return }
                Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET) -> loadNextMap(1)
                Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET) -> loadNextMap(-1)
            }
        }
        super.render()
    }

    override fun dispose() {
        super.dispose()
        assets?.dispose()
    }

    private fun loadAndStart() {
        val a = assets ?: return
        if (!a.isLodAvailable()) {
            setScreen<AssetSetupScreen>()
            return
        }
        setScreen<LoadingScreen>()
        KtxAsync.launch {
            allMapFiles = a.getAllMapFiles()
            val result = a.loadGameAssets()
            loadedMapFiles.addAll(result.loadedFileNames)
            result.maps.zip(result.loadedFileNames).forEach { (map, name) ->
                getScreen<GameScreen>().addMap(GameMap(a, map, name))
            }
            setScreen<GameScreen>()
        }
    }

    private fun refreshMaps() {
        val a = assets ?: return
        val currentFiles = a.getAllMapFiles()
        val currentFileSet = currentFiles.toSet()

        // Remove deleted maps
        val removedFiles = loadedMapFiles.filter { it !in currentFileSet }.toSet()
        if (removedFiles.isNotEmpty()) {
            getScreen<GameScreen>().removeMaps(removedFiles)
            loadedMapFiles.removeAll(removedFiles)
        }

        val previousFiles = allMapFiles.toSet()
        allMapFiles = currentFiles

        if (currentFileSet == previousFiles && removedFiles.isEmpty()) {
            // No changes
            setScreen<GameScreen>()
            getScreen<GameScreen>().randomizeVisibleMapPart()
            return
        }

        if (currentFiles.isEmpty()) {
            setScreen<GameScreen>()
            getScreen<GameScreen>().randomizeVisibleMapPart(force = true)
            return
        }

        // Files changed — rebuild queue and load new maps
        setScreen<LoadingScreen>()
        KtxAsync.launch {
            val result = a.loadGameAssets()
            result.maps.zip(result.loadedFileNames)
                .filter { (_, name) -> name !in loadedMapFiles }
                .forEach { (map, name) ->
                    loadedMapFiles.add(name)
                    getScreen<GameScreen>().addMap(GameMap(a, map, name))
                }
            setScreen<GameScreen>()
            getScreen<GameScreen>().randomizeVisibleMapPart(force = true)
        }
    }

    private fun loadNextMap(direction: Int) {
        if (isLoadingMap || allMapFiles.isEmpty()) return
        val a = assets ?: return

        // Find next unloaded map in the given direction
        var fileName: String? = null
        for (i in 1..allMapFiles.size) {
            val idx = (allMapsIndex + direction * i + allMapFiles.size * i) % allMapFiles.size
            if (allMapFiles[idx] !in loadedMapFiles) {
                fileName = allMapFiles[idx]
                allMapsIndex = idx
                break
            }
        }

        if (fileName == null) {
            // All maps already loaded, just cycle through them
            if (direction > 0) getScreen<GameScreen>().showNextMap()
            else getScreen<GameScreen>().showPreviousMap()
            return
        }

        isLoadingMap = true
        KtxAsync.launch {
            try {
                val map = a.loadMapFile(fileName)
                a.loadSpritesForMaps(listOf(map))
                loadedMapFiles.add(fileName)
                getScreen<GameScreen>().addMap(GameMap(a, map, fileName))
                getScreen<GameScreen>().showLastMap()
            } catch (e: Throwable) {
                ktx.log.logger<Engine>().error(e) { "Failed to load map $fileName" }
            } finally {
                isLoadingMap = false
            }
        }
    }
}
