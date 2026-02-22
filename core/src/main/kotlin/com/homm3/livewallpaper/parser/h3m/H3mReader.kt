package com.homm3.livewallpaper.parser.h3m

import com.homm3.livewallpaper.parser.BinaryReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.BitSet
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.GZIPInputStream
import kotlin.math.pow

class H3mReader(stream: InputStream) {
    private val reader = BinaryReader(readWholeStream(GZIPInputStream(stream)))
    private lateinit var version: H3mVersion
    private var hotaSubVersion: Int = 0

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
        if (version == H3mVersion.HOTA) {
            hotaSubVersion = reader.readInt()
        }
        log.fine("Parsing map: version=$version, hotaSubVersion=$hotaSubVersion")
        val header = readHeader()
        log.fine("Header: size=${header.size}, underground=${header.hasUnderground}")
        logSection("readTerrain")
        val tiles = readTerrain(header)
        val terrainSample = tiles.take(20).map { "t${it.terrain}:${it.terrainIndex}" }
        val terrainDistinct = tiles.map { it.terrain }.distinct().sorted()
        log.fine("Terrain sample: $terrainSample")
        log.fine("Terrain types found: $terrainDistinct (${tiles.size} tiles)")
        logSection("readDefs")
        val defs = readDefs()
        log.fine("Defs count: ${defs.size}, first 5: ${defs.take(5).map { it.spriteName }}")
        logSection("readObjects")
        val objects = readObjects(defs)
        return H3mMap(version, hotaSubVersion, header, tiles, defs, objects)
    }

    private fun logSection(name: String) {
//        log.info("$name @ offset ${reader.position}")
    }

    private fun readHeader(): H3mHeader {
        if (version == H3mVersion.HOTA) {
            readHotaHeaderFields()
        }

        reader.readByte() // hasPlayers
        val size = reader.readInt()
        val hasUnderground = reader.readBool()
        reader.readString() // title
        reader.readString() // description
        reader.readByte() // difficulty

        if (version != H3mVersion.ROE) {
            reader.readByte() // levelLimit
        }

        logSection("readPlayerInfo")
        readPlayerInfo()
        logSection("readVictoryLossConditions")
        readVictoryLossConditions()
        logSection("readTeamInfo")
        readTeamInfo()
        logSection("readAllowedHeroes")
        readAllowedHeroes()
        logSection("readDisposedHeroes")
        readDisposedHeroes()
        logSection("readMapOptions")
        readMapOptions()
        logSection("readHotaScripts")
        readHotaScripts()
        logSection("readAllowedArtifacts")
        readAllowedArtifacts()
        logSection("readAllowedSpellsAbilities")
        readAllowedSpellsAbilities()
        logSection("readRumors")
        readRumors()
        logSection("readPredefinedHeroes")
        readPredefinedHeroes()

        return H3mHeader(size, hasUnderground)
    }

    private fun readPlayerInfo() {
        for (i in 0..7) {
            val canHumanPlay = reader.readBool()
            val canPCPlay = reader.readBool()
            if (!(canHumanPlay || canPCPlay)) {
                when (version) {
                    H3mVersion.HOTA -> reader.readBytes(13)
                    H3mVersion.SOD -> reader.readBytes(13)
                    H3mVersion.AB -> reader.readBytes(12)
                    H3mVersion.ROE -> reader.readBytes(6)
                }
                continue
            }

            reader.readByte() // ai behavior

            if (version === H3mVersion.SOD || version === H3mVersion.HOTA) {
                reader.readBool() // isTownsSet
            }

            if (version == H3mVersion.HOTA) {
                reader.readShort() // allowedFactions (2-byte bitmask)
            } else {
                reader.readByte() // allowedFactions
                if (version != H3mVersion.ROE) {
                    reader.readByte() // conflux
                }
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
            11 -> {} // HotA: eliminate all monsters - no extra data
            12 -> reader.readInt() // HotA: survive for N days
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
        if (version == H3mVersion.HOTA) {
            val heroesCount = reader.readInt()
            reader.readBytes((heroesCount + 7) / 8)
        } else {
            val bytesCount = if (version === H3mVersion.ROE) 16 else 20
            reader.readBytes(bytesCount)
        }

        if (version !== H3mVersion.ROE) {
            val placeholderCount = reader.readInt()
            reader.skip(placeholderCount) // 1 byte per hero ID
        }
    }

    private fun readDisposedHeroes() {
        if (version === H3mVersion.SOD || version === H3mVersion.HOTA) {
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

    private fun readHotaHeaderFields() {
        if (hotaSubVersion >= 8) {
            reader.skip(12) // version triplet: 3 × uint32
        }
        if (hotaSubVersion >= 1) {
            reader.skip(2) // isMirrorMap + isArenaMap
        }
        if (hotaSubVersion >= 2) {
            reader.skip(4) // terrainTypesCount
        }
        if (hotaSubVersion >= 5) {
            reader.skip(4) // townTypesCount
            reader.skip(1) // allowedDifficultiesMask
        }
        if (hotaSubVersion >= 7) {
            reader.skip(1) // canHireDefeatedHeroes
        }
        if (hotaSubVersion >= 8) {
            reader.skip(1) // forceMatchingVersion
        }
        if (hotaSubVersion >= 9) {
            reader.skip(4) // unknown int32
        }
    }

    private fun readMapOptions() {
        if (version != H3mVersion.HOTA) return

        if (hotaSubVersion >= 0) {
            reader.skip(4) // allowSpecialMonths (bool) + 3 padding bytes
        }
        if (hotaSubVersion >= 1) {
            val combinedArtCount = reader.readInt()
            val combinedArtBytes = (combinedArtCount + 7) / 8
            reader.readBytes(combinedArtBytes)
        }
        if (hotaSubVersion >= 3) {
            reader.skip(4) // roundLimit
        }
        if (hotaSubVersion >= 5) {
            reader.skip(8) // 8 × bool heroRecruitmentBlocked
        }
    }

    private fun readHotaScripts() {
        if (version != H3mVersion.HOTA || hotaSubVersion < 9) return

        val eventsSystemActive = reader.readBool()
        if (!eventsSystemActive) return

        // Read 4 event lists (hero, player, town, quest)
        repeat(4) {
            val eventsCount = reader.readInt()
            for (i in 0 until eventsCount) {
                reader.readInt() // eventID
                skipHotaScriptActions()
                reader.readString() // eventName
            }
        }

        // Next ID counters
        reader.skip(20) // 5 × int32

        // Variables
        val variablesCount = reader.readInt()
        for (i in 0 until variablesCount) {
            reader.readInt() // uniqueID
            reader.readString() // variableID
            reader.skip(2) // two bools
            reader.readInt() // initialValue
        }

        // 5 event maps
        repeat(5) {
            val mappingSize = reader.readInt()
            reader.skip(mappingSize * 4)
        }
    }

    private fun skipHotaScriptActions() {
        reader.readInt() // event type (always 1)
        reader.readByte() // unknown (always 0)

        val actionsCount = reader.readInt()
        for (j in 0 until actionsCount) {
            val actionType = reader.readInt()
            when (actionType) {
                1 -> { // CONDITIONAL_CHAIN
                    while (true) {
                        skipHotaScriptCondition()
                        skipHotaScriptActions()
                        reader.readBool() // unknown
                        val continueChain = reader.readInt()
                        if (continueChain == 0) break
                    }
                    reader.readInt() // unknown
                }
                2 -> { // SET_VARIABLE_CONDITIONAL
                    reader.readInt() // variableID
                    skipHotaScriptCondition()
                    skipHotaScriptExpression()
                    skipHotaScriptExpression()
                }
                3 -> { // MODIFY_VARIABLE
                    reader.readInt() // variableID
                    reader.readByte() // mode
                    skipHotaScriptExpressionInternal()
                }
                4 -> { // RESOURCES
                    reader.readByte() // mode
                    repeat(7) { skipHotaScriptExpression() }
                    reader.readBool() // showMessage
                }
                5 -> {} // REMOVE_CURRENT_OBJECT / FINISH_QUEST - no data
                6 -> { // SHOW_REWARDS_MESSAGE
                    reader.readString() // text
                    skipHotaScriptActions()
                }
                7 -> { // QUEST_ACTION
                    skipHotaScriptCondition()
                    reader.readString() // proposal
                    reader.readString() // progression
                    reader.readString() // completion
                    reader.readString() // hint
                    skipHotaScriptActions()
                    reader.readBool() // unknown
                }
                8 -> { // CREATURES
                    reader.readBool() // takeCreatures
                    reader.readInt() // creatureID (32-bit)
                    skipHotaScriptExpression()
                    reader.readBool() // showMessage
                }
                9 -> { // ARTIFACT
                    reader.readBool() // takeArtifact
                    reader.readInt() // artifactID (32-bit)
                    reader.readInt() // scrollSpellID (32-bit)
                    reader.readBool() // showMessage
                }
                10 -> { // CONSTRUCT_BUILDING
                    reader.readInt() // buildingID
                    reader.readShort() // unknownA
                    reader.readShort() // unknownB
                    reader.readBool() // showMessage
                }
                11 -> { // SET_QUEST_HINT
                    reader.readString() // message
                    val numberOfImages = reader.readInt()
                    for (k in 0 until numberOfImages) {
                        reader.readInt() // imageType
                        reader.readInt() // imageSubtype
                        skipHotaScriptExpression()
                    }
                    reader.readBool() // showInLog
                }
                12 -> { // SHOW_QUESTION
                    val imageShowType = reader.readByte()
                    reader.readString() // message
                    skipHotaScriptActions()
                    skipHotaScriptActions()
                    if (imageShowType == 2) {
                        skipHotaScriptActions()
                    }
                    var numberOfImages = 2
                    if (imageShowType == 0 || imageShowType == 3) {
                        numberOfImages = reader.readInt()
                    }
                    for (k in 0 until numberOfImages) {
                        reader.readInt() // imageType
                        reader.readInt() // imageSubtype
                        skipHotaScriptExpression()
                    }
                    if (imageShowType == 1 || imageShowType == 2) {
                        reader.readBool() // showOrBetweenImages
                        reader.readInt() // unknown
                    }
                }
                13 -> { // CONDITIONAL
                    skipHotaScriptCondition()
                    skipHotaScriptActions()
                    skipHotaScriptActions()
                }
                14 -> { // CREATURES_TO_HIRE
                    reader.readInt() // dwelling
                    skipHotaScriptExpression() // amount
                    reader.readInt() // unknown
                    reader.readBool() // showMessage
                }
                15 -> { // SPELL
                    reader.readInt() // spellID (32-bit)
                    reader.readBool() // showMessage
                }
                16 -> { // EXPERIENCE
                    skipHotaScriptExpression()
                    reader.readBool() // showMessage
                }
                17 -> { // SPELL_POINTS
                    skipHotaScriptExpression()
                    reader.readInt() // mode
                    reader.readBool() // showMessage
                }
                18 -> { // MOVEMENT_POINTS
                    skipHotaScriptExpression()
                    reader.readInt() // mode
                    reader.readBool() // showMessage
                }
                19 -> { // PRIMARY_SKILL
                    skipHotaScriptExpression()
                    reader.readInt() // skill
                    reader.readBool() // showMessage
                }
                20 -> { // SECONDARY_SKILL
                    reader.readInt() // masteryLevel
                    reader.readInt() // skillID (32-bit)
                    reader.readBool() // showMessage
                }
                21 -> { // LUCK
                    reader.readInt() // amount
                    reader.readBool() // showMessage
                }
                22 -> { // MORALE
                    reader.readInt() // amount
                    reader.readBool() // showMessage
                }
                23 -> { // START_COMBAT
                    for (k in 0 until 7) {
                        skipHotaScriptExpression()
                        reader.readInt() // creatureID (32-bit)
                    }
                }
                24 -> { // EXECUTE_EVENT
                    reader.readInt() // eventType
                    reader.readInt() // eventID
                }
                25 -> { // WAR_MACHINE
                    reader.readBool() // takeMachine
                    reader.readInt() // artifactID (32-bit)
                    reader.skip(4) // garbage
                    reader.readBool() // showMessage
                }
                26 -> { // SPELLBOOK
                    reader.readBool() // takeSpellbook
                    reader.skip(8) // garbage
                    reader.readBool() // showMessage
                }
                27 -> {} // DISABLE_EVENT - no data
                28 -> { // LOOP_FOR
                    skipHotaScriptActions() // loop body
                    skipHotaScriptExpression() // initial value
                    skipHotaScriptExpression() // final value
                    reader.readInt() // variableID
                }
                29 -> { // SHOW_MESSAGE
                    reader.readString() // text
                    val numberOfImages = reader.readInt()
                    for (k in 0 until numberOfImages) {
                        reader.readInt() // imageType
                        reader.readInt() // imageSubtype
                        skipHotaScriptExpression()
                    }
                }
                else -> throw IllegalArgumentException("Unknown HotA script action type: $actionType")
            }
        }
    }

    private fun skipHotaScriptCondition() {
        reader.readBool() // unknown (always true)
        skipHotaScriptConditionInternal()
    }

    private fun skipHotaScriptConditionInternal() {
        val conditionCode = reader.readInt()
        when (conditionCode) {
            0 -> reader.readBool() // CONSTANT
            1, 2 -> { // ALL_OF, ANY_OF
                val argumentsCount = reader.readInt()
                repeat(argumentsCount) { skipHotaScriptConditionInternal() }
            }
            3, 4, 5, 8, 9, 10 -> { // comparison operators
                skipHotaScriptExpression()
                skipHotaScriptExpression()
            }
            6 -> skipHotaScriptCondition() // NOT
            7 -> { // HAS_ARTIFACT
                reader.readInt() // artifactID (32-bit)
                reader.readInt() // scrollSpellID (32-bit)
            }
            11 -> reader.readInt() // CURRENT_PLAYER - playerID (32-bit)
            12 -> { // HERO_OWNER
                reader.readInt() // heroID (32-bit)
                reader.readInt() // playerID (32-bit)
            }
            14, 15 -> { // PLAYER_DEFEATED_MONSTER, PLAYER_DEFEATED_HERO
                reader.readInt() // playerID (32-bit)
                reader.readInt() // targetObjectID
            }
            16 -> { // HERO_SECONDARY_SKILL
                reader.readInt() // skillID (32-bit)
                reader.readInt() // expectedMastery
            }
            17 -> reader.readInt() // PLAYER_DEFEATED - playerID (32-bit)
            18 -> { // PLAYER_OWNS_TOWN
                reader.readInt() // playerID (32-bit)
                reader.readInt() // targetObjectID
            }
            19 -> reader.readInt() // PLAYER_IS_HUMAN - playerID (32-bit)
            20 -> { // PLAYER_STARTING_FACTION
                reader.readInt() // playerID (32-bit)
                reader.readInt() // factionID (32-bit)
            }
            21 -> {} // TOWN_IS_NEUTRAL - no data
            else -> throw IllegalArgumentException("Unknown HotA script condition: $conditionCode")
        }
    }

    private fun skipHotaScriptExpression() {
        val isExpression = reader.readBool()
        if (!isExpression) {
            reader.readInt() // raw value
            return
        }
        skipHotaScriptExpressionInternal()
    }

    private fun skipHotaScriptExpressionInternal() {
        reader.readBool() // unknown (always true)
        val expressionCode = reader.readInt()
        when (expressionCode) {
            0 -> reader.readInt() // INTEGER_VALUE
            1 -> reader.readInt() // VARIABLE_VALUE
            2 -> { // NEGATE
                reader.readInt() // unknown
                skipHotaScriptExpression()
            }
            3, 4, 6, 7, 8 -> { // arithmetic: ADD, SUBTRACT, MULTIPLY, DIVIDE, REMAINDER
                skipHotaScriptExpressionInternal()
                skipHotaScriptExpressionInternal()
            }
            5 -> { // RESOURCE
                reader.readByte() // player
                reader.readInt() // resourceID (32-bit)
            }
            9 -> reader.readInt() // CREATURE_COUNT_IN_ARMY - creatureID (32-bit)
            10 -> {} // CURRENT_DIFFICULTY - no data
            11 -> reader.readInt() // COMPARE_DIFFICULTY
            12 -> {} // CURRENT_DATE - no data
            13 -> {} // HERO_EXPERIENCE - no data
            14 -> {} // HERO_LEVEL - no data
            15 -> reader.readInt() // HERO_PRIMARY_SKILL - skillID (32-bit)
            16 -> { // RANDOM_NUMBER
                skipHotaScriptExpression()
                skipHotaScriptExpression()
            }
            17 -> { // HERO_OWNED_ARTIFACTS
                reader.readInt() // artifactID (32-bit)
                reader.readInt() // scrollSpellID (32-bit)
            }
            else -> throw IllegalArgumentException("Unknown HotA script expression: $expressionCode")
        }
    }

    private fun readAllowedArtifacts() {
        if (version !== H3mVersion.ROE) {
            if (version == H3mVersion.HOTA) {
                val artifactsCount = reader.readInt()
                reader.readBytes((artifactsCount + 7) / 8)
            } else {
                val bytesCount = if (version === H3mVersion.AB) 17 else 18
                reader.readBytes(bytesCount)
            }
        }
    }

    private fun readAllowedSpellsAbilities() {
        if (version === H3mVersion.SOD || version === H3mVersion.HOTA) {
            reader.readBytes(9) // spells bitmask
            reader.readBytes(4) // skills bitmask
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
        if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
            reader.readShort() // scroll spell ID
        }
    }

    private fun loadArtifactsOfHero() {
        if (!reader.readBool()) return

        for (j in 0..15) {
            loadArtifactToSlot()
        }
        if (version === H3mVersion.SOD || version === H3mVersion.HOTA) {
            loadArtifactToSlot() // misc5 slot
        }
        loadArtifactToSlot() // spellbook
        if (version !== H3mVersion.ROE) {
            loadArtifactToSlot() // misc4
        } else {
            reader.readByte()
        }
        val artsCount = reader.readShort()
        for (h in 0 until artsCount) {
            loadArtifactToSlot()
        }
    }

    private fun readPredefinedHeroes() {
        if (version !== H3mVersion.SOD && version !== H3mVersion.HOTA) return

        val heroesCount = if (version == H3mVersion.HOTA) reader.readInt() else 156

        for (i in 0 until heroesCount) {
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

        if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
            for (i in 0 until heroesCount) {
                reader.skip(6) // alwaysAddSkills (bool) + cannotGainXP (bool) + level (int32)
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
        val objectsReader = H3mObjectDataReader(version, reader, hotaSubVersion)
        for (objectIndex in 0..objectsCount) {
            val posBeforeObject = reader.position
            val x = reader.readByte()
            val y = reader.readByte()
            val z = reader.readByte()
            val index = reader.readInt()
            if (index < 0 || index >= defs.size) {
                log.warning("Invalid def index $index at object $objectIndex/${objectsCount + 1} @ offset $posBeforeObject, stopping object parsing")
                break
            }
            val def = defs[index]
            val objectType = H3mObjectType.fromInt(def.objectId)
//            log.info("Object $objectIndex: ($x,$y,$z) def=$index ${def.spriteName} type=$objectType subId=${def.objectClassSubId} @ offset $posBeforeObject")

            reader.readBytes(5)

            val posBeforeData = reader.position
            val parsedType = when (objectType) {
                H3mObjectType.EVENT -> { objectsReader.readEvent(); "EVENT" }
                H3mObjectType.HERO,
                H3mObjectType.RANDOM_HERO,
                H3mObjectType.PRISON -> { objectsReader.readHero(); "HERO" }
                H3mObjectType.MONSTER,
                H3mObjectType.RANDOM_MONSTER,
                H3mObjectType.RANDOM_MONSTER_L1,
                H3mObjectType.RANDOM_MONSTER_L2,
                H3mObjectType.RANDOM_MONSTER_L3,
                H3mObjectType.RANDOM_MONSTER_L4,
                H3mObjectType.RANDOM_MONSTER_L5,
                H3mObjectType.RANDOM_MONSTER_L6,
                H3mObjectType.RANDOM_MONSTER_L7 -> { objectsReader.readMonster(); "MONSTER" }
                H3mObjectType.OCEAN_BOTTLE,
                H3mObjectType.SIGN -> { objectsReader.readSign(); "SIGN" }
                H3mObjectType.SEER_HUT -> { objectsReader.readSeerHut(); "SEER_HUT" }
                H3mObjectType.WITCH_HUT -> { objectsReader.readWitchHut(); "WITCH_HUT" }
                H3mObjectType.SCHOLAR -> { objectsReader.readScholar(); "SCHOLAR" }
                H3mObjectType.GARRISON,
                H3mObjectType.GARRISON2 -> { objectsReader.readGarrison(); "GARRISON" }
                H3mObjectType.ARTIFACT,
                H3mObjectType.RANDOM_ART,
                H3mObjectType.RANDOM_TREASURE_ART,
                H3mObjectType.RANDOM_MINOR_ART,
                H3mObjectType.RANDOM_MAJOR_ART,
                H3mObjectType.RANDOM_RELIC_ART,
                H3mObjectType.SPELL_SCROLL -> { objectsReader.readArtifact(objectType); "ARTIFACT" }
                H3mObjectType.RANDOM_RESOURCE,
                H3mObjectType.RESOURCE -> { objectsReader.readResource(); "RESOURCE" }
                H3mObjectType.RANDOM_TOWN,
                H3mObjectType.TOWN -> { objectsReader.readTown(); "TOWN" }
                H3mObjectType.MINE,
                H3mObjectType.ABANDONED_MINE -> {
                    if (def.objectClassSubId < 7) { reader.skip(4); "MINE" }
                    else { objectsReader.readAbandonedMine(); "ABANDONED_MINE" }
                }
                H3mObjectType.SHRINE_OF_MAGIC_INCANTATION,
                H3mObjectType.SHRINE_OF_MAGIC_GESTURE,
                H3mObjectType.SHRINE_OF_MAGIC_THOUGHT,
                H3mObjectType.SHIPYARD,
                H3mObjectType.LIGHTHOUSE -> { reader.skip(4); "SKIP4" }
                H3mObjectType.GRAIL -> {
                    if (def.objectClassSubId < 1000) reader.skip(4) // regular grail
                    // else: HotA battle location, no data
                    "GRAIL"
                }
                H3mObjectType.CREATURE_GENERATOR1,
                H3mObjectType.CREATURE_GENERATOR2,
                H3mObjectType.CREATURE_GENERATOR3,
                H3mObjectType.CREATURE_GENERATOR4 -> { reader.skip(4); "CREATURE_GEN" }
                H3mObjectType.PANDORAS_BOX -> { objectsReader.readPandorasBox(); "PANDORAS_BOX" }
                H3mObjectType.CREATURE_BANK,
                H3mObjectType.DERELICT_SHIP,
                H3mObjectType.DRAGON_UTOPIA,
                H3mObjectType.CRYPT,
                H3mObjectType.SHIPWRECK -> { objectsReader.readBank(); "BANK" }
                H3mObjectType.RANDOM_DWELLING,
                H3mObjectType.RANDOM_DWELLING_LVL,
                H3mObjectType.RANDOM_DWELLING_FACTION -> { objectsReader.readRandomDwelling(objectType); "RANDOM_DWELLING" }
                H3mObjectType.QUEST_GUARD -> { objectsReader.readQuestGuard(); "QUEST_GUARD" }
                H3mObjectType.HERO_PLACEHOLDER -> { objectsReader.readHeroPlaceholder(); "HERO_PLACEHOLDER" }
                H3mObjectType.BORDER_GATE -> { objectsReader.readBorderGate(def.objectClassSubId); "BORDER_GATE" }
                H3mObjectType.TREASURE_CHEST,
                H3mObjectType.CORPSE,
                H3mObjectType.WARRIORS_TOMB,
                H3mObjectType.SHIPWRECK_SURVIVOR,
                H3mObjectType.SEA_CHEST,
                H3mObjectType.FLOTSAM,
                H3mObjectType.TREE_OF_KNOWLEDGE,
                H3mObjectType.PYRAMID -> { objectsReader.readHotaRewardShort(); "HOTA_REWARD_SHORT" }
                H3mObjectType.LEAN_TO,
                H3mObjectType.WAGON,
                H3mObjectType.CAMPFIRE -> { objectsReader.readHotaRewardLong(); "HOTA_REWARD_LONG" }
                H3mObjectType.BLACK_MARKET -> { objectsReader.readBlackMarket(); "BLACK_MARKET" }
                H3mObjectType.UNIVERSITY -> { objectsReader.readUniversity(); "UNIVERSITY" }
                H3mObjectType.HOTA_CUSTOM_OBJECT_1,
                H3mObjectType.HOTA_CUSTOM_OBJECT_2,
                H3mObjectType.HOTA_CUSTOM_OBJECT_3 -> { objectsReader.readHotaCustomObject(objectType, def.objectClassSubId); "HOTA_CUSTOM" }
                else -> "NONE"
            }
            val bytesConsumed = reader.position - posBeforeData
//            log.info("  -> parsed as $parsedType, consumed $bytesConsumed bytes")

            objects.add(H3mObject(x, y, z, def, objectType))
        }

        return objects
    }

    companion object {
        private val log = Logger.getLogger(H3mReader::class.java.name)
    }
}
