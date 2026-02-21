package com.homm3.livewallpaper.parser.h3m

import java.util.BitSet

data class H3mMap(
    val version: H3mVersion,
    val hotaSubVersion: Int = 0,
    val header: H3mHeader,
    val tiles: List<H3mTile>,
    val defs: List<H3mDef>,
    val objects: List<H3mObject>
)

enum class H3mVersion(val value: Int) {
    ROE(0x0e),
    AB(0x15),
    SOD(0x1c),
    HOTA(0x20);

    companion object {
        fun fromInt(value: Int): H3mVersion {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown map format: $value")
        }
    }
}

data class H3mHeader(val size: Int, val hasUnderground: Boolean)

data class H3mTile(
    val terrain: Int,
    val terrainIndex: Int,
    val river: Int,
    val riverIndex: Int,
    val road: Int,
    val roadIndex: Int,
    val mirrorConfig: BitSet
)

data class H3mDef(
    val spriteName: String,
    val passableCells: List<Int>,
    val activeCells: List<Int>,
    val placementOrder: Int,
    val objectId: Int,
    val objectClassSubId: Int
) {
    val isVisitable: Boolean
        get() = activeCells.any { it != 0 }
}

data class H3mObject(
    val x: Int,
    val y: Int,
    val z: Int,
    val def: H3mDef,
    val objectType: H3mObjectType
) : Comparable<H3mObject> {

    override fun compareTo(other: H3mObject): Int {
        return compare2way(this, other) { a, b ->
            when {
                a.def.placementOrder != b.def.placementOrder ->
                    a.def.placementOrder > b.def.placementOrder
                a.y != b.y -> a.y < b.y
                b.objectType == H3mObjectType.HERO && a.objectType != H3mObjectType.HERO -> true
                b.objectType != H3mObjectType.HERO && a.objectType == H3mObjectType.HERO -> false
                !a.def.isVisitable && b.def.isVisitable -> true
                !b.def.isVisitable && a.def.isVisitable -> false
                else -> a.x < b.x
            }
        }
    }

    private fun <T> compare2way(x: T, y: T, predicate: (x: T, y: T) -> Boolean): Int {
        return when {
            predicate(x, y) -> -1
            predicate(y, x) -> 1
            else -> 0
        }
    }
}
