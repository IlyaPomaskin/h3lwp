package com.homm3.livewallpaper.parser.h3m

import com.homm3.livewallpaper.parser.BinaryReader

enum class H3mObjectType(val value: Int) {
    NO_OBJ(-1),
    ALTAR_OF_SACRIFICE(2),
    ANCHOR_POINT(3),
    ARENA(4),
    ARTIFACT(5),
    PANDORAS_BOX(6),
    BLACK_MARKET(7),
    BOAT(8),
    BORDERGUARD(9),
    KEYMASTER(10),
    BUOY(11),
    CAMPFIRE(12),
    CARTOGRAPHER(13),
    SWAN_POND(14),
    COVER_OF_DARKNESS(15),
    CREATURE_BANK(16),
    CREATURE_GENERATOR1(17),
    CREATURE_GENERATOR2(18),
    CREATURE_GENERATOR3(19),
    CREATURE_GENERATOR4(20),
    CURSED_GROUND1(21),
    CORPSE(22),
    MARLETTO_TOWER(23),
    DERELICT_SHIP(24),
    DRAGON_UTOPIA(25),
    EVENT(26),
    EYE_OF_MAGI(27),
    FAERIE_RING(28),
    FLOTSAM(29),
    FOUNTAIN_OF_FORTUNE(30),
    FOUNTAIN_OF_YOUTH(31),
    GARDEN_OF_REVELATION(32),
    GARRISON(33),
    HERO(34),
    HILL_FORT(35),
    GRAIL(36),
    HUT_OF_MAGI(37),
    IDOL_OF_FORTUNE(38),
    LEAN_TO(39),
    LIBRARY_OF_ENLIGHTENMENT(41),
    LIGHTHOUSE(42),
    MONOLITH_ONE_WAY_ENTRANCE(43),
    MONOLITH_ONE_WAY_EXIT(44),
    MONOLITH_TWO_WAY(45),
    MAGIC_PLAINS1(46),
    SCHOOL_OF_MAGIC(47),
    MAGIC_SPRING(48),
    MAGIC_WELL(49),
    MERCENARY_CAMP(51),
    MERMAID(52),
    MINE(53),
    MONSTER(54),
    MYSTICAL_GARDEN(55),
    OASIS(56),
    OBELISK(57),
    REDWOOD_OBSERVATORY(58),
    OCEAN_BOTTLE(59),
    PILLAR_OF_FIRE(60),
    STAR_AXIS(61),
    PRISON(62),
    PYRAMID(63),
    WOG_OBJECT(63),
    RALLY_FLAG(64),
    RANDOM_ART(65),
    RANDOM_TREASURE_ART(66),
    RANDOM_MINOR_ART(67),
    RANDOM_MAJOR_ART(68),
    RANDOM_RELIC_ART(69),
    RANDOM_HERO(70),
    RANDOM_MONSTER(71),
    RANDOM_MONSTER_L1(72),
    RANDOM_MONSTER_L2(73),
    RANDOM_MONSTER_L3(74),
    RANDOM_MONSTER_L4(75),
    RANDOM_RESOURCE(76),
    RANDOM_TOWN(77),
    REFUGEE_CAMP(78),
    RESOURCE(79),
    SANCTUARY(80),
    SCHOLAR(81),
    SEA_CHEST(82),
    SEER_HUT(83),
    CRYPT(84),
    SHIPWRECK(85),
    SHIPWRECK_SURVIVOR(86),
    SHIPYARD(87),
    SHRINE_OF_MAGIC_INCANTATION(88),
    SHRINE_OF_MAGIC_GESTURE(89),
    SHRINE_OF_MAGIC_THOUGHT(90),
    SIGN(91),
    SIRENS(92),
    SPELL_SCROLL(93),
    STABLES(94),
    TAVERN(95),
    TEMPLE(96),
    DEN_OF_THIEVES(97),
    TOWN(98),
    TRADING_POST(99),
    LEARNING_STONE(100),
    TREASURE_CHEST(101),
    TREE_OF_KNOWLEDGE(102),
    SUBTERRANEAN_GATE(103),
    UNIVERSITY(104),
    WAGON(105),
    WAR_MACHINE_FACTORY(106),
    SCHOOL_OF_WAR(107),
    WARRIORS_TOMB(108),
    WATER_WHEEL(109),
    WATERING_HOLE(110),
    WHIRLPOOL(111),
    WINDMILL(112),
    WITCH_HUT(113),
    HOLE(124),
    RANDOM_MONSTER_L5(162),
    RANDOM_MONSTER_L6(163),
    RANDOM_MONSTER_L7(164),
    BORDER_GATE(212),
    FREELANCERS_GUILD(213),
    HERO_PLACEHOLDER(214),
    QUEST_GUARD(215),
    RANDOM_DWELLING(216),
    RANDOM_DWELLING_LVL(217),
    RANDOM_DWELLING_FACTION(218),
    GARRISON2(219),
    ABANDONED_MINE(220),
    TRADING_POST_SNOW(221),
    CLOVER_FIELD(222),
    CURSED_GROUND2(223),
    EVIL_FOG(224),
    FAVORABLE_WINDS(225),
    FIERY_FIELDS(226),
    HOLY_GROUNDS(227),
    LUCID_POOLS(228),
    MAGIC_CLOUDS(229),
    MAGIC_PLAINS2(230),
    ROCKLANDS(231);

    companion object {
        fun fromInt(value: Int?): H3mObjectType {
            return entries.find { it.value == value } ?: NO_OBJ
        }
    }
}

