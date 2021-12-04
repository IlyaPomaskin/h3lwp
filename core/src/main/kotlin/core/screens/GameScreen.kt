package core.screens

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.homm3.livewallpaper.core.BrightnessOverlay
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.MINIMAL_MAP_UPDATE_INTERVAL
import com.homm3.livewallpaper.core.InputProcessor
import core.Camera
import core.PreferenceService
import core.layers.ObjectsLayer
import core.layers.H3mLayersGroup
import ktx.app.KtxScreen
import kotlin.math.max
import kotlin.math.min

class GameScreen(private val camera: Camera) : KtxScreen {
    private val viewport = ScreenViewport(camera)
    private val tiledMap = TiledMap()
    private val renderer = object : OrthogonalTiledMapRenderer(tiledMap) {
        override fun renderObjects(layer: MapLayer?) {
            if (layer is ObjectsLayer) {
                layer.render(batch)
            }
        }
    }
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
    private val brightnessOverlay = BrightnessOverlay(camera)
    private val prefs = PreferenceService()

    private var lastMapUpdateTime = Long.MAX_VALUE

    init {
        applyPreferences()

        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            Gdx.input.inputProcessor = inputProcessor
        }
    }

    fun addMap(map: H3mLayersGroup) {
        tiledMap.layers.add(map)

        if (tiledMap.layers.size() == 1) {
            randomizeVisibleMapPart()
        }
    }

    private fun applyPreferences() {
        brightnessOverlay.brightness = prefs.brightness

        viewport.unitsPerPixel = when (prefs.scale) {
            0 -> min(1 / Gdx.graphics.density, 1f)
            else -> 1 / prefs.scale.toFloat()
        }

        viewport.update(Gdx.graphics.width, Gdx.graphics.height)
    }

    private fun randomizeVisibleMapPart() {
        val h3mLayer = tiledMap
            .layers
            .onEach { it.isVisible = false }
            .toList()
            .filterIsInstance(H3mLayersGroup::class.java)
            .randomOrNull()

        if (h3mLayer != null) {
            camera.randomizeCameraPosition(h3mLayer.mapSize)
            h3mLayer.updateVisibleSprites(camera)
            h3mLayer.isVisible = true;
        }
    }

    private fun shouldUpdateVisibleMapPart(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastMapUpdateTime
        val updateInterval = max(prefs.mapUpdateInterval, MINIMAL_MAP_UPDATE_INTERVAL)
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
