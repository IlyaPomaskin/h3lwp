package core.screens

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.homm3.livewallpaper.core.BrightnessOverlay
import com.homm3.livewallpaper.core.Constants
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.DEFAULT_MAP_UPDATE_INTERVAL
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.DEFAULT_SCALE
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.BRIGHTNESS
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.BRIGHTNESS_DEFAULT
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.MAP_UPDATE_INTERVAL
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.MINIMAL_MAP_UPDATE_INTERVAL
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.PREFERENCES_NAME
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.SCALE
import com.homm3.livewallpaper.core.InputProcessor
import core.layers.ObjectsLayer
import core.layers.H3mLayersGroup
import ktx.app.KtxScreen
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameScreen : KtxScreen {
    private val camera = OrthographicCamera().also { it.setToOrtho(true) }
    private val cameraPoint = Vector2()
    private val viewport = ScreenViewport(camera)
    private val tiledMap = TiledMap()
    private val renderer = object : OrthogonalTiledMapRenderer(tiledMap) {
        override fun renderObjects(layer: MapLayer?) {
            if (layer is ObjectsLayer) {
                layer.render(batch)
            }
        }
    }
    private var mapUpdateInterval = DEFAULT_MAP_UPDATE_INTERVAL
    private var lastMapUpdateTime = System.currentTimeMillis()
    private val inputProcessor = InputProcessor(viewport).apply {
        onEnter = { randomizeVisibleMapPart() }
        onSpace = {
            tiledMap
                .layers
                .filter { it.isVisible }
                .filterIsInstance(H3mLayersGroup::class.java)
                .firstOrNull()
                ?.updateVisibleSprites(camera)
        }
    }
    private val prefs = Gdx.app.getPreferences(PREFERENCES_NAME)
    private val brightnessOverlay = BrightnessOverlay(camera)

    init {
        applyPreferences()

        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            Gdx.input.inputProcessor = inputProcessor
        }
    }

    fun addMap(map: H3mLayersGroup) {
        tiledMap.layers.add(map)
    }

    private fun applyPreferences() {
        brightnessOverlay.brightness = kotlin
            .runCatching { prefs.getInteger(BRIGHTNESS, BRIGHTNESS_DEFAULT) }
            .getOrDefault(BRIGHTNESS_DEFAULT)

        mapUpdateInterval = kotlin
            .runCatching { prefs.getString(MAP_UPDATE_INTERVAL).toFloat() }
            .getOrDefault(DEFAULT_MAP_UPDATE_INTERVAL)

        val scale = kotlin
            .runCatching { prefs.getString(SCALE).toInt() }
            .getOrDefault(DEFAULT_SCALE)

        viewport.unitsPerPixel = when (scale) {
            0 -> min(1 / Gdx.graphics.density, 1f)
            else -> 1 / scale.toFloat()
        }
        viewport.update(Gdx.graphics.width, Gdx.graphics.height)
    }

    private fun randomizeCameraPosition(mapSize: Int) {
        val mapSizeFloat = mapSize * Constants.TILE_SIZE

        val halfWidth = camera.viewportWidth / 2
        val xStart = halfWidth
        val xEnd = mapSizeFloat - xStart - halfWidth
        val nextCameraX = xStart + Random.nextFloat() * xEnd

        val halfHeight = camera.viewportHeight / 2
        val yStart = halfHeight
        val yEnd = mapSizeFloat - yStart - halfHeight
        val nextCameraY = yStart + Random.nextFloat() * yEnd

        cameraPoint.set(nextCameraX, nextCameraY)
        camera.position.set(cameraPoint, 0f)
        camera.update()
    }

    fun moveCameraByOffset(offset: Float) {
        camera.position.x = cameraPoint.x + offset * Constants.SCROLL_OFFSET
    }

    private fun randomizeVisibleMapPart() {
        tiledMap
            .layers
            .onEach { it.isVisible = false }
            .toList()
            .filterIsInstance(H3mLayersGroup::class.java)
            .randomOrNull()
            ?.apply {
                randomizeCameraPosition(mapSize)
                updateVisibleSprites(camera)
                isVisible = true;
            }
    }

    private fun shouldUpdateVisibleMapPart(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastMapUpdateTime
        val updateInterval = max(mapUpdateInterval, MINIMAL_MAP_UPDATE_INTERVAL)
        if (timeSinceLastUpdate >= updateInterval) {
            lastMapUpdateTime = currentTime
            return true
        }

        return false
    }

    override fun show() {
        applyPreferences()

        if (shouldUpdateVisibleMapPart()) {
            randomizeVisibleMapPart()
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    override fun render(delta: Float) {
        inputProcessor.handlePressedKeys()
        camera.update()
        renderer.setView(camera)
        renderer.render()
        brightnessOverlay.render()
    }

    override fun dispose() {
        super.dispose()
        renderer.dispose()
    }
}
