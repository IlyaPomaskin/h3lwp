package com.heroes3.livewallpaper.core

class Constants {
    enum class TerrainDefs(value: Int) {
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
                return values().find { it.ordinal == int }.toString()
            }
        }
    }

    enum class RiverDefs(value: Int) {
        clrrvr(1),
        icyrvr(2),
        mudrvr(3),
        lavrvr(4);

        companion object {
            fun byInt(int: Int): String {
                return values().find { it.ordinal == int - 1 }.toString()
            }
        }
    }

    enum class RoadDefs(value: Int) {
        dirtrd(1),
        gravrd(2),
        cobbrd(3);

        companion object {
            fun byInt(int: Int): String {
                return values().find { it.ordinal == int - 1 }.toString()
            }
        }
    }
}