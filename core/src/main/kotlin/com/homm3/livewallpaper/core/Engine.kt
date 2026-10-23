package com.homm3.livewallpaper.core

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.homm3.livewallpaper.core.assets.GameDataLocator
import com.homm3.livewallpaper.core.assets.LoadedMap
import com.homm3.livewallpaper.core.assets.MapBatch
import com.homm3.livewallpaper.core.assets.MapBatchLoader
import com.homm3.livewallpaper.core.assets.SpriteSet
import com.homm3.livewallpaper.core.assets.UiAssets
import com.homm3.livewallpaper.core.map.GameMap
import com.homm3.livewallpaper.core.render.MapCamera
import com.homm3.livewallpaper.core.screen.AssetSetupScreen
import com.homm3.livewallpaper.core.screen.GameScreen
import com.homm3.livewallpaper.core.screen.LoadingScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ktx.app.KtxGame
import ktx.app.clearScreen
import ktx.async.KtxAsync
import ktx.log.logger

open class Engine(
    private val prefs: Flow<WallpaperPreferences>,
    private val onSettingsButtonClick: (onProgress: (String) -> Unit, onDone: () -> Unit) -> Unit = { _, _ -> },
    private val onHotaButtonClick: (onProgress: (String) -> Unit, onDone: () -> Unit) -> Unit = { _, _ -> },
    private val explicitMaps: List<String> = emptyList(),
    private val headless: Boolean = false
) : KtxGame<Screen>(null, false) {
    private var uiAssets: UiAssets? = null
    private val dataLocator = GameDataLocator()
    private val batchLoader = MapBatchLoader(dataLocator)
    private var spriteSet: SpriteSet? = null
    private lateinit var camera: MapCamera
    private var allMapFiles = emptyList<String>()
    private val loadedMapFiles = mutableSetOf<String>()
    private var allMapsIndex = 0
    private var isLoadingMap = false
    private var lastHotaAvailable: Boolean? = null
    private var rotationJob: Job? = null
    private var loadJob: Job? = null

    fun moveCameraByOffset(offset: Float) {
        camera.moveByScrollOffset(offset)
    }

    override fun create() {
        KtxAsync.initiate()

        camera = MapCamera()
        val ui = UiAssets()
        uiAssets = ui

        addScreen(LoadingScreen(ui))
        addScreen(AssetSetupScreen(ui, onSettingsButtonClick, onHotaButtonClick, onConversionDone = ::loadAndStart))
        addScreen(GameScreen(
            currentSpriteSet = { spriteSet },
            camera = camera,
            prefs = prefs,
            onSwitchThresholdReached = { rotateBatchIfDue(force = true) },
        ))

        loadAndStart()
    }

    override fun resume() {
        super.resume()

        if (uiAssets == null) return
        if (isGameAssetsLoaded()) {
            if (dataLocator.isHotaAvailable() != lastHotaAvailable) reinitAssets()
            else refreshMaps()
        } else {
            loadAndStart()
        }
    }

    fun onVisibilityChanged(visible: Boolean) {
        if (!isGameAssetsLoaded()) return
        if (visible) {
            Gdx.app.postRunnable {
                if (dataLocator.isHotaAvailable() != lastHotaAvailable) reinitAssets()
                else refreshMaps()
            }
        } else {
            // Wallpaper just got hidden — do the batch rotation work while the
            // user isn't looking, so when they come back they see a new map.
            Gdx.app.postRunnable { rotateBatchIfDue() }
        }
    }

    private fun isGameAssetsLoaded(): Boolean = spriteSet != null

    private fun rotateBatchIfDue(force: Boolean = false) {
        if (!isGameAssetsLoaded()) return
        if (rotationJob?.isActive == true) return  // already rotating
        rotationJob = KtxAsync.launch {
            val availableFiles = dataLocator.findMapFiles()
            val batch = batchLoader.rotateBatchIfDue(availableFiles, force) ?: return@launch
            if (batch.maps.isEmpty()) {
                log.error { "rotateBatchIfDue: no map of the new batch loaded; keeping the current batch" }
                batch.spriteSet.dispose()
                return@launch
            }
            showBatch(batch)
            getScreen<GameScreen>().randomizeVisibleMapPart(force = true)
        }
    }

    /** Replaces every shown map with [batch]'s maps and swaps in its sprite set, disposing the previous one. */
    private fun showBatch(batch: MapBatch) {
        val previousSpriteSet = spriteSet
        getScreen<GameScreen>().removeMaps(loadedMapFiles.toSet())
        loadedMapFiles.clear()
        spriteSet = batch.spriteSet
        addMaps(batch.spriteSet, batch.maps)
        previousSpriteSet?.dispose()
    }

    private fun addMaps(mapSpriteSet: SpriteSet, maps: List<LoadedMap>) {
        val gameScreen = getScreen<GameScreen>()
        maps.filter { it.fileName !in loadedMapFiles }.forEach { (fileName, h3m) ->
            loadedMapFiles.add(fileName)
            gameScreen.addMap(GameMap(mapSpriteSet, h3m, fileName))
            if (h3m.header.hasUnderground) {
                gameScreen.addMap(GameMap(mapSpriteSet, h3m, fileName, isUnderground = true))
            }
        }
    }

    private fun reinitAssets() {
        getScreen<GameScreen>().removeMaps(loadedMapFiles.toSet())
        loadedMapFiles.clear()
        spriteSet?.dispose()
        spriteSet = null
        loadAndStart()
    }

    override fun render() {
        clearScreen(0f,0f,0f,0f)
        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            when {
                Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) -> { Gdx.app.exit(); return }
                Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET) -> loadNextMap(1)
                Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET) -> loadNextMap(-1)
                Gdx.input.isKeyJustPressed(Input.Keys.U) -> getScreen<GameScreen>().toggleUnderground()
            }
        }
        super.render()
    }

    override fun dispose() {
        super.dispose()
        uiAssets?.dispose()
        spriteSet?.dispose()
        spriteSet = null
    }

    private fun loadAndStart() {
        if (loadJob?.isActive == true) return
        if (!dataLocator.isLodAvailable()) {
            setScreen<AssetSetupScreen>()
            return
        }
        setScreen<LoadingScreen>()
        loadJob = KtxAsync.launch {
            allMapFiles = dataLocator.findMapFiles()
            showBatch(batchLoader.loadInitialBatch(explicitMaps, allMapFiles))
            lastHotaAvailable = dataLocator.isHotaAvailable()
            setScreen<GameScreen>()
            if (headless) Gdx.app.exit()
        }
    }

    private fun refreshMaps() {
        KtxAsync.launch {
            val currentFiles = dataLocator.findMapFiles()
            val currentSpriteSet = spriteSet ?: return@launch
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
                return@launch
            }

            if (currentFiles.isEmpty()) {
                setScreen<GameScreen>()
                getScreen<GameScreen>().randomizeVisibleMapPart(force = true)
                return@launch
            }

            // Files changed — rebuild queue and load new maps
            setScreen<LoadingScreen>()
            val newMaps = batchLoader.loadCurrentBatchInto(
                spriteSet = currentSpriteSet,
                availableFiles = currentFiles,
                excluded = loadedMapFiles.toSet(),
            )
            val isSpriteSetStillCurrent = currentSpriteSet === spriteSet
            if (isSpriteSetStillCurrent) addMaps(currentSpriteSet, newMaps)
            lastHotaAvailable = dataLocator.isHotaAvailable()
            setScreen<GameScreen>()
            getScreen<GameScreen>().randomizeVisibleMapPart(force = true)
        }
    }

    private fun loadNextMap(direction: Int) {
        if (isLoadingMap || allMapFiles.isEmpty()) return
        val currentSpriteSet = spriteSet ?: return

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
                val maps = batchLoader.loadMapsInto(currentSpriteSet, listOf(fileName))
                val isSpriteSetStillCurrent = currentSpriteSet === spriteSet
                if (maps.isEmpty() || !isSpriteSetStillCurrent) return@launch
                addMaps(currentSpriteSet, maps)
                getScreen<GameScreen>().showLastMap()
            } catch (e: Throwable) {
                log.error(e) { "Failed to load map $fileName" }
            } finally {
                isLoadingMap = false
            }
        }
    }

    companion object {
        private val log = logger<Engine>()
    }
}
