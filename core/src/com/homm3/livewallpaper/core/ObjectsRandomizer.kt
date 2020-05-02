package com.homm3.livewallpaper.core

import com.homm3.livewallpaper.parser.formats.JsonMap
import kotlin.random.Random

class ObjectsRandomizer {
    enum class Faction {
        CASTLE,
        RAMPART,
        TOWER,
        INFERNO,
        NECROPOLIS,
        DUNGEON,
        STRONGHOLD,
        FORTRESS,
        CONFLUX;

        companion object {
            fun getByValue(value: Int): Faction? {
                return values().find { it.ordinal == value }
            }
        }
    }

    private val resources = arrayOf("avtwood0", "avtore0", "avtsulf0", "avtmerc0", "avtcrys0", "avtgems0", "avtgold0")

    private val dwellings = hashMapOf(
        Pair(Faction.CASTLE, arrayOf("AVGpike0", "AVGcros0", "AVGgrff0", "AVGswor0", "AVGmonk0", "AVGcavl0", "AVGangl0")),
        Pair(Faction.RAMPART, arrayOf("AVGcent0", "AVGdwrf0", "AVGelf0", "AVGpega0", "AVGtree0", "AVGunic0", "AVGgdrg0")),
        Pair(Faction.TOWER, arrayOf("AVGgrem0", "AVGgarg0", "AVGgolm0", "AVGmage0", "AVGgeni0", "AVGnaga0", "AVGtitn0")),
        Pair(Faction.INFERNO, arrayOf("AVGimp0", "AVGgogs0", "AVGhell0", "AVGdemn0", "AVGpit0", "AVGefre0", "AVGdevl0")),
        Pair(Faction.NECROPOLIS, arrayOf("AVGskel0", "AVGzomb0", "AVGwght0", "AVGvamp0", "AVGlich0", "AVGbkni0", "AVGbone0")),
        Pair(Faction.DUNGEON, arrayOf("AVGtrog0", "AVGharp0", "AVGbhld0", "AVGmdsa0", "AVGmino0", "AVGmant0", "AVGrdrg0")),
        Pair(Faction.STRONGHOLD, arrayOf("AVGgobl0", "AVGwolf0", "AVGorcg0", "AVGogre0", "AVGrocs0", "AVGcycl0", "AVGbhmt0")),
        Pair(Faction.FORTRESS, arrayOf("AVGgnll0", "AVGlzrd0", "AVGdfly0", "AVGbasl0", "AVGgorg0", "AVGwyvn0", "AVGhydr0")),
        Pair(Faction.CONFLUX, arrayOf("AVGpixie", "AVGair0", "AVGwatr0", "AVGfire0", "AVGerth0", "AVGelp", "AVGfbrd"))
    )

    private val monsters = hashMapOf(
        Pair(1, arrayOf("AvWPike", "AVWpikx0", "AVWcent0", "AVWcenx0", "AVWgrem0", "AVWgrex0", "AVWimp0", "AVWimpx0", "AVWskel0", "AVWskex0", "AVWtrog0", "AvWInfr", "AVWgobl0", "AVWgobx0", "AVWgnll0", "AVWgnlx0", "AVWpixie", "AVWsprit", "AVWhalf", "AVWpeas")),
        Pair(2, arrayOf("AvWLCrs", "AvWHCrs", "AVWdwrf0", "AVWdwrx0", "AVWgarg0", "AVWgarx0", "AVWgog0", "AVWgogx0", "AVWzomb0", "AVWzomx0", "AVWharp0", "AVWharx0", "AVWwolf0", "AVWwolx0", "AvWLizr", "AVWlizx0", "AVWelmw0", "AVWicee", "AVWboar", "AVWrog")),
        Pair(3, arrayOf("AvWGrif", "AVWgrix0", "AVWelfw0", "AVWelfx0", "AVWgolm0", "AVWgolx0", "AVWhoun0", "AVWhoux0", "AvWWigh", "AVWwigx0", "AVWbehl0", "AVWbehx0", "AVWorc0", "AVWorcx0", "AvWDFly", "AvWDFir", "AVWelme0", "AVWstone", "AVWmumy", "AVWnomd")),
        Pair(4, arrayOf("AVWswrd0", "AVWswrx0", "AVWpega0", "AVWpegx0", "AVWmage0", "AVWmagx0", "AVWdemn0", "AVWdemx0", "AVWvamp0", "AVWvamx0", "AvWMeds", "AVWmedx0", "AVWogre0", "AVWogrx0", "AvWBasl", "AvWGBas", "AVWelma0", "AVWstorm", "AVWglmg0", "AVWsharp")),
        Pair(5, arrayOf("AvWMonk", "AVWmonx0", "AVWtree0", "AVWtrex0", "AVWgeni0", "AVWgenx0", "AVWpitf0", "AVWpitx0", "AVWlich0", "AVWlicx0", "AvWMino", "AVWminx0", "AVWroc0", "AVWrocx0", "AvWGorg", "AVWgorx0", "AVWelmf0", "AVWnrg", "AVWglmd0")),
        Pair(6, arrayOf("AVWcvlr0", "AVWcvlx0", "AVWunic0", "AVWunix0", "AVWnaga0", "AVWnagx0", "AVWefre0", "AVWefrx0", "AVWbkni0", "AVWbknx0", "AVWmant0", "AVWmanx0", "AVWcycl0", "AVWcycx0", "AvWWyvr", "AVWwyvx0", "AVWpsye", "AVWmagel", "AVWench")),
        Pair(7, arrayOf("AvWAngl", "AvWArch", "AVWdrag0", "AVWdrax0", "AVWtitn0", "AVWtitx0", "AVWdevl0", "AVWdevx0", "AVWbone0", "AVWbonx0", "AvWRDrg", "AVWddrx0", "AVWbhmt0", "AVWbhmx0", "AvWHydr", "AVWhydx0", "AVWfbird", "AVWphx")),
        Pair(10, arrayOf("AVWfdrg", "AVWazure", "AVWcdrg", "AVWrust"))
    )

