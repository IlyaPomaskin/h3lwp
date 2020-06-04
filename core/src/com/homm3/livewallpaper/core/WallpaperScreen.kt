package com.homm3.livewallpaper.core

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.homm3.livewallpaper.core.Constants.Companion.TILE_SIZE
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.DEFAULT_MAP_UPDATE_INTERVAL
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.DEFAULT_SCALE
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.MAP_UPDATE_INTERVAL
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.DIMMING
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.DIMMING_DEFAULT
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.PREFERENCES_NAME
import com.homm3.livewallpaper.core.Constants.Preferences.Companion.SCALE
import com.homm3.livewallpaper.parser.formats.H3mReader
import com.sun.java.swing.plaf.motif.MotifBorders.FrameBorder.BORDER_SIZE
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
    private val overlayPixmap = Pixmap(1, 1, Pixmap.Format.RGBA4444)
    private val overlayTexture = Texture(1, 1, Pixmap.Format.RGBA4444)
    private val overlayBatch = SpriteBatch()

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

        updateOverlay(prefs.getInteger(DIMMING, DIMMING_DEFAULT))
    }

    private fun randomizeCameraPosition() {
        val xVisibleBorderSize = 3 * TILE_SIZE
        val yVisibleBorderSize = 3 * TILE_SIZE
        val mapSize = h3mMap.header.size * TILE_SIZE

        val xStart = (camera.viewportWidth / 2) - xVisibleBorderSize
        val xEnd = mapSize - xStart - (camera.viewportWidth / 2) + xVisibleBorderSize
        val nextCameraX = xStart + Random.nextFloat() * xEnd

        val yStart = (camera.viewportHeight / 2) - yVisibleBorderSize
        val yEnd = mapSize - yStart - (camera.viewportHeight / 2) + yVisibleBorderSize
        val nextCameraY = yStart + Random.nextFloat() * yEnd

        engine.cameraPoint.set(nextCameraX, nextCameraY)
        camera.position.set(engine.cameraPoint, 0f)
        viewport.update(Gdx.graphics.width, Gdx.graphics.height)
        objectsLayer.updateVisibleSprites(camera)
    }

    private fun updateOverlay(dimming: Int) {
        overlayPixmap.setColor(0f, 0f, 0f, dimming / 100f)
        overlayPixmap.fill()
        overlayTexture.draw(overlayPixmap, 0, 0)
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

    override fun render(delta: Float) {
        inputProcessor.handlePressedKeys()
        camera.update()
        renderer.setView(camera)
        renderer.render()
        overlayBatch.use(camera.view) {
            it.draw(
                overlayTexture,
                camera.position.x - camera.viewportWidth / 2,
                camera.position.y - camera.viewportHeight / 2,
                camera.viewportWidth,
                camera.viewportHeight
            )
        }
    }

    override fun dispose() {
        super.dispose()
        renderer.dispose()
    }
}