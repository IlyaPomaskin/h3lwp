package com.heroes3.livewallpaper.parser

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.InputStream


class JsonMapParser {
    class TerrainTile(
        val terrain: Int,
        val terrainImageIndex: Int,
        val river: Int,
        val riverImageIndex: Int,
        val road: Int,
        val roadImageIndex: Int,
        val mirrorConfig: Long
    )

    class Def(
        var spriteName: String,
        val passableCells: List<Int>,
        val activeCells: List<Int>,
        val terrainType: Int,
        val terrainGroup: Int,
        val `object`: String,
        val classSubId: Int,
        val group: Int,
        val placementOrder: Int
    ) {
        val isVisitable = activeCells.all { it == 0 }
    }

    class MapObject(
        val x: Int,
        val y: Int,
        val z: Int,
        val defIndex: Int,
        val info: Map<String, *>?,
        val def: Def
    ) : Comparable<MapObject> {
       override fun compareTo(other: MapObject): Int {
            val a = this
            val b = other

            if (a.def.placementOrder != b.def.placementOrder)
                return a.def.placementOrder.compareTo(b.def.placementOrder)
            if (a.y != b.y) return b.y.compareTo(a.y)
            if (b.def.`object` === "hero" && a.def.`object` !== "hero") return -1
            if (b.def.`object` !== "hero" && a.def.`object` === "hero") return 1
            if (a.def.isVisitable != b.def.isVisitable) {
                if (!a.def.isVisitable && b.def.isVisitable) return -1
                if (!b.def.isVisitable && a.def.isVisitable) return 1
            }
            if (a.x != b.x) return b.x.compareTo(a.x)
            return 0
        }
    }

    class ParsedMap(
        val size: Int = 0,
        val hasUnderground: Boolean,
        val terrain: List<TerrainTile>,
        val objects: List<MapObject>
    )

    private val mapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    fun parse(stream: InputStream): ParsedMap {
        return mapper.readValue(stream, ParsedMap::class.java)
    }
}