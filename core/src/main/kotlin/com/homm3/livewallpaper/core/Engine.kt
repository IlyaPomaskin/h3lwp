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
import ktx.log.logger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

open class Engine(
    private val prefs: Flow<WallpaperPreferences>,
    private val onSettingsButtonClick: (onProgress: (String) -> Unit, onDone: () -> Unit) -> Unit = { _, _ -> },
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

    @Volatile
    private var hideFrames: CountDownLatch? = null
    private var isViewSwitchedOnHide = false

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
        addScreen(AssetSetupScreen(ui, onSettingsButtonClick, onConversionDone = { submit(MapCommand.Load) }))
        addScreen(GameScreen(
            currentSpriteSet = { library.currentSpriteSet },
            camera = camera,
            prefs = prefs,
            onSwitchThresholdReached = { submit(MapCommand.Rotate) },
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
        // Fallback when prepareForHide didn't run or timed out. Switching here, not in the queued Refresh,
        // still puts the new view into the first frame after resume.
        if (!isViewSwitchedOnHide && shownScreen is GameScreen) gameScreen.randomizeVisibleMapPart()
        isViewSwitchedOnHide = false
        submit(MapCommand.Refresh)
    }

    /**
     * Called by the wallpaper service on its main thread right before rendering pauses. Switches the view (if due)
     * and blocks until it's drawn into every swap-chain buffer, so the frame left on screen while hidden, and shown
     * again on screen-on before the first new frame, is already the next view. Gives up after [HIDE_TIMEOUT_MS].
     */
    fun prepareForHide() {
        val hideStart = System.currentTimeMillis()
        val renderedFrames = CountDownLatch(FRAMES_BEFORE_HIDE)
        val isSwitchPending = AtomicBoolean(true)
        Gdx.app.postRunnable {
            // After a timeout this runnable only runs once rendering resumes; resume() has switched by then.
            if (!isSwitchPending.compareAndSet(true, false)) return@postRunnable
            if (shownScreen is GameScreen) gameScreen.randomizeVisibleMapPart()
            isViewSwitchedOnHide = true
            hideFrames = renderedFrames
        }
        Gdx.graphics.requestRendering()
        val isRendered = renderedFrames.await(HIDE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        isSwitchPending.set(false)
        val elapsed = System.currentTimeMillis() - hideStart
        val outcome = if (isRendered) "$FRAMES_BEFORE_HIDE frames rendered" else "timed out (limit ${HIDE_TIMEOUT_MS}ms)"
        log.info { "$TIMING_TAG prepareForHide: $outcome in ${elapsed}ms" }
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
        countHideFrame()
    }

    private fun countHideFrame() {
        val frames = hideFrames ?: return
        frames.countDown()
        if (frames.count == 0L) {
            hideFrames = null
            return
        }
        Gdx.graphics.requestRendering()
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
            MapCommand.Rotate -> rotate()
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
        gameScreen.clearMaps()
        library.clear()
        load()
    }

    private suspend fun rotate() {
        val rotateStart = System.currentTimeMillis()
        val isRotated = library.rotateBatch()
        if (!isRotated) return
        gameScreen.setMaps(library.maps)
        log.info { "$TIMING_TAG batch rotation: ${System.currentTimeMillis() - rotateStart}ms" }
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

    companion object {
        private val log = logger<Engine>()
        private const val FRAMES_BEFORE_HIDE = 2
        private const val HIDE_TIMEOUT_MS = 500L
        const val TIMING_TAG = "[timing]"
    }
}
