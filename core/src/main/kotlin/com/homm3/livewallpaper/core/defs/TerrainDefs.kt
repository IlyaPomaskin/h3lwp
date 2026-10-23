package com.homm3.livewallpaper.core.defs

enum class TerrainDef(val id: Int) {
    dirttl(0), sandtl(1), grastl(2), snowtl(3), swmptl(4),
    rougtl(5), subbtl(6), lavatl(7), watrtl(8), rocktl(9),
    highlnd(10), wastlnd(11);

    companion object {
        fun byId(id: Int): String = entries.find { it.id == id }?.toString() ?: "dirttl"
    }
}

enum class RiverDef(val id: Int) {
    clrrvr(1), icyrvr(2), mudrvr(3), lavrvr(4);

    companion object {
        fun byId(id: Int): String = entries.find { it.id == id }?.toString() ?: "clrrvr"
    }
}

enum class RoadDef(val id: Int) {
    dirtrd(1), gravrd(2), cobbrd(3);

    companion object {
        fun byId(id: Int): String = entries.find { it.id == id }?.toString() ?: "dirtrd"
    }
}
