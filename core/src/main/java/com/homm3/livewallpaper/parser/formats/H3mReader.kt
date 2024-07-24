package com.homm3.livewallpaper.parser.formats

import com.homm3.livewallpaper.parser.formats.H3m.DefInfo
import com.homm3.livewallpaper.parser.formats.H3m.Player
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream
import kotlin.math.ceil
import kotlin.math.pow


class H3mReader(stream: InputStream) {
    private val h3m = H3m()

    private var reader = Reader(readWholeFile(GZIPInputStream(stream)))

    private fun readWholeFile(input: InputStream): ByteArrayInputStream {
        val output = ByteArrayOutputStream()
        output.use { out ->
            val bufferSize = 512
            val buffer = ByteArray(bufferSize)
            var length: Int
            while (input.read(buffer, 0, bufferSize).also { length = it } > 0) {
                out.write(buffer, 0, length)
            }
        }
        return output.toByteArray().inputStream()
    }

    @Throws(IOException::class)
    fun read(): H3m {
        h3m.version = H3m.Version.fromInt(reader.readInt())

        if (h3m.version > H3m.Version.HOTA1) {
            h3m.hotaVersion = reader.readInt()

            if (h3m.hotaVersion >= 1) {
                reader.readByte() // hota_data_1
                reader.readByte() // is_arena
            }
            if (h3m.hotaVersion >= 2) {
                reader.readInt()
            }
            if (h3m.hotaVersion >= 4) { //hotaVersion >= 5 ?
                reader.readInt() // town type count, always 0x0b?
                reader.readBool() // fixed difficulty level
            }
        }


        h3m.header = readHeader()
        h3m.terrainTiles = readTerrain()
        h3m.defs = readDefs()
        h3m.objects = readObjects()
        return h3m
    }

    private fun readHeader(): H3m.Header {
        val header = H3m.Header()
        header.hasPlayers = reader.readByte()
        header.size = reader.readInt()
        header.hasUnderground = reader.readBool()
        header.title = reader.readString()
        header.description = reader.readString()
        reader.readByte() // difficulty

        if (h3m.version > H3m.Version.ROE) {
            reader.readByte() // levelLimit
        }

        header.players = readPlayerInfo()
        readVictoryLossConditions()
        readTeamInfo()
        readAllowedHeroes()
        readDisposedHeroes()
        reader.readBytes(31) // placeholder
        readHota()
        readAllowedArtifacts()
        readAllowedSpellsAbilities()
//        FIXME lost uint32 between allowed heroes and rumors.
//        Hota check for '16' const is okay, maybe problem in arts
        reader.readInt() // unknown
        readRumors()
        readPredefinedHeroes()

        return header
    }

    private fun readHota() {
        if (h3m.version < H3m.Version.HOTA1) {
            return
        }

        reader.readInt() // allowSpecialWeeks
        val unknown1 = reader.readShort() // unknown1 // 16
        assert(unknown1 == 16)
        reader.readInt() // unknown2 // 0

        if (h3m.hotaVersion >= 3) {
            reader.readInt() // round limit
        }

        if (h3m.hotaVersion >= 5) {
            reader.readInt() // unknown3
            reader.readInt() // unknown4
        }
    }

    private fun readPlayerInfo(): MutableList<Player> {
        val players = mutableListOf<Player>()

        for (i in 0..7) {
            val player = Player()
            player.playerColor = i

            val canHumanPlay = reader.readBool()
            val canPCPlay = reader.readBool()

            reader.readByte() //ai behavior

            if (h3m.version >= H3m.Version.SOD) {
                reader.readByte() // allowedAlignments // unused?
            }

            // townTypes
            if (h3m.version === H3m.Version.ROE) {
                reader.readByte()
            } else {
                reader.readShort()
            }

            reader.readBool() // isRandomTown
            player.hasMainTown = reader.readBool()
            if (player.hasMainTown) {
                if (h3m.version !== H3m.Version.ROE) {
                    reader.readBool() // generateHeroAtMainTown
                    reader.readBool() // generateHero
                }
                player.mainTownX = reader.readByte()
                player.mainTownY = reader.readByte()
                player.mainTownZ = reader.readByte()
            }

            reader.readBool() //has random hero

            val heroId = reader.readByte() //main custom hero id
            if (heroId != 0xFF) {
                reader.readByte() // portrait
                reader.readString() // name
            }

            if (h3m.version > H3m.Version.ROE) {
                reader.readByte() //unknown byte

                val heroCount: Int = reader.readInt()
                for (k in 0 until heroCount) {
                    reader.readByte() // hero id
                    reader.readString()
                }
            }
            players.add(player)
        }

        return players
    }

