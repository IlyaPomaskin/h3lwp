package com.homm3.livewallpaper.core

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.homm3.livewallpaper.core.assets.GameDataLocator
import com.homm3.livewallpaper.core.assets.UiAssets
import com.homm3.livewallpaper.core.library.Direction
import com.homm3.livewallpaper.core.library.FolderChange
import com.homm3.livewallpaper.core.library.MapCommand
import com.homm3.livewallpaper.core.library.MapLibrary
import com.homm3.livewallpaper.core.library.NextMapResult
import com.homm3.livewallpaper.core.library.SerialCommandQueue
import com.homm3.livewallpaper.core.render.MapCamera
import com.homm3.livewallpaper.core.render.RenderingDispatcher
import com.homm3.livewallpaper.core.screen.AssetSetupScreen
import com.homm3.livewallpaper.core.screen.GameScreen
import com.homm3.livewallpaper.core.screen.LoadingScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import ktx.app.KtxGame
import ktx.async.KtxAsync

open class Engine(
    private val prefs: Flow<WallpaperPreferences>,
    private val onSettingsButtonClick: (onProgress: (String) -> Unit, onDone: () -> Unit) -> Unit = { _, _ -> },
    private val onHotaButtonClick: (onProgress: (String) -> Unit, onDone: () -> Unit) -> Unit = { _, _ -> },
    private val explicitMaps: List<String> = emptyList(),
    private val headless: Boolean = false
) : KtxGame<Screen>(null, false) {
    private val dataLocator = GameDataLocator()
    private val library = MapLibrary(dataLocator)
    private var uiAssets: UiAssets? = null
    private lateinit var camera: MapCamera
    private var commandScope: CoroutineScope? = null

    @Volatile
    private var commands: SerialCommandQueue<MapCommand>? = null

    private val gameScreen: GameScreen get() = getScreen<GameScreen>()

    fun moveCameraByOffset(offset: Float) {
        camera.moveByScrollOffset(offset)
        Gdx.graphics.requestRendering()
    }

    override fun create() {
        KtxAsync.initiate()
        Gdx.graphics.isContinuousRendering = false

        camera = MapCamera()
        val ui = UiAssets()
        uiAssets = ui

        addScreen(LoadingScreen(ui))
        addScreen(AssetSetupScreen(ui, onSettingsButtonClick, onHotaButtonClick, onConversionDone = { submit(MapCommand.Load) }))
        addScreen(GameScreen(
            currentSpriteSet = { library.currentSpriteSet },
            camera = camera,
            prefs = prefs,
            onSwitchThresholdReached = { submit(MapCommand.Rotate(force = true)) },
        ))

        val scope = CoroutineScope(SupervisorJob() + RenderingDispatcher)
        commandScope = scope
        commands = SerialCommandQueue(scope, ::execute)
        submit(MapCommand.Load)
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        Gdx.graphics.requestRendering()
    }

    override fun <Type : Screen> setScreen(type: Class<Type>) {
        super.setScreen(type)
        Gdx.graphics.requestRendering()
    }

    override fun resume() {
        super.resume()
        submit(MapCommand.Refresh)
    }

    /** Called by the wallpaper service from its own thread. */
    fun onVisibilityChanged(visible: Boolean) {
        if (visible) {
            submit(MapCommand.Refresh)
            return
        }
        // Wallpaper just got hidden — do the batch rotation work while the
        // user isn't looking, so when they come back they see a new map.
        submit(MapCommand.Rotate(force = false))
    }

    override fun render() {
        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            when {
                Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) -> { Gdx.app.exit(); return }
                Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET) -> submit(MapCommand.LoadNext(Direction.FORWARD))
                Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET) -> submit(MapCommand.LoadNext(Direction.BACKWARD))
                Gdx.input.isKeyJustPressed(Input.Keys.U) -> gameScreen.toggleUnderground()
            }
        }
        super.render()
    }

    override fun dispose() {
        commands?.close()
        commandScope?.cancel()
        super.dispose()
        uiAssets?.dispose()
        library.dispose()
    }

    private fun submit(command: MapCommand) {
        commands?.submit(command)
    }

    private suspend fun execute(command: MapCommand) {
        when (command) {
            MapCommand.Load -> load()
            MapCommand.Refresh -> refresh()
            MapCommand.Reinit -> reinit()
            is MapCommand.Rotate -> rotate(command.force)
            is MapCommand.LoadNext -> loadNext(command.direction)
        }
    }

    private suspend fun load() {
        if (library.isLoaded) return
        if (!dataLocator.isLodAvailable()) {
            setScreen<AssetSetupScreen>()
            return
        }
        setScreen<LoadingScreen>()
        library.loadInitialBatch(explicitMaps)
        gameScreen.setMaps(library.maps)
        showGameScreen()
        gameScreen.randomizeVisibleMapPart()
        if (headless) Gdx.app.exit()
    }

    private suspend fun refresh() {
        if (!library.isLoaded) return load()
        if (library.hasHotaAvailabilityChanged) return reinit()

        val folderChange = library.syncWithFolder()
        gameScreen.setMaps(library.maps)
        if (folderChange == FolderChange.UNCHANGED) {
            showGameScreen()
            gameScreen.randomizeVisibleMapPart()
            return
        }

        if (folderChange == FolderChange.CHANGED) {
            setScreen<LoadingScreen>()
            library.loadMissingFromCurrentBatch()
            gameScreen.setMaps(library.maps)
        }
        showGameScreen()
        gameScreen.randomizeVisibleMapPart(force = true)
    }

    private suspend fun reinit() {
        library.clear()
        gameScreen.setMaps(emptyList())
        load()
    }

    private suspend fun rotate(force: Boolean) {
        val isRotated = library.rotateBatchIfDue(force)
        if (!isRotated) return
        gameScreen.setMaps(library.maps)
        gameScreen.randomizeVisibleMapPart(force = true)
    }

    private suspend fun loadNext(direction: Direction) {
        when (val result = library.loadNext(direction)) {
            is NextMapResult.Loaded -> {
                gameScreen.setMaps(library.maps)
                gameScreen.showMap(result.fileName)
            }
            NextMapResult.AllLoaded -> gameScreen.showAdjacentMap(direction)
            NextMapResult.NotLoaded -> Unit
        }
    }

    private fun showGameScreen() {
        if (shownScreen is GameScreen) return
        setScreen<GameScreen>()
    }
}