enum class SeerHutRewardType(val id: Int) {
    NOTHING(0),
    EXPERIENCE(1),
    MANA_POINTS(2),
    MORALE_BONUS(3),
    LUCK_BONUS(4),
    RESOURCES(5),
    PRIMARY_SKILL(6),
    SECONDARY_SKILL(7),
    ARTIFACT(8),
    SPELL(9),
    CREATURE(10);

    companion object {
        fun fromInt(value: Int?): SeerHutRewardType {
            return entries.find { it.id == value }
                ?: throw IllegalArgumentException("Unknown SeerHutRewardType: $value")
        }
    }
}

class H3mObjectDataReader(private val version: H3mVersion, private val stream: BinaryReader) {

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
    }

    private fun loadArtifactsOfHero() {
        if (!stream.readBool()) return

        for (j in 0..15) {
            loadArtifactToSlot()
        }
        if (version === H3mVersion.SOD) {
            loadArtifactToSlot()
        }
        loadArtifactToSlot()
        if (version !== H3mVersion.ROE) {
            loadArtifactToSlot()
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
                stream.skip(artNum * 2)
            }
            6 -> {
                val typeNum = stream.readByte()
                stream.skip(typeNum * 2 * 2)
            }
            7 -> stream.skip(7 * 4)
            8, 9 -> stream.skip(1)
        }
        stream.skip(4)
        stream.readString()
        stream.readString()
        stream.readString()
    }

    fun readEvent() {
        readMessageAndGuards()
        stream.skip(4)
        stream.skip(4)
        stream.skip(1)
        stream.skip(1)
        readResources()
        stream.skip(4)
        stream.skip(stream.readByte() * 2)
        stream.skip(stream.readByte() * if (version === H3mVersion.ROE) 1 else 2)
        stream.skip(stream.readByte())
        readCreatureSet(stream.readByte())
        stream.skip(8)
        stream.skip(1)
        stream.skip(1)
        stream.skip(1)
        stream.skip(4)
    }

    fun readHero() {
        if (version !== H3mVersion.ROE) {
            stream.skip(4)
        }
        stream.skip(1)
        stream.skip(1)
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
            stream.readByte()
        }
        if (stream.readBool()) {
            val skillsCount = stream.readInt()
            stream.skip(skillsCount * 2)
        }
        if (stream.readBool()) {
            readCreatureSet(7)
        }
        stream.readByte()
        loadArtifactsOfHero()
        stream.readByte()
        if (version !== H3mVersion.ROE) {
            if (stream.readBool()) {
                stream.readString()
            }
            stream.readByte()
        }
        if (version !== H3mVersion.ROE && version !== H3mVersion.AB) {
            if (stream.readBool()) {
                stream.skip(9)
            }
        } else if (version === H3mVersion.AB) {
            stream.skip(1)
        }
        if (version !== H3mVersion.ROE && version !== H3mVersion.AB) {
            if (stream.readBool()) {
                stream.skip(4)
            }
        }
        stream.skip(16)
    }

    fun readMonster() {
        if (version !== H3mVersion.ROE) {
            stream.skip(4)
        }
        stream.readShort()
        stream.readByte()
        if (stream.readBool()) {
            stream.readString()
            readResources()
            if (version === H3mVersion.ROE) {
                stream.readByte()
            } else {
                stream.readShort()
            }
        }
        stream.readByte()
        stream.readByte()
        stream.skip(2)
    }

    fun readSign() {
        stream.readString()
        stream.skip(4)
    }

    fun readSeerHut() {
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
                }
                SeerHutRewardType.SPELL -> stream.readByte()
                SeerHutRewardType.CREATURE -> {
                    stream.skip(if (version === H3mVersion.ROE) 3 else 4)
                }
                else -> {}
            }
            stream.skip(2)
        } else {
            stream.skip(3)
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
            stream.readInt()
        }
    }

    fun readResource() {
        readMessageAndGuards()
        stream.readInt()
        stream.skip(4)
    }

    fun readTown() {
        if (version !== H3mVersion.ROE) {
            stream.readInt()
        }
        stream.readByte()
        if (stream.readBool()) {
            stream.readString()
        }
        if (stream.readBool()) {
            readCreatureSet(7)
        }
        stream.readByte()
        if (stream.readBool()) {
            stream.skip(6)
            stream.skip(6)
        } else {
            stream.readBool()
        }
        if (version !== H3mVersion.ROE) {
            stream.skip(9)
        }
        stream.skip(9)
        val castleEvents = stream.readInt()
        for (i in 0 until castleEvents) {
            stream.readString()
            stream.readString()
            readResources()
            stream.readByte()
            if (version == H3mVersion.SOD) {
                stream.readByte()
            }
            stream.skip(1)
            stream.skip(2)
            stream.skip(1)
            stream.skip(17)
            stream.skip(6)
            stream.skip(7 * 2)
            stream.skip(4)
        }
        if (version !== H3mVersion.ROE && version !== H3mVersion.AB) {
            stream.skip(1)
        }
        stream.skip(3)
    }

    fun readPandorasBox() {
        readMessageAndGuards()
        stream.skip(4)
        stream.skip(4)
        stream.skip(1)
        stream.skip(1)
        readResources()
        stream.skip(4)
        stream.skip(stream.readByte() * 2)
        stream.skip(stream.readByte() * if (version === H3mVersion.ROE) 1 else 2)
        stream.skip(stream.readByte())
        readCreatureSet(stream.readByte())
        stream.skip(8)
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
        stream.readByte()
        val heroTypeId = stream.readByte()
        if (heroTypeId == 255) {
            stream.skip(1)
        }
    }
}