    private fun readVictoryLossConditions() {
        val victoryCondition = reader.readByte()
        if (victoryCondition != 0xFF) {
            //allow normal victory
            //applies to ai
            reader.readBytes(2)
        }
        when (victoryCondition) {
            0 -> {
                //ARTIFACT
                reader.readByte() //obj terrain
                if (h3m.version !== H3m.Version.ROE) {
                    reader.readByte()
                }
            }

            1 -> {
                //GATHERTROOP
                reader.readByte() //obj terrain
                if (h3m.version !== H3m.Version.ROE) {
                    reader.readByte()
                }
                reader.readInt() //value
            }

            2 -> {
                //GATHERRESOURCE
                reader.readByte() //obj terrain
                reader.readInt() //value
            }

            3 -> {
                //BUILDCITY
                reader.readBytes(3) //coords
                reader.readByte() //obj terrain village
                reader.readByte() //obj terrain form
            }

            4 -> reader.readBytes(3) //BUILDGRAIL coords
            5 -> reader.readBytes(3) //BEATHERO coords
            6 -> reader.readBytes(3) //CAPTURECITY coords
            7 -> reader.readBytes(3) //BEATMONSTER coords
            10 -> {
                //TRANSPORTITEM
                reader.readByte() //obj terrain
                reader.readBytes(3) //coords
            }

            12 -> reader.readInt() //SURVIVE days
        }

        when (reader.readByte()) { // lossCondition
            0 -> reader.readBytes(3) //LOSSCASTLE
            1 -> reader.readBytes(3) //LOSSHERO
            2 -> reader.readShort() //TIMEEXPIRES
        }
    }

    private fun readTeamInfo() {
        val teamsCount = reader.readByte()
        if (teamsCount > 0) {
            reader.skip(8)
        }
    }

    private fun readAllowedHeroes() {
        when (h3m.version) {
            H3m.Version.ROE -> reader.readBytes(16)
            H3m.Version.SOD -> reader.readBytes(20)
            H3m.Version.AB -> reader.readBytes(20)
            H3m.Version.HOTA1,
            H3m.Version.HOTA2,
            H3m.Version.HOTA3 -> {
                // dynamic?
                // https://github.com/mapron/FreeHeroes/blob/5bb6d8d41de47d8f016512a1844796ba4b1f86ce/src/Core/MapUtil/H3MMap.cpp#L415
                val heroesCount = ceil(reader.readInt().toDouble() / 8).toInt()
//                reader.readBytes(heroesCount)
                reader.readBytes(25)
            }
        }

        if (h3m.version > H3m.Version.ROE) {
            val placeholderSize = reader.readInt()
            reader.readBytes(placeholderSize)
        }
    }

    private fun readDisposedHeroes() {
        if (h3m.version > H3m.Version.SOD) {
            val heroesCount = reader.readByte()
            for (i in 0 until heroesCount) {
                reader.readByte() //hero id
                reader.readByte() //portrait
                reader.readString() //name
                reader.readByte() //players
            }
        }
    }

    private fun readAllowedArtifacts() {
        val artifactsBits = when (h3m.version) {
            H3m.Version.ROE -> 128
            H3m.Version.AB -> 129
            H3m.Version.SOD -> 141
            H3m.Version.HOTA1 -> 165
            H3m.Version.HOTA2 -> 165
            H3m.Version.HOTA3 -> {
                if (h3m.hotaVersion >= 5) {
                    166
                } else {
                    165
                }
            }
        }
        val artifactsBytes = ceil(artifactsBits.toDouble() / 8).toInt()
        reader.readBytes(artifactsBytes)
    }

