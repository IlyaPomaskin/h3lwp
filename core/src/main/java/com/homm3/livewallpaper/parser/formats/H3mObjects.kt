package com.homm3.livewallpaper.parser.formats


class H3mObjects(private val h3m: H3m, private val stream: Reader) {
    enum class Object(val value: Int) {
        NO_OBJ(-1),
        NOTHING(0),
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
        MARKET_OF_TIME(40),
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
        BRUSH(114),
        BUSH(115),
        CACTUS(116),
        CANYON(117),
        CRATER(118),
        DEAD_VEGETATION(119),
        FLOWERS(120),
        FROZEN_LAKE(121),
        HEDGE(122),
        HILL(123),
        HOLE(124),
        KELP(125),
        LAKE(126),
        LAVA_FLOW(127),
        LAVA_LAKE(128),
        MUSHROOMS(129),
        LOG(130),
        MANDRAKE(131),
        MOSS(132),
        MOUND(133),
        MOUNTAIN(134),
        OAK_TREES(135),
        OUTCROPPING(136),
        PINE_TREES(137),
        PLANT(138),
        HOTA_DECORATION_1(139),
        HOTA_DECORATION_2(140),
        HOTA_GROUND(141),
        HOTA_WAREHOUSE(142),
        RIVER_DELTA(143),
        HOTA_VISITABLE_1(144),
        HOTA_COLLECTIBLE(145),
        HOTA_VISITABLE_2(146),
        ROCK(147),
        SAND_DUNE(148),
        SAND_PIT(149),
        SHRUB(150),
        SKULL(151),
        STALAGMITE(152),
        STUMP(153),
        TAR_PIT(154),
        TREES(155),
        VINE(156),
        VOLCANIC_VENT(157),
        VOLCANO(158),
        WILLOW_TREES(159),
        YUCCA_TREES(160),
        REEF(161),
        RANDOM_MONSTER_L5(162),
        RANDOM_MONSTER_L6(163),
        RANDOM_MONSTER_L7(164),
        BRUSH_2(165),
        BUSH_2(166),
        CACTUS_2(167),
        CANYON_2(168),
        CRATER_2(169),
        DEAD_VEGETATION_2(170),
        FLOWERS_2(171),
        FROZEN_LAKE_2(172),
        HEDGE_2(173),
        HILL_2(174),
        HOLE_2(175),
        KELP_2(176),
        LAKE_2(177),
        LAVA_FLOW_2(178),
        LAVA_LAKE_2(179),
        MUSHROOMS_2(180),
        LOG_2(181),
        MANDRAKE_2(182),
        MOSS_2(183),
        MOUND_2(184),
        MOUNTAIN_2(185),
        OAK_TREES_2(186),
        OUTCROPPING_2(187),
        PINE_TREES_2(188),
        PLANT_2(189),
        RIVER_DELTA_2(190),
        ROCK_2(191),
        SAND_DUNE_2(192),
        SAND_PIT_2(193),
        SHRUB_2(194),
        SKULL_2(195),
        STALAGMITE_2(196),
        STUMP_2(197),
        TAR_PIT_2(198),
        TREES_2(199),
        VINE_2(200),
        VOLCANIC_VENT_2(201),
        VOLCANO_2(202),
        WILLOW_TREES_2(203),
        YUCCA_TREES_2(204),
        REEF_2(205),
        DESERT_HILLS(206),
        DIRT_HILLS(207),
        GRASS_HILLS(208),
        ROUGH_HILLS(209),
        SUBTERRANEAN_ROCKS(210),
        SWAMP_FOLIAGE(211),
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
            }
        }
    }

    enum class Quest(val value: Int) {
        NONE(0),
        ACHIEVE_EXPERIENCE_LEVEL(1),
        ACHIEVE_PRIMARY_SKILL_LEVEL(2),
        DEFEAT_SPECIFIC_HERO(3),
        DEFEAT_SPECIFIC_MONSTER(4),
        RETURN_WITH_ARTIFACTS(5),
        RETURN_WITH_CREATURES(6),
        RETURN_WITH_RESOURCES(7),
        BE_SPECIFIC_HERO(8),
        BELONG_TO_SPECIFIC_PLAYER(9),
        HOTA_QUEST(10);

        companion object {
            fun fromInt(value: Int) = Quest.values().find { it.value == value }
                ?: throw IllegalArgumentException("Invalid WarMachineFactory value: $value")
        }

    }

    enum class WarMachineFactory(val value: Int) {
        NORMAL(0),
        CANNON(1);

        companion object {
            fun fromInt(value: Int) = values().find { it.value == value }
                ?: throw IllegalArgumentException("Invalid WarMachineFactory value: $value")
        }
    }

    enum class HotADecoration1(val value: Int) {
        CRATE(0),
        CRATES(1),
        SACK(2),
        BARRELS(3),
        JAW(4),
        ROPE(5),
        FROG(6),
        FROGS(7),
        CHICKEN(8),
        ROOSTER(9),
        SEAWEED(10),
        CRUMBLED_CAMP(11),
        CRUMBLED_FOUNTAIN(12),
        PIG(13),
        ANCIENT_ALTAR(14),
        ABANDONED_BOAT(15),
        FENCE(16),
        WATERFALLS(17),
        FIRE(18),
        CRUMBLED_EDIFICE(19),
        CARNIVOROUS_PLANT(20),
        BRIDGE(21),
        BONE(22),
        SACKS(23),
        PUDDLES(24),
        RUBBLE(25),
        LIMESTONE_PUDDLES(26),
        PILLARS(27),
        REED(28),
        FISSURES(29),
        BURNT_STRUCTURE(30),
        STELE(31);

        companion object {
            fun fromInt(value: Int) = values().find { it.value == value }
                ?: throw IllegalArgumentException("Invalid HotADecoration1 value: $value")
        }
    }

    enum class HotADecoration2(val value: Int) {
        BOULDER(0),
        STONE(1),
        PALMS(2),
        ICE_BLOCK(3),
        PILE_OF_STONES(4),
        SNOW_HILLS(5),
        BARCHAN_DUNES(6),
        SPRUCES(7),
        LIMESTONE_LAKE(8),
        WALL(9),
        STAIRS(10),
        PREDATORY_PLANTS(11),
        MAPLE_TREES(12),
        NATURAL_ARCH(13);

        companion object {
            fun fromInt(value: Int) = values().find { it.value == value }
                ?: throw IllegalArgumentException("Invalid HotADecoration2 value: $value")
        }
    }

    enum class HotAGround(val value: Int) {
        CRACKED_ICE(0),
        DUNES(1),
        FIELDS_OF_GLORY(2);

        companion object {
            fun fromInt(value: Int) = values().find { it.value == value }
                ?: throw IllegalArgumentException("Invalid HotAGround value: $value")
        }
    }

    enum class HotAVisitable1(val value: Int) {
        TEMPLE_OF_LOYALTY(0),
        SKELETON_TRANSFORMER(1),
        COLOSSEUM_OF_THE_MAGI(2),
        WATERING_PLACE(3),
        MINERAL_SPRING(4),
        HERMITS_SHACK(5),
        GAZEBO(6),
        JUNKMAN(7),
        DERRICK(8),
        WARLOCKS_LAB(9),
        PROSPECTOR(10),
        TRAILBLAZER(11);

        companion object {
            fun fromInt(value: Int) = values().find { it.value == value }
                ?: throw IllegalArgumentException("Invalid HotAVisitable1 value: $value")
        }
    }

    enum class HotACollectible(val value: Int) {
        ANCIENT_LAMP(0),
        SEA_BARREL(1),
        JETSAM(2),
        VIAL_OF_MANA(3);

        companion object {
            fun fromInt(value: Int) = values().find { it.value == value }
                ?: throw IllegalArgumentException("Invalid HotACollectible value: $value")
        }
    }

    enum class HotAVisitable2(val value: Int) {
        SEAFARING_ACADEMY(0),
        OBSERVATORY(1),
        ALTAR_OF_MANA(2),
        TOWN_GATE(3),
        ANCIENT_ALTAR(4);

        companion object {
            fun fromInt(value: Int) = values().find { it.value == value }
                ?: throw IllegalArgumentException("Invalid HotAVisitable2 value: $value")
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

        companion object {
            fun fromInt(value: Int?): SeerHutRewardType {
                return values().find { it.value == value }
                    ?: throw Exception("Unknown SeerHutRewardType")
            }
        }
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
        if (h3m.version === H3m.Version.SOD) {
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
        val type = Quest.fromInt(missionType)
        when (type) {
            Quest.NONE -> return

            Quest.DEFEAT_SPECIFIC_HERO,
            Quest.DEFEAT_SPECIFIC_MONSTER,
            Quest.ACHIEVE_PRIMARY_SKILL_LEVEL,
            Quest.ACHIEVE_EXPERIENCE_LEVEL -> stream.skip(4)

            Quest.RETURN_WITH_ARTIFACTS -> {
                val amount = stream.readByte()
                stream.skip(amount * 4)
            }

            Quest.RETURN_WITH_CREATURES -> {
                val amount = stream.readByte()
                stream.skip(amount * 2 * 2)
            }

            Quest.RETURN_WITH_RESOURCES -> stream.skip(7 * 4)
            Quest.BE_SPECIFIC_HERO -> stream.readByte()
            Quest.BELONG_TO_SPECIFIC_PLAYER -> stream.readByte()
            Quest.HOTA_QUEST -> {
                val questType = stream.readInt()
                if (questType == 0) { // belong to class
                    stream.readInt()
                    stream.readBytes(3)
                } else if (questType == 1) { // date
                    stream.readInt()
                }
            }
        }

        stream.readInt() // deadline
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
        stream.readByte() //never flees
        stream.readByte() //not grown
        stream.skip(2)

        if (h3m.hotaVersion >= 3) {
            stream.readInt() // character spec
            stream.readByte() // money join
            stream.readInt() // percent join
            stream.readInt() // upgraded stack
            stream.readInt() // stacks count
        }

        if (h3m.hotaVersion >= 4) {
            stream.readBytes(5)
        }
    }

    fun readSign() {
        stream.readString()
        stream.skip(4)
    }

    fun readSeerHutReward() {
        when (SeerHutRewardType.fromInt(stream.readByte())) {
            SeerHutRewardType.EXPERIENCE,
            SeerHutRewardType.MANA_POINTS -> stream.readInt()

            SeerHutRewardType.MORALE_BONUS,
            SeerHutRewardType.SPELL,
            SeerHutRewardType.LUCK_BONUS -> stream.readByte()

            SeerHutRewardType.RESOURCES -> {
                stream.readByte()
                stream.readInt()
            }

            SeerHutRewardType.PRIMARY_SKILL,
            SeerHutRewardType.SECONDARY_SKILL-> {
                stream.readByte()
                stream.readByte()
            }

            SeerHutRewardType.ARTIFACT -> {
                if (h3m.version === H3m.Version.ROE) {
                    stream.readByte()
                } else if (h3m.hotaVersion >= 4) {
                    stream.readInt()
                } else {
                    stream.readShort()
                }
            }

            SeerHutRewardType.CREATURE -> {
                stream.skip(if (h3m.version === H3m.Version.ROE) 3 else 4)
            }

            else -> {}
        }
    }

    fun readSeerHut() {
        if (h3m.hotaVersion >= 3) {
            val oneTimeQuests = stream.readInt()
            for (i in 0 until oneTimeQuests) {
                readQuest(stream.readByte())
                readSeerHutReward()
            }

            val repeatableQuests = stream.readInt()
            for (i in 0 until repeatableQuests) {
                readQuest(stream.readByte())
                readSeerHutReward()
            }
        } else {
            var missionType: Int

            if (h3m.version == H3m.Version.ROE) {
                val artId = stream.readByte()
                missionType = if (artId != 255) 1 else 0
            } else {
                missionType = stream.readByte()
                readQuest(missionType)
            }

            if (missionType > 0) {
                readSeerHutReward()
                stream.skip(2)
            } else {
                stream.skip(1)
            }
        }

        stream.skip(2)
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
        stream.skip(1)
        stream.skip(3)
        readCreatureSet(7)
        if (h3m.version != H3m.Version.ROE) {
            stream.readBool()
        }
        stream.skip(8)
    }

    fun readArtifact(obj: Object?) {
        readMessageAndGuards()
        if (obj === Object.SPELL_SCROLL) {
            stream.readInt()
        }

        if (h3m.hotaVersion >= 4 && obj !== Object.SPELL_SCROLL) {
            stream.readBytes(5)
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
            stream.skip(9) //spells must appear
        }
        stream.skip(9) //spells cant appear

        if (h3m.hotaVersion >= 1) {
            stream.readByte() // spell research
        }
        if (h3m.hotaVersion >= 4) {
            val settingsCount = stream.readInt() // unknown
            stream.readBytes(settingsCount)
        }

        val castleEvents = stream.readInt()
        for (i in 0 until castleEvents) {
            stream.readString() //name
            stream.readString() //message
            readResources()
            stream.readByte() // players
            if (h3m.version > H3m.Version.AB) {
                stream.readByte() //humanAffected
            }
            stream.skip(1) //computerAffected
            stream.skip(2) //firstOccurence
            stream.skip(2) //nextOccurence
            stream.skip(16) //gap

            if (h3m.hotaVersion >= 4) {
                stream.readInt() // hota_lvl_7b
                stream.readInt() // hota_amount
                stream.readBytes(6) // unknown
            }

            stream.skip(6) //new buildings
            stream.skip(7 * 2) //creatures
            stream.skip(4)
        }
        if (h3m.version > H3m.Version.AB) {
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
            val castleIndex =
                stream.readInt() // dwelling faction same as a town #castleIndex faction
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