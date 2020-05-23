package com.homm3.livewallpaper.parser.formats

import java.util.*

class H3m {
    enum class Version(val value: Int) {
        ROE(0x0e),
        AB(0x15),
        SOD(0x1c);

        companion object {
            fun fromInt(value: Int): Version {
                return values().find { it.value == value }
                    ?: throw Exception("Unknown map format")
            }
        }
    }

    class Player {
        var playerColor = 0
        var hasMainTown = false
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
            get() = activeCells.any { it != 0 }
    }

    class Object : Comparable<Object> {
        var x = 0
        var y = 0
        var z = 0
        lateinit var def: DefInfo
        lateinit var obj: H3mObjects.Object

        private fun <T>compare2way(x: T, y: T, predicate: (x: T, y: T) -> Boolean): Int {
            return when {
                predicate(x, y) -> -1
                predicate(y, x) -> 1
                else -> 0
            }
        }

        override fun compareTo(other: Object): Int {
            return compare2way(this, other) { a, b ->
                when {
                    a.def.placementOrder != b.def.placementOrder ->
                        a.def.placementOrder > b.def.placementOrder
                    a.y != b.y -> a.y < b.y
                    b.obj == H3mObjects.Object.HERO && a.obj != H3mObjects.Object.HERO -> true
                    b.obj != H3mObjects.Object.HERO && a.obj == H3mObjects.Object.HERO -> false
                    !a.def.isVisitable && b.def.isVisitable -> true
                    !b.def.isVisitable && a.def.isVisitable -> false
                    else -> a.x < b.x
                }
            }
        }
    }

    class Header {
        var hasPlayers = 0
        var size = 0
        var hasUnderground = false
        var title = ""
        var description = ""
        var players = mutableListOf<Player>()
    }

    var version: Version? = null
    lateinit var header: Header
    lateinit var terrainTiles: MutableList<Tile>
    lateinit var defs: MutableList<DefInfo>
    lateinit var objects: MutableList<Object>
}