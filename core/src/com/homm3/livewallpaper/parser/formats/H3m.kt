package com.homm3.livewallpaper.parser.formats

import java.util.*

class H3m {
    enum class Version(val value: Int) {
        ROE(0x0e),
        AB(0x15),
        SOD(0x1c),
        WOG(0x33);

        companion object {
            fun fromInt(value: Int): Version {
                return values().find { it.value == value }
                    ?: throw Exception("Unknown map format")
            }
        }
    }

    enum class Town(var value: Int) {
        CASTLE(0),
        RAMPART(1),
        TOWER(2),
        INFERNO(3),
        NECROPOLIS(4),
        DUNGEON(5),
        STRONGHOLD(6),
        FORTRESS(7),
        CONFLUX(8);
    }

    enum class PlayerColor(val value: Int) {
        Red(0),
        Blue(1),
        Tan(2),
        Green(3),
        Orange(4),
        Purple(5),
        Teal(6),
        Pink(7);

        companion object {
            fun fromInt(value: Int): PlayerColor {
                return values().find { it.value == value }
                    ?: throw Exception("Enum value $value not found in ${PlayerColor::name}")
            }
        }
    }

    class Player {
        var playerColor: PlayerColor? = null
        var allowedTowns = mutableListOf<Town>()
        var isRandomTown = false
        var hasMainTown = false
        var isTownsSet = false
        var generateHeroAtMainTown = false
        var generateHero = false
        var hasRandomHero = false
        var mainCustomHeroId = false
        var mainTownType: Town? = null
        var mainTownX = 0
        var mainTownY = 0
        var mainTownZ = 0
    }

    class Tile {
        var terrain = 0
        var terrainImageIndex = 0
        var river = 0
        var riverImageIndex = 0
        var road = 0
        var roadImageIndex = 0
        lateinit var mirrorConfig: BitSet

        fun setMirrorConfig(input: Int) {
            var value = input
            val bits = BitSet()
            var index = 0
            while (value != 0) {
                if (value % 2L != 0L) {
                    bits.set(index)
                }
                ++index
                value = value ushr 1
            }

            mirrorConfig = bits
        }
    }

    class DefInfo {
        var spriteName = ""
        var passableCells = emptyList<Int>()
        var activeCells = emptyList<Int>()
        var placementOrder = 0
        var objectId = 0
        var objectClassSubId = 0

        var terrainType = 0
        var terrainGroup = 0
        var objectsGroup = 0
        var placeholder = byteArrayOf()

        val isVisitable: Boolean
            get() = activeCells.all { it == 0 }
    }

    class Object {
        var x = 0
        var y = 0
        var z = 0
        lateinit var def: DefInfo
        lateinit var obj: H3mObjects.Object
    }

    class Header {
        var hasPlayers = 0
        var size = 0
        var hasUnderground = false
        var title = ""
        var description = ""
        var difficulty = 0
        var levelLimit = 0
        var players = mutableListOf<Player>()
        var availableArtifacts: BitSet? = null
    }

    var version: Version? = null
    lateinit var header: Header
    lateinit var terrainTiles: MutableList<Tile>
    lateinit var defs: MutableList<DefInfo>
    lateinit var objects: MutableList<Object>
}