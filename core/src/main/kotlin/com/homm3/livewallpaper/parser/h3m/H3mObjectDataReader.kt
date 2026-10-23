package com.homm3.livewallpaper.parser.h3m

import com.homm3.livewallpaper.parser.BinaryReader

class H3mObjectDataReader(
    private val version: H3mVersion,
    private val stream: BinaryReader,
    private val hotaSubVersion: Int = 0
) {

    private fun readCreatureSet(count: Int) {
        for (i in 0 until count) {
            stream.skip(if (version !== H3mVersion.ROE) 2 else 1)
            stream.skip(2)
        }
    }

    private fun readMessageAndGuards() {
        if (!stream.readBool()) return
        stream.readString()
        if (stream.readBool()) {
            readCreatureSet(7)
        }
        stream.skip(4)
    }

    private fun readResources() {
        for (i in 0..6) {
            stream.readInt()
        }
    }

    private fun loadArtifactToSlot() {
        if (version === H3mVersion.ROE) {
            stream.readByte()
        } else {
            stream.readShort()
        }
        if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
            stream.readShort() // scroll spell ID
        }
    }

    private fun loadArtifactsOfHero() {
        if (!stream.readBool()) return

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
            stream.readByte()
        }
        val artsCount = stream.readShort()
        for (h in 0 until artsCount) {
            loadArtifactToSlot()
        }
    }

    private fun readQuest(missionType: Int) {
        when (missionType) {
            0 -> return
            2 -> stream.skip(4)
            1, 3, 4 -> stream.skip(4)
            5 -> {
                val artNum = stream.readByte()
                val artSize = if (version == H3mVersion.HOTA && hotaSubVersion >= 5) 4 else 2
                stream.skip(artNum * artSize)
            }
            6 -> {
                val typeNum = stream.readByte()
                stream.skip(typeNum * 2 * 2)
            }
            7 -> stream.skip(7 * 4)
            8, 9 -> stream.skip(1)
            10 -> {
                // HOTA_MULTI quest type
                val missionSubId = stream.readInt()
                when (missionSubId) {
                    0 -> {
                        // HOTA_HERO_CLASS: bitmask with size prefix
                        val classesCount = stream.readInt()
                        val classesBytes = (classesCount + 7) / 8
                        stream.skip(classesBytes)
                    }
                    1 -> stream.skip(4) // HOTA_REACH_DATE
                    2 -> stream.skip(4) // HOTA_GAME_DIFFICULTY
                    3 -> stream.skip(5) // HOTA_SCRIPTED: scriptID(4) + unknown(1)
                }
            }
        }
        stream.skip(4)
        stream.readString()
        stream.readString()
        stream.readString()
    }

    private fun readBoxContent() {
        readMessageAndGuards()
        stream.skip(4) // heroExperience
        stream.skip(4) // manaDiff
        stream.skip(1) // morale
        stream.skip(1) // luck
        readResources()
        stream.skip(4) // primarySkills
        stream.skip(stream.readByte() * 2) // gained abilities
        val artSize = when {
            version === H3mVersion.ROE -> 1
            version == H3mVersion.HOTA && hotaSubVersion >= 5 -> 4
            else -> 2
        }
        stream.skip(stream.readByte() * artSize) // gained artifacts (+ scroll spell for HotA5+)
        stream.skip(stream.readByte()) // gained spells
        readCreatureSet(stream.readByte()) // gained creatures
        stream.skip(8) // reserved
    }

    private fun readBoxHotaContent() {
        if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
            stream.skip(8) // movementMode(4) + movementAmount(4)
        }
        if (version == H3mVersion.HOTA && hotaSubVersion >= 6) {
            stream.skip(4) // allowedDifficultiesMask
        }
        if (version == H3mVersion.HOTA && hotaSubVersion >= 9) {
            val usesEventSystem = stream.readBool()
            if (usesEventSystem) {
                stream.skip(5) // eventID(4) + syncObjects(1)
            }
        }
    }

    fun readEvent() {
        readBoxContent()
        stream.skip(1) // availableFor
        stream.skip(1) // computerActivate
        stream.skip(1) // removeAfterVisit
        stream.skip(4) // reserved
        if (version == H3mVersion.HOTA && hotaSubVersion >= 3) {
            stream.skip(1) // humanActivate
        }
        readBoxHotaContent()
    }

    fun readHero() {
        if (version !== H3mVersion.ROE) {
            stream.skip(4)
        }
        stream.skip(1) // owner
        stream.skip(1) // heroType
        if (stream.readBool()) {
            stream.readString()
        }
        if (version !== H3mVersion.ROE && version !== H3mVersion.AB) {
            if (stream.readBool()) {
                stream.readInt()
            }
        } else {
            stream.readInt()
        }
        if (stream.readBool()) {
            stream.readByte() // portrait
        }
        if (stream.readBool()) {
            val skillsCount = stream.readInt()
            stream.skip(skillsCount * 2)
        }
        if (stream.readBool()) {
            readCreatureSet(7)
        }
        stream.readByte() // formation
        loadArtifactsOfHero()
        stream.readByte() // patrolRadius
        if (version !== H3mVersion.ROE) {
            if (stream.readBool()) {
                stream.readString() // biography
            }
            stream.readByte() // gender
        }
        if (version !== H3mVersion.ROE && version !== H3mVersion.AB) {
            if (stream.readBool()) {
                stream.skip(9) // spells bitmask
            }
        } else if (version === H3mVersion.AB) {
            stream.skip(1) // single spell byte
        }
        if (version !== H3mVersion.ROE && version !== H3mVersion.AB) {
            if (stream.readBool()) {
                stream.skip(4) // primary skills
            }
        }
        stream.skip(16) // placeholder

        if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
            stream.skip(6) // alwaysAddSkills (bool) + cannotGainXP (bool) + level (int32)
        }
    }

    fun readMonster() {
        if (version !== H3mVersion.ROE) {
            stream.skip(4)
        }
        stream.readShort() // count
        stream.readByte() // character
        if (stream.readBool()) {
            stream.readString() // message
            readResources()
            if (version === H3mVersion.ROE) {
                stream.readByte() // gained artifact
            } else {
                stream.readShort() // gained artifact (2 bytes)
            }
        }
        stream.readByte() // neverFlees
        stream.readByte() // notGrowingTeam
        stream.skip(2) // reserved

        if (version == H3mVersion.HOTA && hotaSubVersion >= 3) {
            stream.skip(17) // agression(4) + joinOnlyForMoney(1) + joiningPercentage(4) + upgradedStackPresence(4) + stacksCount(4)
        }
        if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
            stream.skip(5) // sizeByValue(1) + targetValue(4)
        }
    }

    fun readSign() {
        stream.readString()
        stream.skip(4)
    }

    fun readSeerHut() {
        var questsCount = 1
        if (version == H3mVersion.HOTA && hotaSubVersion >= 3) {
            questsCount = stream.readInt()
        }

        for (q in 0 until questsCount) {
            readSeerHutQuest()
        }

        if (version == H3mVersion.HOTA && hotaSubVersion >= 3) {
            val repeatableQuestsCount = stream.readInt()
            for (q in 0 until repeatableQuestsCount) {
                readSeerHutQuest()
            }
        }

        stream.skip(2) // reserved
    }

    private fun readSeerHutQuest() {
        var missionType: Int
        if (version !== H3mVersion.ROE) {
            missionType = stream.readByte()
            readQuest(missionType)
        } else {
            val artId = stream.readByte()
            missionType = if (artId != 255) 1 else 0
        }

        if (missionType > 0) {
            when (SeerHutRewardType.fromInt(stream.readByte())) {
                SeerHutRewardType.EXPERIENCE -> stream.readInt()
                SeerHutRewardType.MANA_POINTS -> stream.readInt()
                SeerHutRewardType.MORALE_BONUS -> stream.readByte()
                SeerHutRewardType.LUCK_BONUS -> stream.readByte()
                SeerHutRewardType.RESOURCES -> {
                    stream.readByte()
                    stream.readInt()
                }
                SeerHutRewardType.PRIMARY_SKILL -> {
                    stream.readByte()
                    stream.readByte()
                }
                SeerHutRewardType.SECONDARY_SKILL -> {
                    stream.readByte()
                    stream.readByte()
                }
                SeerHutRewardType.ARTIFACT -> {
                    if (version === H3mVersion.ROE) {
                        stream.readByte()
                    } else {
                        stream.readShort()
                    }
                    if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
                        stream.readShort() // scroll spell ID
                    }
                }
                SeerHutRewardType.SPELL -> stream.readByte()
                SeerHutRewardType.CREATURE -> {
                    stream.skip(if (version === H3mVersion.ROE) 3 else 4)
                }
                else -> {}
            }
        } else {
            stream.skip(1) // skip 1 byte for no-quest (missionType==255 case reads 1 zero byte)
        }
    }

    fun readWitchHut() {
        if (version != H3mVersion.ROE) {
            stream.skip(4)
        }
    }

    fun readScholar() {
        stream.skip(2)
        stream.skip(6)
    }

    fun readGarrison() {
        stream.skip(1)
        stream.skip(3)
        readCreatureSet(7)
        if (version != H3mVersion.ROE) {
            stream.readBool()
        }
        stream.skip(8)
    }

    fun readArtifact(objType: H3mObjectType?) {
        readMessageAndGuards()
        if (objType === H3mObjectType.SPELL_SCROLL) {
            stream.readInt() // scroll spell ID
        } else if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
            stream.skip(5) // pickupMode(4) + pickupFlags(1)
        }
    }

    fun readResource() {
        readMessageAndGuards()
        stream.readInt()
        stream.skip(4)
    }

    fun readTown() {
        if (version !== H3mVersion.ROE) {
            stream.readInt() // identifier
        }
        stream.readByte() // owner
        if (stream.readBool()) {
            stream.readString() // name
        }
        if (stream.readBool()) {
            readCreatureSet(7) // garrison
        }
        stream.readByte() // formation
        if (stream.readBool()) { // hasCustomBuildings
            stream.skip(6) // builtBuildings bitmask
            stream.skip(6) // forbiddenBuildings bitmask
        } else {
            stream.readBool() // hasFort
        }
        if (version !== H3mVersion.ROE) {
            stream.skip(9) // obligatorySpells bitmask
        }
        stream.skip(9) // possibleSpells bitmask

        if (version == H3mVersion.HOTA && hotaSubVersion >= 1) {
            stream.skip(1) // spellResearchAllowed
        }
        if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
            val specialBuildingsSize = stream.readInt()
            stream.skip(specialBuildingsSize) // 1 byte per special building
        }

        val castleEvents = stream.readInt()
        for (i in 0 until castleEvents) {
            readTownEvent()
        }

        if (version !== H3mVersion.ROE && version !== H3mVersion.AB) {
            stream.skip(1) // alignment
        }
        stream.skip(3) // reserved
    }

    private fun readTownEvent() {
        stream.readString() // event name
        stream.readString() // event message
        readResources()
        stream.readByte() // players bitmask
        if (version == H3mVersion.SOD || version == H3mVersion.HOTA) {
            stream.readByte() // humanAffected
        }
        stream.skip(1) // computerAffected
        stream.skip(2) // firstOccurrence
        stream.skip(2) // nextOccurrence (was stream.skip(1))
        stream.skip(16) // reserved

        if (version == H3mVersion.HOTA && hotaSubVersion >= 7) {
            stream.skip(4) // affectedDifficulties
        }
        if (version == H3mVersion.HOTA && hotaSubVersion >= 9) {
            val usesEventSystem = stream.readBool()
            if (usesEventSystem) {
                stream.skip(5) // eventID(4) + syncObjects(1)
            }
        }

        if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
            stream.skip(14) // creatureGrowth8(4) + hotaAmount(4) + hotaSpecialA(4) + hotaSpecialB(2)
        }
        if (version == H3mVersion.HOTA && hotaSubVersion >= 7) {
            stream.skip(1) // neutralAffected
        }

        stream.skip(6) // newBuildings bitmask
        stream.skip(7 * 2) // creature growth
        stream.skip(4) // reserved
    }

    fun readPandorasBox() {
        readBoxContent()
        if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
            stream.skip(1) // unknown (always 0)
        }
        readBoxHotaContent()
    }

    fun readRandomDwelling(objType: H3mObjectType?) {
        stream.skip(4)
        if (objType === H3mObjectType.RANDOM_DWELLING || objType === H3mObjectType.RANDOM_DWELLING_LVL) {
            val castleIndex = stream.readInt()
            if (castleIndex == 0) {
                stream.readShort()
            }
        }
        if (objType === H3mObjectType.RANDOM_DWELLING || objType === H3mObjectType.RANDOM_DWELLING_FACTION) {
            stream.readByte()
            stream.readByte()
        }
    }

    fun readQuestGuard() {
        val missionType = stream.readByte()
        readQuest(missionType)
    }

    fun readHeroPlaceholder() {
        stream.readByte() // owner
        val heroTypeId = stream.readByte()
        if (heroTypeId == 255) {
            stream.skip(1) // power rank
        }
        if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
            stream.skip(1) // customizedStartingUnits (bool)
            stream.skip(7 * (4 + 4)) // 7 stacks: amount(int32) + creatureId(int32)
            val artifactsToGive = stream.readInt()
            stream.skip(artifactsToGive * 4) // artifact IDs (int32 each)
        }
    }

    fun readBorderGate(subId: Int) {
        when (subId) {
            1000 -> readQuestGuard() // Quest Gate
            1001 -> { // HotA Grave
                if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
                    stream.skip(18) // content(4) + data/garbage(14)
                }
            }
            // else: regular border gate, no data
        }
    }

    fun readBank() {
        if (version == H3mVersion.HOTA && hotaSubVersion >= 3) {
            stream.readInt() // guardsPresetIndex
            stream.readByte() // upgradedStackPresence
            val artNumber = stream.readInt()
            stream.skip(artNumber * 4) // artifact IDs (32-bit each)
        }
    }

    fun readAbandonedMine() {
        stream.skip(4) // abandonedMineResources bitmask (4 bytes)
        if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
            stream.skip(13) // hasCustomGuards(1) + creature/unused(4) + minAmount/unused(4) + maxAmount/unused(4)
        }
    }

    fun readHotaCustomObject(objectType: H3mObjectType, subId: Int) {
        if (version != H3mVersion.HOTA) return
        // Most HotA custom objects only have data with subVer >= 5
        // Reference: VCMI readRewardWithGarbage, readLeanTo, readRewardWithAmount, readGeneric, readHotaTrapperLodge
        when (objectType) {
            H3mObjectType.HOTA_CUSTOM_OBJECT_1 -> readHotaCustomObject1(subId)
            H3mObjectType.HOTA_CUSTOM_OBJECT_2 -> readHotaCustomObject2(subId)
            H3mObjectType.HOTA_CUSTOM_OBJECT_3 -> readHotaCustomObject3(subId)
            else -> {}
        }
    }

    // HotA5+ adds variable-length data to many object types that were "no data" in SOD
    fun readHotaRewardShort() {
        // readRewardWithGarbage / readRewardWithArtifact / readPyramid pattern: 8 bytes
        if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
            stream.skip(8) // content(4) + data/garbage(4)
        }
    }

    fun readHotaRewardLong() {
        // readLeanTo / readCampfire / readWagon / readRewardWithAmount pattern: 18 bytes
        if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
            stream.skip(18) // content(4) + data/garbage(14)
        }
    }

    fun readBlackMarket() {
        if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
            stream.skip(28) // 7 * (artifact(2) + scrollSpell(2))
        }
    }

    fun readUniversity() {
        if (version == H3mVersion.HOTA && hotaSubVersion >= 5) {
            stream.skip(8) // customized(4) + skillsBitmask(4)
        }
    }

    private fun readHotaCustomObject1(subId: Int) {
        // subid 0 = Ancient Lamp (readRewardWithAmount): 4+14=18 bytes
        // subid 1 = Sea Barrel (readLeanTo): 4+14=18 bytes
        // subid 2,3 = Jetsam, Vial of Mana (readRewardWithGarbage): 4+4=8 bytes
        if (hotaSubVersion >= 5) {
            when (subId) {
                0, 1 -> stream.skip(18) // content(4) + data/garbage(14)
                else -> stream.skip(8) // content(4) + garbage(4)
            }
        }
    }

    private fun readHotaCustomObject2(subId: Int) {
        // subid 0 = Seafaring Academy (readUniversity)
        if (hotaSubVersion >= 5 && subId == 0) {
            stream.skip(8) // customized(4) + skillsBitmask(4)
        }
        // other subids: readGeneric = no data
    }

    private fun readHotaCustomObject3(subId: Int) {
        // subid 12 = Trapper Lodge
        if (hotaSubVersion >= 9 && subId == 12) {
            stream.skip(16) // content(4) + goldAmount(4) + creatureAmount(4) + creatureType(4)
        }
        // other subids: readGeneric = no data
    }
}
