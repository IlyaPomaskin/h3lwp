package com.homm3.livewallpaper.core

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.homm3.livewallpaper.core.Constants.Companion.BORDER_SIZE
import com.homm3.livewallpaper.core.Constants.Companion.TILE_SIZE
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.DEFAULT_MAP_UPDATE_INTERVAL
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.DEFAULT_SCALE
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.BRIGHTNESS
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.BRIGHTNESS_DEFAULT
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.MAP_UPDATE_INTERVAL
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.PREFERENCES_NAME
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.SCALE
import com.homm3.livewallpaper.parser.formats.H3mReader
import ktx.app.KtxScreen
import ktx.graphics.use
import kotlin.math.ceil
import kotlin.math.min
import kotlin.random.Random

class WallpaperScreen(private val engine: Engine) : KtxScreen {
    private val h3mMap = H3mReader(Gdx.files.internal("maps/invasion.h3m").read()).read()
    val camera = OrthographicCamera().also {
        it.setToOrtho(true)
    }
    private val viewport = ScreenViewport(camera).also {
        it.worldHeight = h3mMap.header.size.toFloat()
        it.worldWidth = h3mMap.header.size.toFloat()
        it.update(Gdx.graphics.width, Gdx.graphics.height)
    }
    private val objectsLayer = ObjectsLayer(engine, h3mMap)
    private val tiledMap = TiledMap().also {
        it.layers.add(TerrainGroupLayer(engine.assets, h3mMap))
        it.layers.add(objectsLayer)
        it.layers.add(BorderLayer(
            engine.assets,
            h3mMap.header.size,
            BORDER_SIZE,
            BORDER_SIZE
        ))
    }
    private val renderer = object : OrthogonalTiledMapRenderer(tiledMap) {
        override fun renderObjects(layer: MapLayer?) {
            if (layer is ObjectsLayer) {
                layer.render(batch)
            }
        }
    }
    private var mapUpdateInterval = DEFAULT_MAP_UPDATE_INTERVAL
    private var lastMapUpdateTime = System.currentTimeMillis()
    private val inputProcessor = InputProcessor(camera).also {
        it.onEnter = ::randomizeCameraPosition
        it.onSpace = { objectsLayer.updateVisibleSprites(camera) }
    }
    private val prefs = Gdx.app.getPreferences(PREFERENCES_NAME)
    private var brightness = BRIGHTNESS_DEFAULT
    private val brightnessOverlay = ShapeRenderer()

    init {
        applyPreferences()
        randomizeCameraPosition()

        if (Gdx.app.type == Application.ApplicationType.Desktop) {
            Gdx.input.inputProcessor = inputProcessor
        }
    }

    private fun applyPreferences() {
        // Old float/integer preferences used in <= 2.2.0
        mapUpdateInterval = kotlin
            .runCatching {
                prefs
                    .getFloat(MAP_UPDATE_INTERVAL)
                    .also { prefs.putString(MAP_UPDATE_INTERVAL, it.toInt().toString()).flush() }
            }
            .recoverCatching { prefs.getString(MAP_UPDATE_INTERVAL).toFloat() }
            .getOrDefault(DEFAULT_MAP_UPDATE_INTERVAL)

        val scale = kotlin
            .runCatching {
                prefs
                    .getInteger(SCALE)
                    .also { prefs.putString(SCALE, it.toString()).flush() }
            }
            .recoverCatching { prefs.getString(SCALE).toInt() }
            .getOrDefault(DEFAULT_SCALE)

        viewport.unitsPerPixel = when (scale) {
            0 -> min(1 / Gdx.graphics.density, 1f)
            else -> 1 / scale.toFloat()
        }
        viewport.update(Gdx.graphics.width, Gdx.graphics.height)

        brightness = prefs.getInteger(BRIGHTNESS, BRIGHTNESS_DEFAULT)
    }

    private fun randomizeCameraPosition() {
        val mapSize = h3mMap.header.size * TILE_SIZE

        val halfWidth = camera.viewportWidth / 2
        val xStart = halfWidth
        val xEnd = mapSize - xStart - halfWidth
        val nextCameraX = xStart + Random.nextFloat() * xEnd

        val halfHeight = camera.viewportHeight / 2
        val yStart = halfHeight
        val yEnd = mapSize - yStart - halfHeight
        val nextCameraY = yStart + Random.nextFloat() * yEnd

        engine.cameraPoint.set(nextCameraX, nextCameraY)
        camera.position.set(engine.cameraPoint, 0f)
        viewport.update(Gdx.graphics.width, Gdx.graphics.height)
        objectsLayer.updateVisibleSprites(camera)
    }

    override fun show() {
        applyPreferences()

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastMapUpdateTime >= mapUpdateInterval) {
            lastMapUpdateTime = currentTime
            randomizeCameraPosition()
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    private fun renderBrightnessOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        brightnessOverlay.projectionMatrix = camera.view
        brightnessOverlay.use(ShapeRenderer.ShapeType.Filled) {
            it.color = Color(0f, 0f, 0f, brightness / 100f)
            it.rect(
                camera.position.x - camera.viewportWidth / 2,
                camera.position.y - camera.viewportHeight / 2,
                camera.viewportWidth,
                camera.viewportHeight
            )
        }
    }

    override fun render(delta: Float) {
        inputProcessor.handlePressedKeys()
        camera.update()
        renderer.setView(camera)
        renderer.render()
        renderBrightnessOverlay()
    }

    override fun dispose() {
        super.dispose()
        renderer.dispose()
    }
}