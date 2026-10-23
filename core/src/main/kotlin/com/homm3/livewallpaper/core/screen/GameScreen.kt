package com.homm3.livewallpaper.core.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.homm3.livewallpaper.core.GameConfig
import com.homm3.livewallpaper.core.MapUpdateInterval
import com.homm3.livewallpaper.core.Scale
import com.homm3.livewallpaper.core.WallpaperPreferences
import com.homm3.livewallpaper.core.assets.SpriteSet
import com.homm3.livewallpaper.core.input.CameraInputProcessor
import com.homm3.livewallpaper.core.library.Direction
import com.homm3.livewallpaper.core.map.GameMap
import com.homm3.livewallpaper.core.render.DebugCapture
import com.homm3.livewallpaper.core.render.MapCamera
import com.homm3.livewallpaper.core.render.PaletteSpriteBatch
import com.homm3.livewallpaper.core.render.PaletteTiledMapRenderer
import com.homm3.livewallpaper.core.render.RenderStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.async.KtxAsync
import ktx.log.logger
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameScreen(
    private val currentSpriteSet: () -> SpriteSet?,
    private val camera: MapCamera,
    private val prefs: Flow<WallpaperPreferences>,
    private val onSwitchThresholdReached: () -> Unit = {},
) : KtxScreen {

    private val viewport = ScreenViewport(camera)
    private val spriteBatch = PaletteSpriteBatch(size = 1000) { currentSpriteSet()?.paletteTexture() }
    private val tiledMap = TiledMap()
    private val renderer = PaletteTiledMapRenderer(tiledMap, spriteBatch)
    private val inputProcessor = CameraInputProcessor(viewport).apply {
        onEnter = { randomizeVisibleMapPart(force = true) }
    }
    private val renderStats = RenderStats()
    private val debugCapture = DebugCapture()
    private var mapUpdateInterval = 0f
    private var lastMapUpdateTime = 0L
    private var currentMapIndex = 0
    private var prefsJob: Job? = null
    private var mapSwitchCount = 0

    /** Replaces the shown maps; keeps showing the current one if it's still among [maps]. */
    fun setMaps(maps: List<GameMap>) {
        val current = tiledMap.layers.toList().filterIsInstance<GameMap>().getOrNull(currentMapIndex)
        tiledMap.layers.toList().forEach { tiledMap.layers.remove(it) }
        maps.forEach { tiledMap.layers.add(it) }
        maps.forEach { it.isVisible = it === current }
        currentMapIndex = maps.indexOf(current).coerceAtLeast(0)
    }

    fun showMap(fileName: String) {
        val maps = tiledMap.layers.toList().filterIsInstance<GameMap>()
        val index = maps.indexOfFirst { it.fileName == fileName && !it.isUnderground }
        if (index >= 0) showMapAtIndex(index)
    }

    fun showAdjacentMap(direction: Direction) {
        if (direction == Direction.FORWARD) showNextMap() else showPreviousMap()
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
        prefsJob = KtxAsync.launch {
            prefs.collect {
                renderer.brightness = it.brightness
                setUnitsPerPixelByScale(it.scale)
                setMapUpdateInterval(it.mapUpdateInterval)
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    override fun render(delta: Float) {
        clearScreen(0f, 0f, 0f, 1f)
        inputProcessor.handlePressedKeys()
        currentSpriteSet()?.updatePalettes(delta)
        camera.update()
        renderer.setView(camera)
        val renderStart = System.nanoTime()
        renderer.render()
        renderStats.record(spriteBatch.renderCalls, System.nanoTime() - renderStart)
        debugCapture.onFrameRendered(hasMap = tiledMap.layers.size() > 0)
    }

    override fun hide() {
        prefsJob?.cancel()
    }

    override fun dispose() {
        prefsJob?.cancel()
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
        if (!shouldUpdateVisibleMapPart(force)) {
            return
        }

        val maps = tiledMap.layers.toList().filterIsInstance(GameMap::class.java)
        if (maps.isEmpty()) return

        mapSwitchCount++
        log.info { "map switch #$mapSwitchCount/$MAX_SWITCHES_BEFORE_BATCH_ROTATE (force=$force)" }
        if (mapSwitchCount >= MAX_SWITCHES_BEFORE_BATCH_ROTATE) {
            mapSwitchCount = 0
            onSwitchThresholdReached()
        }

        val originalIndex = currentMapIndex
        for (attempt in 0 until MAX_REROLLS) {
            val idx = randomIndexExcluding(maps.size, originalIndex)
            showMapAtIndex(idx)   // sets currentMapIndex, rerolls camera, flips visibility
            val map = maps[idx]
            val coverage = map.objectCoverage(
                camera.position.x, camera.position.y,
                camera.viewportWidth, camera.viewportHeight
            )
            log.info {
                "Reroll ${attempt + 1}/$MAX_REROLLS: map='${map.fileName}' " +
                    "(underground=${map.isUnderground}) coverage=${"%.2f".format(coverage)}"
            }
            if (coverage >= MIN_OBJECT_COVERAGE) return
        }
        log.info { "All $MAX_REROLLS attempts below coverage threshold; showing last view" }
    }

    /** Uniform random index in `[0, size)` excluding [exclude]. Returns 0 if `size <= 1`. */
    private fun randomIndexExcluding(size: Int, exclude: Int): Int {
        if (size <= 1) return 0
        val next = Random.nextInt(size - 1)
        return if (next < exclude) next else next + 1
    }

    fun showNextMap() {
        val maps = tiledMap.layers.toList().filterIsInstance(GameMap::class.java)
        if (maps.isEmpty()) return
        currentMapIndex = (currentMapIndex + 1) % maps.size
        showMapAtIndex(currentMapIndex)
    }

    fun showPreviousMap() {
        val maps = tiledMap.layers.toList().filterIsInstance(GameMap::class.java)
        if (maps.isEmpty()) return
        currentMapIndex = (currentMapIndex - 1 + maps.size) % maps.size
        showMapAtIndex(currentMapIndex)
    }

    fun showLastMap() {
        val maps = tiledMap.layers.toList().filterIsInstance(GameMap::class.java)
        if (maps.isEmpty()) return
        showMapAtIndex(maps.size - 1)
    }

    fun toggleUnderground() {
        val maps = tiledMap.layers.toList().filterIsInstance(GameMap::class.java)
        if (maps.isEmpty()) return
        val current = maps[currentMapIndex]
        val counterpart = maps.indexOfFirst {
            it.fileName == current.fileName && it.isUnderground != current.isUnderground
        }
        if (counterpart >= 0) {
            showMapAtIndex(counterpart)
        }
    }

    companion object {
        private val log = logger<GameScreen>()
        private const val MIN_OBJECT_COVERAGE = 0.8f
        private const val MAX_REROLLS = 20
        private const val MAX_SWITCHES_BEFORE_BATCH_ROTATE = 20
    }

    private fun showPinnedView() {
        showMapAtIndex(0)
        val (x, y) = debugCapture.pinnedCamera ?: return
        camera.position.set(x, y, 0f)
        camera.update()
    }

    private fun showMapAtIndex(index: Int) {
        val maps = tiledMap.layers.toList().filterIsInstance(GameMap::class.java)
        if (maps.isEmpty()) return
        currentMapIndex = min(index, maps.size - 1)

        tiledMap.layers.forEach { it.isVisible = false }
        val mapLayer = maps[currentMapIndex]
        camera.randomizePosition(mapLayer.mapSize)
        mapLayer.isVisible = true
    }
}
