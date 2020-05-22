package com.homm3.livewallpaper.parser.formats


class H3mObjects(private val h3m: H3m, private val stream: Reader) {
    enum class Object(val value: Int) {
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
            fun fromInt(value: Int?): Object {
                return values().find { it.value == value }
                    ?: NO_OBJ
//                    ?: throw Exception("Unknown map object")
            }
        }
    }

    enum class SeerHutRewardType(var value: Int) {
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
    }

    private fun readCreatureSet(creaturesCount: Int) {
        for (i in 0 until creaturesCount) {
            stream.skip(if (h3m.version !== H3m.Version.ROE) 2 else 1) //creature id
            stream.skip(2) //count
        }
    }

    private fun readMessageAndGuards() {
        if (!stream.readBool()) { // hasMessage
            return
        }
        stream.readString()
        if (stream.readBool()) { // hasGuards
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
        if (h3m.version === H3m.Version.ROE) {
            stream.readByte()
        } else {
            stream.readShort()
        }
    }

    private fun loadArtifactsOfHero() {
        if (!stream.readBool()) { //has arts
            return
        }

        for (j in 0..15) {
            loadArtifactToSlot()
        }
        if (h3m.version === H3m.Version.SOD || h3m.version === H3m.Version.WOG) {
            loadArtifactToSlot()
        }
        loadArtifactToSlot() //spellbook
        if (h3m.version !== H3m.Version.ROE) {
            loadArtifactToSlot() //fifth slot
        } else {
            stream.readByte()
        }
        val artsCount = stream.readShort() //bag
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
        stream.readString() //first visit
        stream.readString() //next visit
        stream.readString() //completed
    }

    fun readEvent() {
        readMessageAndGuards()
        stream.skip(4) //exp
        stream.skip(4) //mana
        stream.skip(1) //morale
        stream.skip(1) //luck
        readResources()
        stream.skip(4) //prim skills
        stream.skip(stream.readByte() * 2) // gainedAbilities
        stream.skip(stream.readByte() * if (h3m.version === H3m.Version.ROE) 1 else 2) // gainedArts
        stream.skip(stream.readByte()) // gainedSpells
        readCreatureSet(stream.readByte()) // gainedCreatures
        stream.skip(8)
        stream.skip(1) //available for
        stream.skip(1) //computer activate
        stream.skip(1) //remove after visit
        stream.skip(4)
    }

    fun readHero() {
        if (h3m.version !== H3m.Version.ROE) {
            stream.skip(4) //id
        }
        stream.skip(1) //owner
        stream.skip(1) //sub id
        if (stream.readBool()) { //hasName
            stream.readString()
        }
        if (h3m.version !== H3m.Version.ROE && h3m.version !== H3m.Version.AB) {
            if (stream.readBool()) { //hasExp
                stream.readInt()
            }
        } else {
            stream.readInt()
        }
        if (stream.readBool()) {  //has portait
            stream.readByte()
        }
        if (stream.readBool()) { //has sec skills
            val skillsCount = stream.readInt()
            stream.skip(skillsCount * 2)
        }
        if (stream.readBool()) { //has garison
            readCreatureSet(7)
        }
        stream.readByte() //formation
        loadArtifactsOfHero()
        stream.readByte() //patrol radius
        if (h3m.version !== H3m.Version.ROE) {
            if (stream.readBool()) { //custom bio
                stream.readString()
            }
            stream.readByte() //sex
        }
        if (h3m.version !== H3m.Version.ROE && h3m.version !== H3m.Version.AB) {
            if (stream.readBool()) { //custom spells
                stream.skip(9)
            }
        } else if (h3m.version === H3m.Version.AB) {
            stream.skip(1) //one spell?
        }
        if (h3m.version !== H3m.Version.ROE && h3m.version !== H3m.Version.AB) {
            //prim skills
            if (stream.readBool()) {
                stream.skip(4)
            }
        }
        stream.skip(16)
    }

    fun readMonster() {
        if (h3m.version !== H3m.Version.ROE) {
            stream.skip(4)
        }
        stream.readShort() //count
        stream.readByte() //character
        if (stream.readBool()) { //has msg
            stream.readString()
            readResources()
            if (h3m.version === H3m.Version.ROE) {
                stream.readByte()
            } else {
                stream.readShort()
            }
        }
        stream.readByte() //never fless
        stream.readByte() //not grown
        stream.skip(2)
    }

    fun readSign() {
        stream.readString()
        stream.skip(4)
    }

    fun readSeerHut() {
        var missionType = 0
        if (h3m.version !== H3m.Version.ROE) {
            missionType = stream.readByte()
            readQuest(missionType)
        } else {
            val artId = stream.readByte()
            missionType = if (artId != 255) 1 else 0
        }

        if (missionType > 0) {
            when (SeerHutRewardType.values()[stream.readByte()]) {
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
                    if (h3m.version === H3m.Version.ROE) {
                        stream.readByte()
                    } else {
                        stream.readShort()
                    }
                }
                SeerHutRewardType.SPELL -> stream.readByte()
                SeerHutRewardType.CREATURE -> {
                    stream.skip(if (h3m.version === H3m.Version.ROE) 4 else 3)
                }
            }
            stream.skip(2)
        } else {
            stream.skip(3)
        }
    }

    fun readWitchHut() {
        if (h3m.version != H3m.Version.ROE) {
            stream.skip(4)
        }
    }

    fun readScholar() {
        stream.skip(2)
        stream.skip(6)
    }

    fun readGarrison() {
        stream.skip(1);
        stream.skip(3);
        readCreatureSet(7);
        if (h3m.version != H3m.Version.ROE) {
            stream.readBool();
        }
        stream.skip(8);
    }

    fun readArtifact(obj: Object?) {
        readMessageAndGuards()
        if (obj === Object.SPELL_SCROLL) {
            stream.readInt()
        }
    }

    fun readResource() {
        readMessageAndGuards()
        stream.readInt() // gold amount
        stream.skip(4)
    }

    fun readTown() {
        if (h3m.version !== H3m.Version.ROE) {
            stream.readInt() //town identifier
        }
        stream.readByte() //owner
        if (stream.readBool()) {   //has name
            stream.readString()
        }
        if (stream.readBool()) { //has garison
            readCreatureSet(7)
        }
        stream.readByte() //formation
        if (stream.readBool()) { //has custom buildings
            stream.skip(6) //builtBuildings
            stream.skip(6) //forbiddenBuildings
        } else {
            stream.readBool() //has fort
        }
        if (h3m.version !== H3m.Version.ROE) {
            stream.skip(9) //spells?
        }
        stream.skip(9) //more spells?
        val castleEvents = stream.readInt()
        for (i in 0 until castleEvents) {
            stream.readString() //name
            stream.readString() //message
            readResources()
            stream.readByte() // players
            if (h3m.version == H3m.Version.SOD) {
                stream.readByte() //humanAffected
            }
            stream.skip(1) //computerAffected
            stream.skip(2) //firstOccurence
            stream.skip(1) //nextOccurence
            stream.skip(17) //gap
            stream.skip(6) //new buildings
            stream.skip(7 * 2) //creatures
            stream.skip(4)
        }
        if (h3m.version !== H3m.Version.ROE && h3m.version !== H3m.Version.AB) {
            stream.skip(1) //alignment
        }
        stream.skip(3)
    }

    fun readPandorasBox() {
        readMessageAndGuards()
        stream.skip(4) //exp
        stream.skip(4) //mana
        stream.skip(1) //morale
        stream.skip(1) //luck
        readResources()
        stream.skip(4) //prim skills
        stream.skip(stream.readByte() * 2) // gainedAbilitiesPandora
        stream.skip(stream.readByte() * if (h3m.version === H3m.Version.ROE) 1 else 2) // gainedArtsPandora
        stream.skip(stream.readByte()) // gainedSpellsPandora
        readCreatureSet(stream.readByte()) // gainedCreaturesPandora
        stream.skip(8)
    }

    fun readRandomDwelling(obj: Object?) {
        stream.skip(4)
        if (obj === Object.RANDOM_DWELLING || obj === Object.RANDOM_DWELLING_LVL) {
            val castleIndex = stream.readInt() // dwelling faction same as a town #castleIndex faction
            if (castleIndex == 0) {
                stream.readShort() // allowedTowns
            }
        }
        if (obj === Object.RANDOM_DWELLING || obj === Object.RANDOM_DWELLING_FACTION) {
            stream.readByte() // min lvl
            stream.readByte() // max lvl
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