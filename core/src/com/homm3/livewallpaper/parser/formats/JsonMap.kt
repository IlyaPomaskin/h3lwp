package com.homm3.livewallpaper.parser.formats

import com.badlogic.gdx.utils.Json
import java.io.InputStream
import java.util.*


class JsonMap {
    class TerrainTile {
        var terrain: Int = 0
        var terrainImageIndex: Int = 0
        var river: Int = 0
        var riverImageIndex: Int = 0
        var road: Int = 0
        var roadImageIndex: Int = 0
        var mirrorConfig: Long = 0

        fun mirror(): BitSet {
            var value = mirrorConfig
            val bits = BitSet()
            var index = 0
            while (value != 0L) {
                if (value % 2L != 0L) {
                    bits.set(index)
                }
                ++index
                value = value ushr 1
            }

            return bits
        }
    }

    class Def {
        lateinit var spriteName: String
        lateinit var activeCells: List<Int>
        lateinit var `object`: String
        var classSubId: Int = 0
        var placementOrder: Int = 0

        fun isVisitable(): Boolean {
            return activeCells.all { it == 0 }
        }
    }

    class MapObject : Comparable<MapObject> {
        var x: Int = 0
        var y: Int = 0
        lateinit var def: Def

        override fun compareTo(other: MapObject): Int {
            val a = this
            val b = other

            if (a.def.placementOrder != b.def.placementOrder)
                return a.def.placementOrder.compareTo(b.def.placementOrder)
            if (a.y != b.y) return b.y.compareTo(a.y)
            if (b.def.`object` === "hero" && a.def.`object` !== "hero") return -1
            if (b.def.`object` !== "hero" && a.def.`object` === "hero") return 1
            if (a.def.isVisitable() != b.def.isVisitable()) {
                if (!a.def.isVisitable() && b.def.isVisitable()) return -1
                if (!b.def.isVisitable() && a.def.isVisitable()) return 1
            }
            if (a.x != b.x) return b.x.compareTo(a.x)
            return 0
        }
    }

    class ParsedMap {
        var size: Int = 0
        lateinit var terrain: List<TerrainTile>
        lateinit var objects: List<MapObject>
    }

    fun parse(stream: InputStream): ParsedMap {
        val json = Json()
        json.ignoreUnknownFields = true
        json.addClassTag("terrain", TerrainTile::class.java)
        json.addClassTag("objects", MapObject::class.java)
        json.addClassTag("def", Def::class.java)
        return json.fromJson(ParsedMap::class.java, stream)
    }
}