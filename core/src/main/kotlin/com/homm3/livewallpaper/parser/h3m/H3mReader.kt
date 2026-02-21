package com.homm3.livewallpaper.parser.h3m

import com.homm3.livewallpaper.parser.BinaryReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.BitSet
import java.util.zip.GZIPInputStream
import kotlin.math.pow

class H3mReader(stream: InputStream) {
    private val reader = BinaryReader(readWholeStream(GZIPInputStream(stream)))
    private lateinit var version: H3mVersion

    private fun readWholeStream(input: InputStream): ByteArrayInputStream {
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

    fun read(): H3mMap {
        version = H3mVersion.fromInt(reader.readInt())
        val header = readHeader()
        val tiles = readTerrain(header)
        val defs = readDefs()
        val objects = readObjects(defs)
        return H3mMap(version, header, tiles, defs, objects)
    }

    private fun readHeader(): H3mHeader {
        reader.readByte() // hasPlayers
        val size = reader.readInt()
        val hasUnderground = reader.readBool()
        reader.readString() // title
        reader.readString() // description
        reader.readByte() // difficulty

        if (version != H3mVersion.ROE) {
            reader.readByte() // levelLimit
        }

        readPlayerInfo()
        readVictoryLossConditions()
        readTeamInfo()
        readAllowedHeroes()
        readDisposedHeroes()
        readAllowedArtifacts()
        readAllowedSpellsAbilities()
        readRumors()
        readPredefinedHeroes()

        return H3mHeader(size, hasUnderground)
    }

    private fun readPlayerInfo() {
        for (i in 0..7) {
            val canHumanPlay = reader.readBool()
            val canPCPlay = reader.readBool()
            if (!(canHumanPlay || canPCPlay)) {
                when (version) {
                    H3mVersion.SOD -> reader.readBytes(13)
                    H3mVersion.AB -> reader.readBytes(12)
                    H3mVersion.ROE -> reader.readBytes(6)
                }
                continue
            }

            reader.readByte() // ai behavior

            if (version === H3mVersion.SOD) {
                reader.readBool() // isTownsSet
            }

            reader.readByte() // allowedFactions
            if (version != H3mVersion.ROE) {
                reader.readByte() // conflux
            }

            reader.readBool() // isRandomTown
            val hasMainTown = reader.readBool()
            if (hasMainTown) {
                if (version !== H3mVersion.ROE) {
                    reader.readBool() // generateHeroAtMainTown
                    reader.readBool() // generateHero
                }
                reader.readByte() // mainTownX
                reader.readByte() // mainTownY
                reader.readByte() // mainTownZ
            }

            reader.readBool() // has random hero
            val heroId = reader.readByte()
            if (heroId != 0xFF) {
                reader.readByte() // portrait
                reader.readString()
            }
            if (version !== H3mVersion.ROE) {
                reader.readByte()
                val heroCount = reader.readInt()
                for (k in 0 until heroCount) {
                    reader.skip(1) // hero id
                    reader.readString()
                }
            }
        }
    }

    private fun readVictoryLossConditions() {
        val victoryCondition = reader.readByte()
        if (victoryCondition != 0xFF) {
            reader.readBytes(2)
        }
        when (victoryCondition) {
            0 -> {
                reader.readByte()
                if (version !== H3mVersion.ROE) reader.readByte()
            }
            1 -> {
                reader.readByte()
                if (version !== H3mVersion.ROE) reader.readByte()
                reader.readInt()
            }
            2 -> {
                reader.readByte()
                reader.readInt()
            }
            3 -> {
                reader.readBytes(3)
                reader.readByte()
                reader.readByte()
            }
            4 -> reader.readBytes(3)
            5 -> reader.readBytes(3)
            6 -> reader.readBytes(3)
            7 -> reader.readBytes(3)
            10 -> {
                reader.readByte()
                reader.readBytes(3)
            }
        }

        when (reader.readByte()) { // lossCondition
            0 -> reader.readBytes(3)
            1 -> reader.readBytes(3)
            2 -> reader.readBytes(2)
        }
    }

    private fun readTeamInfo() {
        val teamsCount = reader.readByte()
        if (teamsCount > 0) {
            reader.skip(8)
        }
    }

    private fun readAllowedHeroes() {
        val bytesCount = if (version === H3mVersion.ROE) 16 else 20
        reader.readBytes(bytesCount)

        if (version !== H3mVersion.ROE) {
            val placeholderSize = reader.readInt()
            reader.readBytes(placeholderSize)
        }
    }

    private fun readDisposedHeroes() {
        if (version === H3mVersion.SOD) {
            val heroesCount = reader.readByte()
            for (i in 0 until heroesCount) {
                reader.readByte() // hero id
                reader.readByte() // portrait
                reader.readString() // name
                reader.readByte() // players
            }
        }

        reader.readBytes(31) // placeholder
    }

    private fun readAllowedArtifacts() {
        if (version !== H3mVersion.ROE) {
            val bytesCount = if (version === H3mVersion.AB) 17 else 18
            reader.readBytes(bytesCount)
        }
    }

    private fun readAllowedSpellsAbilities() {
        if (version === H3mVersion.SOD) {
            reader.readBytes(9)
            reader.readBytes(4)
        }
    }

    private fun readRumors() {
        val rumorsCount = reader.readInt()
        for (i in 0 until rumorsCount) {
            reader.readString()
            reader.readString()
        }
    }

    private fun loadArtifactToSlot() {
        if (version === H3mVersion.ROE) {
            reader.readByte()
        } else {
            reader.readShort()
        }
    }

    private fun loadArtifactsOfHero() {
        if (!reader.readBool()) return

        for (j in 0..15) {
            loadArtifactToSlot()
        }
        if (version === H3mVersion.SOD) {
            loadArtifactToSlot()
        }
        loadArtifactToSlot() // spellbook
        if (version !== H3mVersion.ROE) {
            loadArtifactToSlot()
        } else {
            reader.readByte()
        }
        val artsCount = reader.readShort()
        for (h in 0 until artsCount) {
            loadArtifactToSlot()
        }
    }

    private fun readPredefinedHeroes() {
        if (version !== H3mVersion.SOD) return

        for (i in 0..155) {
            if (!reader.readBool()) continue

            if (reader.readBool()) {
                reader.readInt()
            }

            if (reader.readBool()) {
                val skillsCount = reader.readInt()
                for (k in 0 until skillsCount) {
                    reader.readByte()
                    reader.readByte()
                }
            }

            loadArtifactsOfHero()

            if (reader.readBool()) {
                reader.readString()
            }

            reader.readByte() // sex

            if (reader.readBool()) {
                reader.readBytes(9)
            }

            if (reader.readBool()) {
                reader.readBytes(4)
            }
        }
    }

    private fun readTerrain(header: H3mHeader): List<H3mTile> {
        val undergroundMultiplier = if (header.hasUnderground) 2 else 1
        val size = header.size.toDouble().pow(2.0).toInt() * undergroundMultiplier

        return (0 until size).map { readTile() }
    }

    private fun readTile(): H3mTile {
        val terrain = reader.readByte()
        val terrainIndex = reader.readByte()
        val river = reader.readByte()
        val riverIndex = reader.readByte()
        val road = reader.readByte()
        val roadIndex = reader.readByte()
        val mirrorConfig = parseMirrorConfig(reader.readByte())
        return H3mTile(terrain, terrainIndex, river, riverIndex, road, roadIndex, mirrorConfig)
    }

    private fun parseMirrorConfig(input: Int): BitSet {
        var value = input
        val bits = BitSet()
        var index = 0
        while (value != 0) {
            if (value % 2L != 0L) {
                bits.set(index)
            }
            ++index
            value = value ushr 1
        }
        return bits
    }

    private fun readDefs(): List<H3mDef> {
        val defsCount = reader.readInt()
        return (0 until defsCount).map {
            val spriteName = reader.readString()
            val passableCells = listOf(reader.readInt(), reader.readShort())
            val activeCells = listOf(reader.readInt(), reader.readShort())
            reader.readShort() // terrainType
            reader.readShort() // terrainGroup
            val objectId = reader.readInt()
            val objectClassSubId = reader.readInt()
            reader.readByte() // objectsGroup
            val placementOrder = reader.readByte()
            reader.readBytes(16) // placeholder
            H3mDef(spriteName, passableCells, activeCells, placementOrder, objectId, objectClassSubId)
        }
    }

    private fun readObjects(defs: List<H3mDef>): List<H3mObject> {
        val objects = mutableListOf<H3mObject>()
        val objectsCount = reader.readInt() - 1
        val objectsReader = H3mObjectDataReader(version, reader)
        for (objectIndex in 0..objectsCount) {
            val x = reader.readByte()
            val y = reader.readByte()
            val z = reader.readByte()
            val index = reader.readInt()
            val def = defs[index]
            val objectType = H3mObjectType.fromInt(def.objectId)

            reader.readBytes(5)

            when (objectType) {
                H3mObjectType.EVENT -> objectsReader.readEvent()
                H3mObjectType.HERO,
                H3mObjectType.RANDOM_HERO,
                H3mObjectType.PRISON -> objectsReader.readHero()
                H3mObjectType.MONSTER,
                H3mObjectType.RANDOM_MONSTER,
                H3mObjectType.RANDOM_MONSTER_L1,
                H3mObjectType.RANDOM_MONSTER_L2,
                H3mObjectType.RANDOM_MONSTER_L3,
                H3mObjectType.RANDOM_MONSTER_L4,
                H3mObjectType.RANDOM_MONSTER_L5,
                H3mObjectType.RANDOM_MONSTER_L6,
                H3mObjectType.RANDOM_MONSTER_L7 -> objectsReader.readMonster()
                H3mObjectType.OCEAN_BOTTLE,
                H3mObjectType.SIGN -> objectsReader.readSign()
                H3mObjectType.SEER_HUT -> objectsReader.readSeerHut()
                H3mObjectType.WITCH_HUT -> objectsReader.readWitchHut()
                H3mObjectType.SCHOLAR -> objectsReader.readScholar()
                H3mObjectType.GARRISON,
                H3mObjectType.GARRISON2 -> objectsReader.readGarrison()
                H3mObjectType.ARTIFACT,
                H3mObjectType.RANDOM_ART,
                H3mObjectType.RANDOM_TREASURE_ART,
                H3mObjectType.RANDOM_MINOR_ART,
                H3mObjectType.RANDOM_MAJOR_ART,
                H3mObjectType.RANDOM_RELIC_ART,
                H3mObjectType.SPELL_SCROLL -> objectsReader.readArtifact(objectType)
                H3mObjectType.RANDOM_RESOURCE,
                H3mObjectType.RESOURCE -> objectsReader.readResource()
                H3mObjectType.RANDOM_TOWN,
                H3mObjectType.TOWN -> objectsReader.readTown()
                H3mObjectType.MINE,
                H3mObjectType.ABANDONED_MINE,
                H3mObjectType.SHRINE_OF_MAGIC_INCANTATION,
                H3mObjectType.SHRINE_OF_MAGIC_GESTURE,
                H3mObjectType.SHRINE_OF_MAGIC_THOUGHT,
                H3mObjectType.SHIPYARD,
                H3mObjectType.LIGHTHOUSE,
                H3mObjectType.GRAIL -> reader.skip(4)
                H3mObjectType.CREATURE_GENERATOR1,
                H3mObjectType.CREATURE_GENERATOR2,
                H3mObjectType.CREATURE_GENERATOR3,
                H3mObjectType.CREATURE_GENERATOR4 -> reader.skip(4)
                H3mObjectType.PANDORAS_BOX -> objectsReader.readPandorasBox()
                H3mObjectType.RANDOM_DWELLING,
                H3mObjectType.RANDOM_DWELLING_LVL,
                H3mObjectType.RANDOM_DWELLING_FACTION -> objectsReader.readRandomDwelling(objectType)
                H3mObjectType.QUEST_GUARD -> objectsReader.readQuestGuard()
                H3mObjectType.HERO_PLACEHOLDER -> objectsReader.readHeroPlaceholder()
                else -> Unit
            }

            objects.add(H3mObject(x, y, z, def, objectType))
        }

        return objects
    }
}