    private fun readAllowedSpellsAbilities() {
        if (h3m.version >= H3m.Version.SOD) {
            reader.readBytes(9) //spells
            reader.readBytes(4) //abilities
        }
    }

    private fun readRumors() {
        val rumorsCount = reader.readInt()
        for (i in 0 until rumorsCount) {
            reader.readString() //name
            reader.readString() //text
        }
    }

    private fun loadArtifactToSlot() {
        if (h3m.version === H3m.Version.ROE) {
            reader.readByte()
        } else {
            reader.readShort()
        }
    }

    private fun loadArtifactsOfHero() {
        if (!reader.readBool()) {
            // no arts
            return
        }

        for (j in 0..15) {
            loadArtifactToSlot()
        }
        if (h3m.version === H3m.Version.SOD) {
            loadArtifactToSlot()
        }

        loadArtifactToSlot() //spellbook

        if (h3m.version !== H3m.Version.ROE) {
            loadArtifactToSlot() //fifth slot
        } else {
            reader.readByte()
        }

        val artsCount = reader.readShort() //bag
        for (h in 0 until artsCount) {
            loadArtifactToSlot()
        }
    }

    private fun readPredefinedHeroes() {
        if (h3m.version < H3m.Version.SOD) {
            return
        }

        val size = if (h3m.version >= H3m.Version.HOTA1) {
            reader.readInt()
        } else {
            155
        }

        for (i in 0 until size) {
            //skip if hero doesnt have settings
            if (!reader.readBool()) {
                continue
            }

            if (reader.readBool()) {
                reader.readInt() //exp
            }

            if (reader.readBool()) {  //secondary skills
                val skillsCount = reader.readInt()
                for (k in 0 until skillsCount) {
                    reader.readByte() //skill
                    reader.readByte() //value
                }
            }

            loadArtifactsOfHero()  //artifacts

            if (reader.readBool()) {
                reader.readString() //bio
            }

            reader.readByte() //sex

            if (reader.readBool()) {
                reader.readBytes(9) //spells
            }

            if (reader.readBool()) {
                reader.readBytes(4) //primary skills
            }
        }

        for (i in 0 until size) {
            reader.readBool() // always_add_skills
            reader.readBool() // cannot_gain_xp
            reader.readInt() // level
        }
    }

    private fun readTerrain(): MutableList<H3m.Tile> {
        val tiles = mutableListOf<H3m.Tile>()

        val undergroundMultiplier = if (h3m.header.hasUnderground) 2 else 1
        val size = h3m.header.size.toDouble().pow(2.toDouble()).toInt() * undergroundMultiplier

        for (i in 0 until size) {
            val tile = H3m.Tile()
            tile.terrain = reader.readByte()
            tile.terrainImageIndex = reader.readByte()
            tile.river = reader.readByte()
            tile.riverImageIndex = reader.readByte()
            tile.road = reader.readByte()
            tile.roadImageIndex = reader.readByte()
            tile.setMirrorConfig(reader.readByte())
            tiles.add(tile)
        }

        return tiles
    }

    private fun readDefs(): MutableList<DefInfo> {
        val defs = mutableListOf<DefInfo>()

        val defsCount = reader.readInt()
        for (i in 0 until defsCount) {
            val def = DefInfo()
            def.spriteName = reader.readString()
            def.passableCells = listOf(reader.readInt(), reader.readShort())
            def.activeCells = listOf(reader.readInt(), reader.readShort())
            def.terrainType = reader.readShort() //terrain type
            def.terrainGroup = reader.readShort() //terrain group
            def.objectId = reader.readInt()
            def.objectClassSubId = reader.readInt()
            def.objectsGroup = reader.readByte()
            def.placementOrder = reader.readByte()
            def.placeholder = reader.readBytes(16)
            defs.add(def)
        }

        return defs
    }

    val CHECK_PADDING = 10

