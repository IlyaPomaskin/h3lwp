package com.homm3.livewallpaper.core.map

import com.homm3.livewallpaper.parser.h3m.H3mObject
import com.homm3.livewallpaper.parser.h3m.H3mObjectType
import kotlin.random.Random

class ObjectsRandomizer {
    enum class Faction {
        CASTLE, RAMPART, TOWER, INFERNO, NECROPOLIS,
        DUNGEON, STRONGHOLD, FORTRESS, CONFLUX, COVE, FACTORY;

        companion object {
            fun fromOrdinal(value: Int): Faction? {
                return entries.find { it.ordinal == value }
            }
        }
    }

    private val resources = arrayOf(
        "avtwood0", "avtore0", "avtsulf0", "avtmerc0", "avtcrys0", "avtgems0", "avtgold0"
    )

    private val dwellings = hashMapOf(
        Faction.CASTLE to arrayOf("AVGpike0", "AVGcros0", "AVGgrff0", "AVGswor0", "AVGmonk0", "AVGcavl0", "AVGangl0"),
        Faction.RAMPART to arrayOf("AVGcent0", "AVGdwrf0", "AVGelf0", "AVGpega0", "AVGtree0", "AVGunic0", "AVGgdrg0"),
        Faction.TOWER to arrayOf("AVGgrem0", "AVGgarg0", "AVGgolm0", "AVGmage0", "AVGgeni0", "AVGnaga0", "AVGtitn0"),
        Faction.INFERNO to arrayOf("AVGimp0", "AVGgogs0", "AVGhell0", "AVGdemn0", "AVGpit0", "AVGefre0", "AVGdevl0"),
        Faction.NECROPOLIS to arrayOf("AVGskel0", "AVGzomb0", "AVGwght0", "AVGvamp0", "AVGlich0", "AVGbkni0", "AVGbone0"),
        Faction.DUNGEON to arrayOf("AVGtrog0", "AVGharp0", "AVGbhld0", "AVGmdsa0", "AVGmino0", "AVGmant0", "AVGrdrg0"),
        Faction.STRONGHOLD to arrayOf("AVGgobl0", "AVGwolf0", "AVGorcg0", "AVGogre0", "AVGrocs0", "AVGcycl0", "AVGbhmt0"),
        Faction.FORTRESS to arrayOf("AVGgnll0", "AVGlzrd0", "AVGdfly0", "AVGbasl0", "AVGgorg0", "AVGwyvn0", "AVGhydr0"),
        Faction.CONFLUX to arrayOf("AVGpixie", "AVGair0", "AVGwatr0", "AVGfire0", "AVGerth0", "AVGelp", "AVGfbrd"),
        Faction.COVE to arrayOf("AVGnymph", "AVGcrew0", "AVGpira0", "AVGstorm", "AVGsorc0", "AVGnixy0", "AVGhsea0"),
        Faction.FACTORY to arrayOf("AVG2half", "AVGmech0", "AVGarmd0", "AVGauto0", "AVGwurm0", "AVGguns0", "AVGcotl0")
    )

    private val monsters = hashMapOf(
        1 to arrayOf("AvWPike", "AVWpikx0", "AVWcent0", "AVWcenx0", "AVWgrem0", "AVWgrex0", "AVWimp0", "AVWimpx0", "AVWskel0", "AVWskex0", "AVWtrog0", "AvWInfr", "AVWgobl0", "AVWgobx0", "AVWgnll0", "AVWgnlx0", "AVWpixie", "AVWsprit", "AVWhalf", "AVWpeas", "AVWhbom", "AVWnymph", "AVWocean"),
        2 to arrayOf("AvWLCrs", "AvWHCrs", "AVWdwrf0", "AVWdwrx0", "AVWgarg0", "AVWgarx0", "AVWgog0", "AVWgogx0", "AVWzomb0", "AVWzomx0", "AVWharp0", "AVWharx0", "AVWwolf0", "AVWwolx0", "AvWLizr", "AVWlizx0", "AVWelmw0", "AVWicee", "AVWboar", "AVWrog", "AVWmech", "AVWengn", "AVWcrew0", "AVWsamn0", "AVWlepr"),
        3 to arrayOf("AvWGrif", "AVWgrix0", "AVWelfw0", "AVWelfx0", "AVWgolm0", "AVWgolx0", "AVWhoun0", "AVWhoux0", "AvWWigh", "AVWwigx0", "AVWbehl0", "AVWbehx0", "AVWorc0", "AVWorcx0", "AvWDFly", "AvWDFir", "AVWelme0", "AVWstone", "AVWmumy", "AVWnomd", "AVWalmd", "AVWalmb", "AVWpira0", "AVWcors0", "AVWsdog0"),
        4 to arrayOf("AVWswrd0", "AVWswrx0", "AVWpega0", "AVWpegx0", "AVWmage0", "AVWmagx0", "AVWdemn0", "AVWdemx0", "AVWvamp0", "AVWvamx0", "AvWMeds", "AVWmedx0", "AVWogre0", "AVWogrx0", "AvWBasl", "AvWGBas", "AVWelma0", "AVWstorm", "AVWglmg0", "AVWsharp", "AVWauto", "AVWauth", "AVWstrb0", "AVWaysd0", "AVWsatr", "AVWstgl"),
        5 to arrayOf("AvWMonk", "AVWmonx0", "AVWtree0", "AVWtrex0", "AVWgeni0", "AVWgenx0", "AVWpitf0", "AVWpitx0", "AVWlich0", "AVWlicx0", "AvWMino", "AVWminx0", "AVWroc0", "AVWrocx0", "AvWGorg", "AVWgorx0", "AVWelmf0", "AVWnrg", "AVWglmd0", "AVWworm", "AVWolgo", "AVWsorc0", "AVWsrcx0", "AVWfang"),
        6 to arrayOf("AVWcvlr0", "AVWcvlx0", "AVWunic0", "AVWunix0", "AVWnaga0", "AVWnagx0", "AVWefre0", "AVWefrx0", "AVWbkni0", "AVWbknx0", "AVWmant0", "AVWmanx0", "AVWcycl0", "AVWcycx0", "AvWWyvr", "AVWwyvx0", "AVWpsye", "AVWmagel", "AVWench", "AVWguns", "AVWbhun", "AVWnixy0", "AVWnixx0"),
        7 to arrayOf("AvWAngl", "AvWArch", "AVWdrag0", "AVWdrax0", "AVWtitn0", "AVWtitx0", "AVWdevl0", "AVWdevx0", "AVWbone0", "AVWbonx0", "AvWRDrg", "AVWddrx0", "AVWbhmt0", "AVWbhmx0", "AvWHydr", "AVWhydx0", "AVWfbird", "AVWphx", "AVWccoat", "AVWradc", "AVWdred", "AVWjugg", "AVWhsea0", "AVWhspd0"),
        10 to arrayOf("AVWfdrg", "AVWazure", "AVWcdrg", "AVWrust")
    )