    private val towns = hashMapOf(
        Pair(Faction.CASTLE, "avccasx0"),
        Pair(Faction.RAMPART, "avcramx0"),
        Pair(Faction.TOWER, "avctowx0"),
        Pair(Faction.INFERNO, "avcinfx0"),
        Pair(Faction.NECROPOLIS, "avcnecx0"),
        Pair(Faction.DUNGEON, "avcdunx0"),
        Pair(Faction.STRONGHOLD, "avcstrx0"),
        Pair(Faction.FORTRESS, "avcftrx0"),
        Pair(Faction.CONFLUX, "avchforx")
    )

    private val villages = hashMapOf(
        Pair(Faction.CASTLE, "avccast0"),
        Pair(Faction.RAMPART, "avcramp0"),
        Pair(Faction.TOWER, "avctowr0"),
        Pair(Faction.INFERNO, "avcinfc0"),
        Pair(Faction.NECROPOLIS, "avcnecr0"),
        Pair(Faction.DUNGEON, "avcdung0"),
        Pair(Faction.STRONGHOLD, "avcstro0"),
        Pair(Faction.FORTRESS, "avcftrt0"),
        Pair(Faction.CONFLUX, "avchfor0")
    )

    private fun randomMonster(level: Int?): String {
        if (level == null) {
            return randomMonster(monsters.keys.random())
        }

        return monsters[level]!!.random()
    }

    private fun randomArtifact(isRelic: Boolean = false): String {
        val artId = if (isRelic)
            Random.nextInt(129, 141)
        else
            Random.nextInt(10, 127)

        return String.format("ava%04d", artId)
    }

    private fun randomResource(): String {
        return resources.random()
    }

    private fun randomTown(subId: Int): String {
        val hasFort = Random.nextBoolean()
        val factionsToSprites = if (hasFort) towns else villages

        return factionsToSprites[Faction.getByValue(subId)] ?: factionsToSprites.values.random()
    }

    private fun randomDwelling(faction: Faction?, level: Int?): String {
        if (faction != null && level != null) {
            return dwellings[faction]?.get(level) ?: randomDwelling(null, null)
        }

        if (level != null) {
            return dwellings.values.random()[level]
        }

        return dwellings.values.random().random()
    }

    fun replaceRandomObject(obj: JsonMap.MapObject): String {
        return when (obj.def.`object`) {
            "random-monster" -> randomMonster(null)
            "random-monster-l1" -> randomMonster(1)
            "random-monster-l2" -> randomMonster(2)
            "random-monster-l3" -> randomMonster(3)
            "random-monster-l4" -> randomMonster(4)
            "random-monster-l5" -> randomMonster(5)
            "random-monster-l6" -> randomMonster(6)
            "random-monster-l7" -> randomMonster(7)
            "random-art" -> randomArtifact()
            "random-treasure-art" -> randomArtifact()
            "random-minor-art" -> randomArtifact()
            "random-major-art" -> randomArtifact()
            "random-relic-art" -> randomArtifact(true)
            "random-resource" -> randomResource()
            "random-town" -> randomTown(obj.def.classSubId)
            "random-hero" -> "empty"
            "random-dwelling" -> randomDwelling(null, null)
            "random-dwelling-lvl" -> randomDwelling(
                null,
                null
                // TODO Random.nextInt(obj.info.get("min"), obj.info.get("max"))
            )
            "random-dwelling-faction" -> randomDwelling(null, null)
            else -> obj.def.spriteName
        }
    }
}