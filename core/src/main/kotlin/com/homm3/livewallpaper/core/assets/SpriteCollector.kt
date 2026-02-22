package com.homm3.livewallpaper.core.assets

import com.homm3.livewallpaper.core.map.ObjectsRandomizer
import com.homm3.livewallpaper.core.map.layers.TerrainLayer
import com.homm3.livewallpaper.parser.h3m.H3mMap

class SpriteCollector {
    private val randomizer = ObjectsRandomizer()

    fun collectNeededSprites(maps: List<H3mMap>): Set<String> {
        val names = mutableSetOf<String>()

        for (map in maps) {
            for (tile in map.tiles) {
                names.add(TerrainLayer.TerrainDef.byId(tile.terrain) + ".def")
                if (tile.river > 0) {
                    names.add(TerrainLayer.RiverDef.byId(tile.river) + ".def")
                }
                if (tile.road > 0) {
                    names.add(TerrainLayer.RoadDef.byId(tile.road) + ".def")
                }
            }

            for (obj in map.objects) {
                val spriteName = randomizer.resolveSpriteName(obj)
                if (spriteName.isNotEmpty()) {
                    val defName = if (spriteName.endsWith(".def", true)) {
                        spriteName
                    } else {
                        "$spriteName.def"
                    }
                    names.add(defName)
                }
            }
        }

        names.add("edg.def")
        return names
    }
}
