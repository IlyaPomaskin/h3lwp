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
        ROCKLANDS(231),

        //
        HOTA_VISITABLE_2(145),
        HOTA_VISITABLE_3(146);

        companion object {
            fun fromInt(value: Int?): Object {
                return values().find { it.value == value }
                    ?: NO_OBJ
//                    ?: throw Exception("Unknown map object")
            }
        }
    }

    enum class ObjectSubId(val value: Int) {
        Nothing(0),

        // 1
        Altar_of_Sacrifice(2),
        Anchor_Point(3),
        Arena(4),
        Artifact(5),
        Pandoras_Box(6),
        Black_Market(7),
        Boat(8),
        Border_Guard(9),
        Keymasters_Tent(10),
        Buoy(11),
        Campfire(12),
        Cartographer(13),
        Swan_Pond(14),
        Cover_of_Darkness(15),
        Creature_Bank(16),
        Creature_Generator_1(17),
        Creature_Generator_2(18),
        Creature_Generator_3(19),
        Creature_Generator_4(20),
        Cursed_Ground_RoE(21),
        Corpse(22),
        Marletto_Tower(23),
        Derelict_Ship(24),
        Dragon_Utopia(25),
        Event(26),
        Eye_of_the_Magi(27),
        Faerie_Ring(28),
        Flotsam(29),
        Fountain_of_Fortune(30),
        Fountain_of_Youth(31),
        Garden_of_Revelation(32),
        Garrison(33),
        Hero(34),
        Hill_Fort(35),
        Grail(36),
        Hut_of_the_Magi(37),
        Idol_of_Fortune(38),
        Lean_To(39),

        // 40
        Library_of_Enlightenment(41),
        Lighthouse(42),
        Monolith_One_Way_Entrance(43),
        Monolith_One_Way_Exit(44),
        Two_Way_Monolith(45),
        Magic_Plains_RoE(46),
        School_of_Magic(47),
        Magic_Spring(48),
        Magic_Well(49),
        Market_of_Time(50),
        Mercenary_Camp(51),
        Mermaids(52),
        Mine(53),
        Monster(54),
        Mystical_Garden(55),
        Oasis(56),
        Obelisk(57),
        Redwood_Observatory(58),
        Ocean_Bottle(59),
        Pillar_of_Fire(60),
        Star_Axis(61),
        Prison(62),
        Pyramid(63),
        Rally_Flag(64),
        Random_Artifact(65),
        Random_Treasure_Artifact(66),
        Random_Minor_Artifact(67),
        Random_Major_Artifact(68),
        Random_Relic(69),
        Random_Hero(70),
        Random_Monster(71),
        Random_Monster_1(72),
        Random_Monster_2(73),
        Random_Monster_3(74),
        Random_Monster_4(75),
        Random_Resource(76),
        Random_Town(77),
        Refugee_Camp(78),
        Resource(79),
        Sanctuary(80),
        Scholar(81),
        Sea_Chest(82),
        Seers_Hut(83),
        Crypt(84),
        Shipwreck(85),
        Shipwreck_Survivor(86),
        Shipyard(87),
        Shrine_of_Magic_Incantation(88),
        Shrine_of_Magic_Gesture(89),
        Shrine_of_Magic_Thought(90),
        Sign(91),
        Sirens(92),
        Spell_Scroll(93),
        Stables(94),
        Tavern(95),
        Temple(96),
        Den_of_Thieves(97),
        Town(98),
        Trading_Post(99),
        Learning_Stone(100),
        Treasure_Chest(101),
        Tree_of_Knowledge(102),
        Subterranean_Gate(103),
        University(104),
        Wagon(105),
        War_Machine_Factory(106),
        School_of_War(107),
        Warriors_Tomb(108),
        Water_Wheel(109),
        Watering_Hole(110),
        Whirlpool(111),
        Windmill(112),
        Witch_Hut(113),
        Brush(114),
        Bush(115),
        Cactus(116),
        Canyon(117),
        Crater(118),
        Dead_Vegetation(119),
        Flowers(120),
        Frozen_Lake(121),
        Hedge(122),
        Hill(123),
        Hole(124),
        Kelp(125),
        Lake(126),
        Lava_Flow(127),
        Lava_Lake(128),
        Mushrooms(129),
        Log(130),
        Mandrake(131),
        Moss(132),
        Mound(133),
        Mountain(134),
        Oak_Trees(135),
        Outcropping(136),
        Pine_Trees(137),
        Plant(138),
        HotA_Decoration_1(139), // HotA
        HotA_Decoration_2(140), // HotA
        HotA_Ground(141), // HotA
        HotA_Warehouse(142), // HotA
        River_Delta(143),
        HotA_Visitable_1(144), // HotA
        HotA_Collectible(145), // HotA
        HotA_Visitable_2(146), // HotA
        Rock(147),
        Sand_Dune(148),
        Sand_Pit(149),
        Shrub(150),
        Skull(151),
        Stalagmite(152),
        Stump(153),
        Tar_Pit(154),
        Trees(155),
        Vine(156),
        Volcanic_Vent(157),
        Volcano(158),
        Willow_Trees(159),
        Yucca_Trees(160),
        Reef(161),
        Random_Monster_5(162),
        Random_Monster_6(163),
        Random_Monster_7(164),
        Brush_2(165),
        Bush_2(166),
        Cactus_2(167),
        Canyon_2(168),
        Crater_2(169),
        Dead_Vegetation_2(170),
        Flowers_2(171),
        Frozen_Lake_2(172),
        Hedge_2(173),
        Hill_2(174),
        Hole_2(175),
        Kelp_2(176),
        Lake_2(177),
        Lava_Flow_2(178),
        Lava_Lake_2(179),
        Mushrooms_2(180),
        Log_2(181),
        Mandrake_2(182),
        Moss_2(183),
        Mound_2(184),
        Mountain_2(185),
        Oak_Trees_2(186),
        Outcropping_2(187),
        Pine_Trees_2(188),
        Plant_2(189),
        River_Delta_2(190),
        Rock_2(191),
        Sand_Dune_2(192),
        Sand_Pit_2(193),
        Shrub_2(194),
        Skull_2(195),
        Stalagmite_2(196),
        Stump_2(197),
        Tar_Pit_2(198),
        Trees_2(199),
        Vine_2(200),
        Volcanic_Vent_2(201),
        Volcano_2(202),
        Willow_Trees_2(203),
        Yucca_Trees_2(204),
        Reef_2(205),
        Desert_Hills(206),
        Dirt_Hills(207),
        Grass_Hills(208),
        Rough_Hills(209),
        Subterranean_Rocks(210),
        Swamp_Foliage(211),
        Border_Gate(212),
        Freelancers_Guild(213),
        Hero_Placeholder(214),
        Quest_Guard(215),
        Random_Dwelling(216),
        Random_Dwelling_Leveled(217),
        Random_Dwelling_Faction(218),
        Garrison_Vertical(219),
        Abandoned_Mine(220),
        Trading_Post_Snow(221),
        Clover_Field(222),
        Cursed_Ground(223),
        Evil_Fog(224),
        Favorable_Winds(225),
        Fiery_Fields(226),
        Holy_Ground(227),
        Lucid_Pools(228),
        Magic_Clouds(229),
        Magic_Plains(230),
        Rocklands(231);

        companion object {
            private val map = values().associateBy(ObjectSubId::value)
            fun fromInt(type: Int): ObjectSubId {
                return map[type] ?: throw Exception("unknown obj sub id: $type")
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
        var missionType: Int
        if (h3m.version !== H3m.Version.ROE) {
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
                    if (h3m.version === H3m.Version.ROE) {
                        stream.readByte()
                    } else {
                        stream.readShort()
                    }
                }

                SeerHutRewardType.SPELL -> stream.readByte()
                SeerHutRewardType.CREATURE -> {
                    stream.skip(if (h3m.version === H3m.Version.ROE) 3 else 4)
                }

                else -> {}
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