    private fun readObject(objectsReader: H3mObjects): H3m.Object {
        val obj = H3m.Object()
        obj.x = reader.readByte()
        obj.y = reader.readByte()
        obj.z = reader.readByte()

        val isIncorrectZ = if (h3m.header.hasUnderground) (obj.z > 1) else (obj.z > 0)
        if (isIncorrectZ) throw Exception("Wrong z: ${obj.z}")
        if (obj.x > h3m.header.size + CHECK_PADDING) throw Exception("Wrong x: ${obj.x}")
        if (obj.y > h3m.header.size + CHECK_PADDING) throw Exception("Wrong y: ${obj.z}")

        val index = reader.readInt()

        if (index > h3m.defs.size) throw Exception("Wrong def index $index of ${h3m.defs.size}")

        obj.def = h3m.defs[index]
        obj.obj = H3mObjects.Object.fromInt(obj.def.objectId)

        reader.readBytes(5)

        when (obj.obj) {
            H3mObjects.Object.EVENT -> objectsReader.readEvent()
            H3mObjects.Object.HERO,
            H3mObjects.Object.RANDOM_HERO,
            H3mObjects.Object.PRISON -> objectsReader.readHero()

            H3mObjects.Object.MONSTER,
            H3mObjects.Object.RANDOM_MONSTER,
            H3mObjects.Object.RANDOM_MONSTER_L1,
            H3mObjects.Object.RANDOM_MONSTER_L2,
            H3mObjects.Object.RANDOM_MONSTER_L3,
            H3mObjects.Object.RANDOM_MONSTER_L4,
            H3mObjects.Object.RANDOM_MONSTER_L5,
            H3mObjects.Object.RANDOM_MONSTER_L6,
            H3mObjects.Object.RANDOM_MONSTER_L7 -> objectsReader.readMonster()

            H3mObjects.Object.OCEAN_BOTTLE,
            H3mObjects.Object.SIGN -> objectsReader.readSign()

            H3mObjects.Object.SEER_HUT -> objectsReader.readSeerHut()
            H3mObjects.Object.WITCH_HUT -> objectsReader.readWitchHut()
            H3mObjects.Object.SCHOLAR -> objectsReader.readScholar()
            H3mObjects.Object.GARRISON,
            H3mObjects.Object.GARRISON2 -> objectsReader.readGarrison()

            H3mObjects.Object.ARTIFACT,
            H3mObjects.Object.RANDOM_ART,
            H3mObjects.Object.RANDOM_TREASURE_ART,
            H3mObjects.Object.RANDOM_MINOR_ART,
            H3mObjects.Object.RANDOM_MAJOR_ART,
            H3mObjects.Object.RANDOM_RELIC_ART,
            H3mObjects.Object.SPELL_SCROLL -> objectsReader.readArtifact(obj.obj)

            H3mObjects.Object.RANDOM_RESOURCE,
            H3mObjects.Object.RESOURCE -> objectsReader.readResource()

            H3mObjects.Object.RANDOM_TOWN,
            H3mObjects.Object.TOWN -> objectsReader.readTown()

            H3mObjects.Object.MINE,
            H3mObjects.Object.ABANDONED_MINE,
            H3mObjects.Object.SHRINE_OF_MAGIC_INCANTATION,
            H3mObjects.Object.SHRINE_OF_MAGIC_GESTURE,
            H3mObjects.Object.SHRINE_OF_MAGIC_THOUGHT,
            H3mObjects.Object.SHIPYARD,
            H3mObjects.Object.LIGHTHOUSE,
            H3mObjects.Object.GRAIL -> reader.skip(4)

            H3mObjects.Object.CREATURE_GENERATOR1,
            H3mObjects.Object.CREATURE_GENERATOR2,
            H3mObjects.Object.CREATURE_GENERATOR3,
            H3mObjects.Object.CREATURE_GENERATOR4 -> reader.skip(4)

            H3mObjects.Object.PANDORAS_BOX -> objectsReader.readPandorasBox()
            H3mObjects.Object.RANDOM_DWELLING,
            H3mObjects.Object.RANDOM_DWELLING_LVL,
            H3mObjects.Object.RANDOM_DWELLING_FACTION -> objectsReader.readRandomDwelling(obj.obj)

            H3mObjects.Object.QUEST_GUARD -> objectsReader.readQuestGuard()
            H3mObjects.Object.HERO_PLACEHOLDER -> objectsReader.readHeroPlaceholder()

            H3mObjects.Object.HOTA_COLLECTIBLE -> {
                val collectibleId = H3mObjects.HotACollectible.fromInt(obj.def.objectClassSubId)
                when (collectibleId) {
                    H3mObjects.HotACollectible.ANCIENT_LAMP -> {
                        reader.readInt() // content
                        reader.readBytes(4) // unknown
                        reader.readInt() // amount
                        reader.readBytes(6) // unknown
                    }

                    H3mObjects.HotACollectible.SEA_BARREL -> {
                        reader.readInt() // contents
                        reader.readBytes(4) // unknown
                        reader.readInt() // amount
                        reader.readByte() // resource
                        reader.readBytes(5) // unknown
                    }

                    H3mObjects.HotACollectible.JETSAM -> {
                        reader.readInt() // contents
                        reader.readInt() // unknown
                    }

                    H3mObjects.HotACollectible.VIAL_OF_MANA -> {
                        reader.readInt() // contents
                        reader.readBytes(4) // unknown
                    }
                }
            }

            H3mObjects.Object.BLACK_MARKET -> {
                if (h3m.hotaVersion >= 4) {
                    reader.readBytes(7 * 4)
                }
            }

            H3mObjects.Object.CREATURE_BANK,
            H3mObjects.Object.DERELICT_SHIP,
            H3mObjects.Object.DRAGON_UTOPIA,
            H3mObjects.Object.CRYPT,
            H3mObjects.Object.SHIPWRECK -> {
                if (h3m.hotaVersion >= 3) {
                    reader.readInt() // variant
                    reader.readBool() // upgraded
                    val artsCount = reader.readInt()
                    reader.skip(artsCount * 4)
                }
            }

            H3mObjects.Object.UNIVERSITY -> {
                if (h3m.hotaVersion >= 4) {
                    reader.skip(8)
                }
            }

            H3mObjects.Object.FLOTSAM -> {
                reader.readInt() // floatsam
                reader.skip(4) // unknown
            }

            H3mObjects.Object.CORPSE,
            H3mObjects.Object.SEA_CHEST,
            H3mObjects.Object.SHIPWRECK_SURVIVOR,
            H3mObjects.Object.TREASURE_CHEST,
            H3mObjects.Object.TREE_OF_KNOWLEDGE -> {
                if (h3m.hotaVersion >= 4) {
                    reader.readInt()
                    reader.readInt()
                }
            }

            H3mObjects.Object.WARRIORS_TOMB,
            H3mObjects.Object.PYRAMID-> {
                reader.readInt()
                reader.readInt()
            }

            H3mObjects.Object.BORDER_GATE -> {
                if (obj.def.objectClassSubId == 1000) {
                    objectsReader.readQuestGuard()
                } else if (obj.def.objectClassSubId == 1001) {
                    reader.readInt() // content
                    reader.readInt() // artifact
                    reader.readInt() // amount
                    reader.readByte() // resource
                    reader.skip(5) // unknown
                }
            }

            H3mObjects.Object.CAMPFIRE -> {
                reader.skip(8)
                reader.skip(8)
                reader.skip(2)
            }

            else -> Unit
        }

        return obj
    }

    private fun readObjects(): MutableList<H3m.Object> {
        val objects = mutableListOf<H3m.Object>()
        val objectsReader = H3mObjects(h3m, reader)
        val objectsCount = reader.readInt() - 1
        for (objectIndex in 0..objectsCount) {
            try {
                objects.add(readObject(objectsReader))
            } catch (e: Exception) {
                println("index: $objectIndex / $objectsCount")
                println("prev item: ${objects[objectIndex - 1].obj}")
                println(
                    "Last items: ${
                        objects.takeLast(20).reversed().joinToString(", ") { it.obj.name }
                    }"
                )
                println("Error $e")
                throw e
            }
        }

        return objects
    }
}