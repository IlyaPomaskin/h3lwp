package com.homm3.livewallpaper.core.render

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.maps.MapLayer
import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.homm3.livewallpaper.core.WallpaperPreferences
import com.homm3.livewallpaper.core.assets.indexed.PaletteColor
import com.homm3.livewallpaper.core.assets.indexed.PaletteRegion
import com.homm3.livewallpaper.core.map.layers.ObjectsLayer

/**
 * OrthogonalTiledMapRenderer that draws through PaletteShader. Tile layers are rendered with a per-tile
 * vertex colour carrying the tile's palette slot instead of one colour for the whole layer.
 */
class PaletteTiledMapRenderer(
    map: TiledMap,
    batch: PaletteSpriteBatch,
) : OrthogonalTiledMapRenderer(map, batch as SpriteBatch) {

    val paletteShader = PaletteShader.compile()

    var brightness = WallpaperPreferences.defaultBrightness
        set(value) {
            field = value.coerceIn(MIN_BRIGHTNESS, MAX_BRIGHTNESS)
        }

    init {
        batch.shader = paletteShader
        paletteShader.bind()
        paletteShader.setUniformi(PaletteShader.TEXTURE_UNIFORM, INDEX_UNIT)
        paletteShader.setUniformi(PaletteShader.PALETTE_UNIFORM, PaletteSpriteBatch.PALETTE_UNIT)
    }

    override fun beginRender() {
        super.beginRender()
        paletteShader.setUniformf(PaletteShader.BRIGHTNESS_UNIFORM, brightness)
    }

    override fun renderObjects(layer: MapLayer?) {
        if (layer is ObjectsLayer) layer.render(this)
    }

    /** Copy of libGDX 1.14.0 OrthogonalTiledMapRenderer.renderTileLayer; only the vertex colour differs. */
    override fun renderTileLayer(layer: TiledMapTileLayer) {
        val alpha = batch.color.a * layer.combinedTintColor.a * layer.opacity

        val layerWidth = layer.width
        val layerHeight = layer.height

        val layerTileWidth = layer.tileWidth * unitScale
        val layerTileHeight = layer.tileHeight * unitScale

        val layerOffsetX = layer.renderOffsetX * unitScale - viewBounds.x * (layer.parallaxX - 1)
        val layerOffsetY = -layer.renderOffsetY * unitScale - viewBounds.y * (layer.parallaxY - 1)

        val col1 = maxOf(0, ((viewBounds.x - layerOffsetX) / layerTileWidth).toInt())
        val col2 = minOf(
            layerWidth,
            ((viewBounds.x + viewBounds.width + layerTileWidth - layerOffsetX) / layerTileWidth).toInt(),
        )

        val row1 = maxOf(0, ((viewBounds.y - layerOffsetY) / layerTileHeight).toInt())
        val row2 = minOf(
            layerHeight,
            ((viewBounds.y + viewBounds.height + layerTileHeight - layerOffsetY) / layerTileHeight).toInt(),
        )

        var y = row2 * layerTileHeight + layerOffsetY
        val xStart = col1 * layerTileWidth + layerOffsetX
        val vertices = this.vertices

        for (row in row2 downTo row1) {
            var x = xStart
            for (col in col1 until col2) {
                val cell = layer.getCell(col, row)
                val tile = cell?.tile
                if (cell != null && tile != null) {
                    val region = tile.textureRegion
                    val color = PaletteColor.encode(paletteSlotOf(region), alpha)

                    val x1 = x + tile.offsetX * unitScale
                    val y1 = y + tile.offsetY * unitScale
                    val x2 = x1 + region.regionWidth * unitScale
                    val y2 = y1 + region.regionHeight * unitScale

                    val u1 = region.u
                    val v1 = region.v2
                    val u2 = region.u2
                    val v2 = region.v

                    vertices[X1] = x1
                    vertices[Y1] = y1
                    vertices[C1] = color
                    vertices[U1] = u1
                    vertices[V1] = v1

                    vertices[X2] = x1
                    vertices[Y2] = y2
                    vertices[C2] = color
                    vertices[U2] = u1
                    vertices[V2] = v2

                    vertices[X3] = x2
                    vertices[Y3] = y2
                    vertices[C3] = color
                    vertices[U3] = u2
                    vertices[V3] = v2

                    vertices[X4] = x2
                    vertices[Y4] = y1
                    vertices[C4] = color
                    vertices[U4] = u2
                    vertices[V4] = v1

                    if (cell.flipHorizontally) {
                        swap(vertices, U1, U3)
                        swap(vertices, U2, U4)
                    }
                    if (cell.flipVertically) {
                        swap(vertices, V1, V3)
                        swap(vertices, V2, V4)
                    }
                    rotate(vertices, cell.rotation)

                    batch.draw(region.texture, vertices, 0, NUM_VERTICES)
                }
                x += layerTileWidth
            }
            y -= layerTileHeight
        }
    }

    override fun dispose() {
        super.dispose()
        paletteShader.dispose()
    }

    private fun paletteSlotOf(region: TextureRegion): Int {
        return (region as? PaletteRegion)?.paletteSlots?.at(AnimationClock.frameStep) ?: DEFAULT_PALETTE_SLOT
    }

    private fun swap(vertices: FloatArray, first: Int, second: Int) {
        val temp = vertices[first]
        vertices[first] = vertices[second]
        vertices[second] = temp
    }

    private fun rotate(vertices: FloatArray, rotation: Int) {
        when (rotation) {
            TiledMapTileLayer.Cell.ROTATE_90 -> {
                val tempV = vertices[V1]
                vertices[V1] = vertices[V2]
                vertices[V2] = vertices[V3]
                vertices[V3] = vertices[V4]
                vertices[V4] = tempV

                val tempU = vertices[U1]
                vertices[U1] = vertices[U2]
                vertices[U2] = vertices[U3]
                vertices[U3] = vertices[U4]
                vertices[U4] = tempU
            }
            TiledMapTileLayer.Cell.ROTATE_180 -> {
                swap(vertices, U1, U3)
                swap(vertices, U2, U4)
                swap(vertices, V1, V3)
                swap(vertices, V2, V4)
            }
            TiledMapTileLayer.Cell.ROTATE_270 -> {
                val tempV = vertices[V1]
                vertices[V1] = vertices[V4]
                vertices[V4] = vertices[V3]
                vertices[V3] = vertices[V2]
                vertices[V2] = tempV

                val tempU = vertices[U1]
                vertices[U1] = vertices[U4]
                vertices[U4] = vertices[U3]
                vertices[U3] = vertices[U2]
                vertices[U2] = tempU
            }
        }
    }

    private companion object {
        const val INDEX_UNIT = 0
        const val MIN_BRIGHTNESS = 0f
        const val MAX_BRIGHTNESS = 1f
        const val DEFAULT_PALETTE_SLOT = 0
        const val NUM_VERTICES = 20

        const val X1 = 0
        const val Y1 = 1
        const val C1 = 2
        const val U1 = 3
        const val V1 = 4
        const val X2 = 5
        const val Y2 = 6
        const val C2 = 7
        const val U2 = 8
        const val V2 = 9
        const val X3 = 10
        const val Y3 = 11
        const val C3 = 12
        const val U3 = 13
        const val V3 = 14
        const val X4 = 15
        const val Y4 = 16
        const val C4 = 17
        const val U4 = 18
        const val V4 = 19
    }
}
