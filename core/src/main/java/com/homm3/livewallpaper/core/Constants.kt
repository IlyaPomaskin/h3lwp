package com.homm3.livewallpaper.core

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion

class Constants {
    companion object {
        const val TILE_SIZE = 32f
        const val FRAME_TIME = 0.18f
        const val VISIBLE_BORDER_SIZE = 3 * TILE_SIZE
        const val SCROLL_OFFSET = 3 * TILE_SIZE
        const val BORDER_SIZE = ((VISIBLE_BORDER_SIZE + SCROLL_OFFSET) / TILE_SIZE).toInt()
        const val VISIBLE_TILES_OFFSET = 4
        const val MAX_RANDOMIZE_RECT_TRIES = 10
        const val MAX_RANDOMIZE_EMPTY_TILES_PERCENT = 30
    }

    class Assets {
        companion object {
            const val USER_MAPS_FOLDER = "user-maps"
            const val ATLAS_FOLDER = "sprites"
            const val ATLAS_NAME = "images"
            const val ATLAS_PATH = "$ATLAS_FOLDER/$ATLAS_NAME.atlas"
            const val SKIN_PATH = "skin/uiskin.json"
            const val I18N_PATH = "i18n/Bundle"
            val emptyPixmap = TextureAtlas.AtlasRegion(
                TextureRegion(
                    Texture(
                        Pixmap(0, 0, Pixmap.Format.RGBA8888)
                    )
                )
            )
        }
    }

    class Preferences {
        companion object {
            // Do not change this or users will be forced to parse assets again
            val PREFERENCES_NAME = Engine::class.java.`package`.name + ".PREFERENCES"
            const val MAP_UPDATE_INTERVAL = "mapUpdateInterval"
            const val DEFAULT_MAP_UPDATE_INTERVAL = (15f * 60f * 1000f).toString()
            const val MINIMAL_MAP_UPDATE_INTERVAL = 1000f
            const val SCALE = "scale"
            const val DEFAULT_SCALE = "0"
            const val USE_SCROLL = "useScroll"
            const val USE_SCROLL_DEFAULT = true
            const val BRIGHTNESS = "brightness"
            const val BRIGHTNESS_DEFAULT = 0.6f
        }
    }
}