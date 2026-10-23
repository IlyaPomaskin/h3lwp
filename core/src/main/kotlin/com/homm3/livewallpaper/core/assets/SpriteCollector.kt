package com.homm3.livewallpaper.core.assets

import com.homm3.livewallpaper.core.defs.ObjectsRandomizer
import com.homm3.livewallpaper.core.defs.RiverDef
import com.homm3.livewallpaper.core.defs.RoadDef
import com.homm3.livewallpaper.core.defs.TerrainDef
import com.homm3.livewallpaper.parser.h3m.H3mMap
import java.util.Locale

class SpriteCollector(hasHotaAssets: Boolean = false) {
    private val randomizer = ObjectsRandomizer(hasHotaAssets)

    /** Lowercase names of the DEFs [maps] draw, as the sprite registry keys them. */
    fun collectNeededSprites(maps: List<H3mMap>): Set<String> {
        val names = mutableSetOf<String>()

        for (map in maps) {
            for (tile in map.tiles) {
                names.add(TerrainDef.byId(tile.terrain) + ".def")
                if (tile.river > 0) {
                    names.add(RiverDef.byId(tile.river) + ".def")
                }
                if (tile.road > 0) {
                    names.add(RoadDef.byId(tile.road) + ".def")
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
                    names.add(defName.lowercase(Locale.ROOT))
                }
            }
        }

        names.add("edg.def")
        return names.map { it.lowercase(Locale.ROOT) }.toSet()
    }
}