    private val towns = hashMapOf(
        Faction.CASTLE to "avccasx0",
        Faction.RAMPART to "avcramx0",
        Faction.TOWER to "avctowx0",
        Faction.INFERNO to "avcinfx0",
        Faction.NECROPOLIS to "avcnecx0",
        Faction.DUNGEON to "avcdunx0",
        Faction.STRONGHOLD to "avcstrx0",
        Faction.FORTRESS to "avcftrx0",
        Faction.CONFLUX to "avchforx",
        Faction.COVE to "avchota0",
        Faction.FACTORY to "avcfact0"
    )

    private val villages = hashMapOf(
        Faction.CASTLE to "avccast0",
        Faction.RAMPART to "avcramp0",
        Faction.TOWER to "avctowr0",
        Faction.INFERNO to "avcinfc0",
        Faction.NECROPOLIS to "avcnecr0",
        Faction.DUNGEON to "avcdung0",
        Faction.STRONGHOLD to "avcstro0",
        Faction.FORTRESS to "avcftrt0",
        Faction.CONFLUX to "avchfor0",
        Faction.COVE to "avchota1",
        Faction.FACTORY to "avcfact1"
    )

    fun resolveSpriteName(obj: H3mObject): String {
        return when (obj.objectType) {
            H3mObjectType.RANDOM_MONSTER -> randomMonster(null)
            H3mObjectType.RANDOM_MONSTER_L1 -> randomMonster(1)
            H3mObjectType.RANDOM_MONSTER_L2 -> randomMonster(2)
            H3mObjectType.RANDOM_MONSTER_L3 -> randomMonster(3)
            H3mObjectType.RANDOM_MONSTER_L4 -> randomMonster(4)
            H3mObjectType.RANDOM_MONSTER_L5 -> randomMonster(5)
            H3mObjectType.RANDOM_MONSTER_L6 -> randomMonster(6)
            H3mObjectType.RANDOM_MONSTER_L7 -> randomMonster(7)
            H3mObjectType.ARTIFACT -> specificArtifact(obj.def.objectClassSubId)
            H3mObjectType.RANDOM_ART,
            H3mObjectType.RANDOM_TREASURE_ART,
            H3mObjectType.RANDOM_MINOR_ART,
            H3mObjectType.RANDOM_MAJOR_ART -> randomArtifact()
            H3mObjectType.RANDOM_RELIC_ART -> randomArtifact(isRelic = true)
            H3mObjectType.RANDOM_RESOURCE -> randomResource()
            H3mObjectType.RANDOM_TOWN -> randomTown(obj.def.objectClassSubId)
            H3mObjectType.RANDOM_DWELLING,
            H3mObjectType.RANDOM_DWELLING_LVL,
            H3mObjectType.RANDOM_DWELLING_FACTION -> randomDwelling(null, null)
            H3mObjectType.EVENT,
            H3mObjectType.GRAIL,
            H3mObjectType.RANDOM_HERO,
            H3mObjectType.HERO_PLACEHOLDER,
            H3mObjectType.HERO -> ""
            else -> obj.def.spriteName
        }
    }

    private fun randomMonster(level: Int?): String {
        if (level == null) {
            return randomMonster(monsters.keys.random())
        }
        return monsters[level]!!.random()
    }

    private fun randomArtifact(isRelic: Boolean = false): String {
        val artId = if (isRelic) Random.nextInt(129, 141) else Random.nextInt(10, 127)
        return specificArtifact(artId)
    }

    private fun specificArtifact(id: Int): String {
        return String.format("ava%04d", id)
    }

    private fun randomResource(): String {
        return resources.random()
    }

    private fun randomTown(subId: Int): String {
        val hasFort = Random.nextBoolean()
        val factionsToSprites = if (hasFort) towns else villages
        return factionsToSprites[Faction.fromOrdinal(subId)] ?: factionsToSprites.values.random()
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
}
