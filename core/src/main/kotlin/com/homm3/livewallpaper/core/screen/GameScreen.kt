package com.homm3.livewallpaper.core.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.homm3.livewallpaper.core.Engine
import com.homm3.livewallpaper.core.GameConfig
import com.homm3.livewallpaper.core.MapUpdateInterval
import com.homm3.livewallpaper.core.Scale
import com.homm3.livewallpaper.core.WallpaperPreferences
import com.homm3.livewallpaper.core.input.CameraInputProcessor
import com.homm3.livewallpaper.core.library.Direction
import com.homm3.livewallpaper.core.map.GameMap
import com.homm3.livewallpaper.core.render.AnimationTicker
import com.homm3.livewallpaper.core.render.DebugCapture
import com.homm3.livewallpaper.core.render.MapCamera
import com.homm3.livewallpaper.core.render.PaletteSpriteBatch
import com.homm3.livewallpaper.core.render.PaletteTiledMapRenderer
import com.homm3.livewallpaper.core.render.RenderStats
import com.homm3.livewallpaper.core.render.RenderingDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.async.KtxAsync
import ktx.log.logger
import kotlin.math.max
import kotlin.random.Random

/** Shows one [GameMap] at a time out of the maps handed to [setMaps]. */
class GameScreen(
    private val camera: MapCamera,
    private val prefs: Flow<WallpaperPreferences>,
    private val onSwitchThresholdReached: () -> Unit = {},
) : KtxScreen {

    private val viewport = ScreenViewport(camera)
    private val spriteBatch = PaletteSpriteBatch(size = 1000) { currentMap?.spriteSet?.paletteTexture() }
    private val emptyTiledMap = TiledMap()
    private val renderer = PaletteTiledMapRenderer(emptyTiledMap, spriteBatch)
    private val animationTicker = AnimationTicker()
    private val inputProcessor = CameraInputProcessor(viewport).apply {
        onEnter = { randomizeVisibleMapPart(force = true) }
    }
    private val renderStats = RenderStats()
    private val debugCapture = DebugCapture()
    private var mapUpdateInterval = 0f
    private var lastMapUpdateTime = 0L
    private var maps = emptyList<GameMap>()
    private var currentMap: GameMap? = null
    private var prefsJob: Job? = null
    private var mapSwitchCount = 0

    /**
     * Replaces the maps to pick from. Keeps showing the current map until the next switch even when it isn't
     * among [maps], so its sprite set must stay alive until then.
     */
    fun setMaps(maps: List<GameMap>) {
        this.maps = maps
    }

    /** Drops all maps and stops showing the current one, e.g. before its sprite set is disposed. */
    fun clearMaps() {
        maps = emptyList()
        currentMap = null
        renderer.map = emptyTiledMap
    }

    /** Shows the surface of [fileName] from a random camera position. */
    fun showMap(fileName: String) {
        val map = maps.firstOrNull { it.fileName == fileName && !it.isUnderground } ?: return
        showFromRandomPosition(map)
    }

    fun showAdjacentMap(direction: Direction) {
        if (maps.isEmpty()) return
        val fromIndex = maps.indexOf(currentMap).coerceAtLeast(0)
        showFromRandomPosition(maps[Math.floorMod(fromIndex + direction.step, maps.size)])
    }

    fun toggleUnderground() {
        val current = currentMap ?: return
        val counterpart = maps.firstOrNull {
            it.fileName == current.fileName && it.isUnderground != current.isUnderground
        } ?: return
        showFromRandomPosition(counterpart)
    }

    init {
        if (Gdx.app.type == com.badlogic.gdx.Application.ApplicationType.Desktop) {
            Gdx.input.inputProcessor = inputProcessor
        }
    }

    override fun show() {
        if (Gdx.app.type == com.badlogic.gdx.Application.ApplicationType.Desktop) {
            Gdx.input.inputProcessor = inputProcessor
        }
        prefsJob = KtxAsync.launch(RenderingDispatcher, start = CoroutineStart.UNDISPATCHED) {
            prefs.collect {
                renderer.brightness = it.brightness
                setUnitsPerPixelByScale(it.scale)
                setMapUpdateInterval(it.mapUpdateInterval)
                Gdx.graphics.requestRendering()
            }
        }
        animationTicker.start()
    }

    override fun pause() {
        animationTicker.stop()
    }

    override fun resume() {
        animationTicker.start()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    override fun render(delta: Float) {
        clearScreen(0f, 0f, 0f, 1f)
        inputProcessor.handlePressedKeys()
        val map = currentMap ?: return
        map.spriteSet.updatePalettes(delta)
        camera.update()
        renderer.setView(camera)
        val renderStart = System.nanoTime()
        renderer.render()
        renderStats.record(spriteBatch.renderCalls, System.nanoTime() - renderStart)
        debugCapture.onFrameRendered(hasMap = true)
    }

    override fun hide() {
        prefsJob?.cancel()
        animationTicker.stop()
    }

    override fun dispose() {
        prefsJob?.cancel()
        animationTicker.dispose()
        renderer.dispose()
        spriteBatch.dispose()
    }

    private fun setUnitsPerPixelByScale(scale: Scale) {
        val nextUnitsPerPixel = when (scale) {
            Scale.X1 -> 1f
            Scale.X2 -> 0.5f
            Scale.X3 -> 0.33f
            Scale.X4 -> 0.25f
            Scale.X5 -> 0.2f
        }

        if (viewport.unitsPerPixel != nextUnitsPerPixel) {
            viewport.unitsPerPixel = nextUnitsPerPixel
            viewport.update(Gdx.graphics.width, Gdx.graphics.height)
            randomizeVisibleMapPart(force = true)
        }
    }

    private fun setMapUpdateInterval(interval: MapUpdateInterval) {
        mapUpdateInterval = when (interval) {
            MapUpdateInterval.EVERY_SWITCH -> 1000f
            MapUpdateInterval.HOURS_2 -> 2f * 60f * 60f * 1000f
            MapUpdateInterval.HOURS_24 -> 24f * 60f * 60f * 1000f
            MapUpdateInterval.MINUTES_10 -> 10f * 60f * 1000f
            MapUpdateInterval.MINUTES_30 -> 30f * 60f * 1000f
        }
    }

    private fun shouldUpdateVisibleMapPart(force: Boolean): Boolean {
        val isInitialRender = lastMapUpdateTime == 0L

        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastMapUpdateTime
        val updateInterval = max(mapUpdateInterval, GameConfig.MIN_MAP_UPDATE_INTERVAL_MS)
        val isTimeToUpdate = timeSinceLastUpdate >= updateInterval

        if (force || isInitialRender || isTimeToUpdate) {
            lastMapUpdateTime = currentTime
            return true
        }

        return false
    }

    fun randomizeVisibleMapPart(force: Boolean = false) {
        if (debugCapture.isEnabled) {
            showPinnedView()
            return
        }
        if (maps.isEmpty()) return
        val isNothingShown = currentMap == null
        if (!shouldUpdateVisibleMapPart(force || isNothingShown)) return

        mapSwitchCount++
        log.info { "map switch #$mapSwitchCount/$MAX_SWITCHES_BEFORE_BATCH_ROTATE (force=$force)" }
        if (mapSwitchCount >= MAX_SWITCHES_BEFORE_BATCH_ROTATE) {
            mapSwitchCount = 0
            onSwitchThresholdReached()
        }

        val rerollStart = System.currentTimeMillis()
        val nextMap = rollWellCoveredView(excludedIndex = maps.indexOf(currentMap))
        val showStart = System.currentTimeMillis()
        show(nextMap)
        log.info {
            "${Engine.TIMING_TAG} map switch to '${nextMap.fileName}': reroll ${showStart - rerollStart}ms, " +
                "show ${System.currentTimeMillis() - showStart}ms"
        }
    }

    /** Rerolls map and camera position until objects cover enough of the view; leaves the camera on the result. */
    private fun rollWellCoveredView(excludedIndex: Int): GameMap {
        val candidates = List(MAX_REROLLS) { maps[randomIndexExcluding(maps.size, excludedIndex)] }
        for ((attempt, candidate) in candidates.withIndex()) {
            camera.randomizePosition(candidate.mapSize)
            val coverage = candidate.objectCoverage(
                camera.position.x, camera.position.y,
                camera.viewportWidth, camera.viewportHeight
            )
            log.info {
                "Reroll ${attempt + 1}/$MAX_REROLLS: map='${candidate.fileName}' " +
                    "(underground=${candidate.isUnderground}) coverage=${"%.2f".format(coverage)}"
            }
            if (coverage >= MIN_OBJECT_COVERAGE) return candidate
        }
        log.info { "All $MAX_REROLLS attempts below coverage threshold; showing last view" }
        return candidates.last()
    }

    /** Uniform random index in `[0, size)` other than [exclude] when that is a valid index. Returns 0 if `size <= 1`. */
    private fun randomIndexExcluding(size: Int, exclude: Int): Int {
        if (size <= 1) return 0
        val isExcludedIndexValid = exclude in 0 until size
        if (!isExcludedIndexValid) return Random.nextInt(size)
        val next = Random.nextInt(size - 1)
        return if (next < exclude) next else next + 1
    }

    private fun showPinnedView() {
        val firstMap = maps.firstOrNull() ?: return
        showFromRandomPosition(firstMap)
        val (x, y) = debugCapture.pinnedCamera ?: return
        camera.position.set(x, y, 0f)
        camera.update()
    }

    private fun showFromRandomPosition(map: GameMap) {
        camera.randomizePosition(map.mapSize)
        show(map)
    }

    private fun show(map: GameMap) {
        currentMap = map
        renderer.map = map.tiledMap
        Gdx.graphics.requestRendering()
    }

    companion object {
        private val log = logger<GameScreen>()
        private const val MIN_OBJECT_COVERAGE = 0.8f
        private const val MAX_REROLLS = 20
        private const val MAX_SWITCHES_BEFORE_BATCH_ROTATE = 20
    }
}
