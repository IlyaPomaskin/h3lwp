package com.homm3.livewallpaper.core

class Constants {
    companion object {
        const val TILE_SIZE = 32f
        const val FRAME_TIME = 0.18f
    }

    class Preferences {
        companion object {
            val PREFERENCES_NAME = Engine::class.java.`package`.name + ".PREFERENCES"
            const val IS_ASSETS_READY_KEY = "isAssetsReady"
            const val MAP_UPDATE_INTERVAL = "mapUpdateInterval"
            const val SCALE = "scale"
            const val DEFAULT_MAP_UPDATE_INTERVAL = 15
            const val DEFAULT_SCALE = "DPI"
        }
    }

    enum class TerrainDefs(val value: Int) {
        dirttl(0),
        sandtl(1),
        grastl(2),
        snowtl(3),
        swmptl(4),
        rougtl(5),
        subbtl(6),
        lavatl(7),
        watrtl(8),
        rocktl(9);

        companion object {
            fun byInt(int: Int): String {
                return values().find { it.value == int }.toString()
            }
        }
    }

    enum class RiverDefs(val value: Int) {
        clrrvr(1),
        icyrvr(2),
        mudrvr(3),
        lavrvr(4);

        companion object {
            fun byInt(int: Int): String {
                return values().find { it.value == int }.toString()
            }
        }
    }

    enum class RoadDefs(val value: Int) {
        dirtrd(1),
        gravrd(2),
        cobbrd(3);

        companion object {
            fun byInt(int: Int): String {
                return values().find { it.value == int }.toString()
            }
        }
    }
}