package com.homm3.livewallpaper.parser.formats

import com.homm3.livewallpaper.parser.formats.H3m.DefInfo
import com.homm3.livewallpaper.parser.formats.H3m.Player
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream
import kotlin.math.pow


internal class H3mReader(stream: InputStream) {
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

        if (h3m.version != H3m.Version.ROE) {
            reader.readByte() // levelLimit
        }

        header.players = readPlayerInfo()
        readVictoryLossConditions()
        readTeamInfo()
        readAllowedHeroes()
        readDisposedHeroes()
        readAllowedArtifacts()
        readAllowedSpellsAbilities()
        readRumors()
        readPredefinedHeroes()

        return header
    }

    private fun readPlayerInfo(): MutableList<Player> {
        val players = mutableListOf<Player>()

        for (i in 0..7) {
            val player = Player()
            player.playerColor = i

            val canHumanPlay = reader.readBool()
            val canPCPlay = reader.readBool()
            if (!(canHumanPlay || canPCPlay)) {
                when (h3m.version) {
                    H3m.Version.SOD -> reader.readBytes(13)
                    H3m.Version.AB -> reader.readBytes(12)
                    H3m.Version.ROE -> reader.readBytes(6)
                }
                continue
            }

            reader.readByte() //ai behavior

            if (h3m.version === H3m.Version.SOD) {
                reader.readBool() // isTownsSet
            }

            reader.readByte() // allowedFactions
            if (h3m.version != H3m.Version.ROE) {
                reader.readByte() // conflux?
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

            //has random hero
            reader.readBool()
            //main custom hero id
            val heroId = reader.readByte()
            if (heroId != 0xFF) {
                //portrait
                reader.readByte()
                reader.readString()
            }
            if (h3m.version !== H3m.Version.ROE) {
                //unknown byte
                reader.readByte()
                val heroCount: Int = reader.readInt()
                for (k in 0 until heroCount) {
                    //hero id
                    reader.skip(1)
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
            7 -> reader.readBytes(3) // BEATMONSTER coords
            10 -> {
                //TRANSPORTITEM
                reader.readByte() //obj terrain
                reader.readBytes(3) //coords
            }
        }

        when (reader.readByte()) { // lossCondition
            0 -> reader.readBytes(3) //LOSSCASTLE
            1 -> reader.readBytes(3) //LOSSHERO
            2 -> reader.readBytes(2) //TIMEEXPIRES
        }
    }

    private fun readTeamInfo() {
        val teamsCount = reader.readByte()
        if (teamsCount > 0) {
            reader.skip(8)
        }
    }

    private fun readAllowedHeroes() {
        val bytesCount = if (h3m.version === H3m.Version.ROE) 16 else 20
        reader.readBytes(bytesCount)

        if (h3m.version !== H3m.Version.ROE) {
            val placeholderSize = reader.readInt()
            reader.readBytes(placeholderSize)
        }
    }

    private fun readDisposedHeroes() {
        if (h3m.version === H3m.Version.SOD) {
            val heroesCount = reader.readByte()
            for (i in 0 until heroesCount) {
                reader.readByte() //hero id
                reader.readByte() //portrait
                reader.readString() //name
                reader.readByte() //players
            }
        }

        reader.readBytes(31) // placeholder
    }

    private fun readAllowedArtifacts() {
        // Reading allowed artifacts: 17 or 18 bytes
        if (h3m.version !== H3m.Version.ROE) {
            val bytesCount = if (h3m.version === H3m.Version.AB) 17 else 18
            reader.readBytes(bytesCount)
        }
    }

    private fun readAllowedSpellsAbilities() {
        if (h3m.version === H3m.Version.SOD) {
            reader.readBytes(9) //spells
            reader.readBytes(4) //abillities
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
        if (h3m.version !== H3m.Version.SOD) {
            return
        }

        for (i in 0..155) {
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

    private fun readObjects(): MutableList<H3m.Object> {
        val objects = mutableListOf<H3m.Object>()
        val objectsReader = H3mObjects(h3m, reader)
        val objectsCount = reader.readInt() - 1
        for (objectIndex in 0..objectsCount) {
            val obj = H3m.Object()
            obj.x = reader.readByte()
            obj.y = reader.readByte()
            obj.z = reader.readByte()
            val index = reader.readInt()
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
                else -> Unit
            }

            objects.add(obj)
        }

        return objects
    }
}