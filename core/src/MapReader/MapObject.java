package MapReader;

import com.badlogic.gdx.utils.TimeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class MapObject implements Comparable<MapObject> {
    public static final int CASTLE = 0;
    public static final int RAMPART = 1;
    public static final int TOWER = 2;
    public static final int INFERNO = 3;
    public static final int NECROPOLIS = 4;
    public static final int DUNGEON = 5;
    public static final int STRONGHOLD = 6;
    public static final int FORTRESS = 7;
    public static final int CONFLUX = 8;

    public static String[] RESOURCES = {
            "avtwood0.def",
            "avtore0.def",
            "avtsulf0.def",
            "avtmerc0.def",
            "avtcrys0.def",
            "avtgems0.def",
            "avtgold0.def",
    };

    private static HashMap<Integer, List<String>> dwellings = new HashMap<Integer, List<String>>() {{
        put(CASTLE, new ArrayList<String>() {{
            add("AVGpike0.def");
            add("AVGcros0.def");
            add("AVGgrff0.def");
            add("AVGswor0.def");
            add("AVGmonk0.def");
            add("AVGcavl0.def");
            add("AVGangl0.def");
        }});
        put(RAMPART, new ArrayList<String>() {{
            add("AVGcent0.def");
            add("AVGdwrf0.def");
            add("AVGelf0.def");
            add("AVGpega0.def");
            add("AVGtree0.def");
            add("AVGunic0.def");
            add("AVGgdrg0.def");
        }});
        put(TOWER, new ArrayList<String>() {{
            add("AVGgrem0.def");
            add("AVGgarg0.def");
            add("AVGgolm0.def");
            add("AVGmage0.def");
            add("AVGgeni0.def");
            add("AVGnaga0.def");
            add("AVGtitn0.def");
        }});
        put(INFERNO, new ArrayList<String>() {{
            add("AVGimp0.def");
            add("AVGgogs0.def");
            add("AVGhell0.def");
            add("AVGdemn0.def");
            add("AVGpit0.def");
            add("AVGefre0.def");
            add("AVGdevl0.def");
        }});
        put(NECROPOLIS, new ArrayList<String>() {{
            add("AVGskel0.def");
            add("AVGzomb0.def");
            add("AVGwght0.def");
            add("AVGvamp0.def");
            add("AVGlich0.def");
            add("AVGbkni0.def");
            add("AVGbone0.def");
        }});
        put(DUNGEON, new ArrayList<String>() {{
            add("AVGtrog0.def");
            add("AVGharp0.def");
            add("AVGbhld0.def");
            add("AVGmdsa0.def");
            add("AVGmino0.def");
            add("AVGmant0.def");
            add("AVGrdrg0.def");
        }});
        put(STRONGHOLD, new ArrayList<String>() {{
            add("AVGgobl0.def");
            add("AVGwolf0.def");
            add("AVGorcg0.def");
            add("AVGogre0.def");
            add("AVGrocs0.def");
            add("AVGcycl0.def");
            add("AVGbhmt0.def");
        }});
        put(FORTRESS, new ArrayList<String>() {{
            add("AVGgnll0.def");
            add("AVGlzrd0.def");
            add("AVGdfly0.def");
            add("AVGbasl0.def");
            add("AVGgorg0.def");
            add("AVGwyvn0.def");
            add("AVGhydr0.def");
        }});
        put(CONFLUX, new ArrayList<String>() {{
            add("AVGpixie.def");
            add("AVGair0.def");
            add("AVGwatr0.def");
            add("AVGfire0.def");
            add("AVGerth0.def");
            add("AVGelp.def");
            add("AVGfbrd.def");
        }});
    }};
    private static HashMap<Integer, List<String>> monsters = new HashMap<Integer, List<String>>() {{
        put(1, new ArrayList<String>() {{
            add("AvWPike.def");
            add("AVWpikx0.def");
            add("AVWcent0.def");
            add("AVWcenx0.def");
            add("AVWgrem0.def");
            add("AVWgrex0.def");
            add("AVWimp0.def");
            add("AVWimpx0.def");
            add("AVWskel0.def");
            add("AVWskex0.def");
            add("AVWtrog0.def");
            add("AvWInfr.def");
            add("AVWgobl0.def");
            add("AVWgobx0.def");
            add("AVWgnll0.def");
            add("AVWgnlx0.def");
            add("AVWpixie.def");
            add("AVWsprit.def");
            add("AVWhalf.def");
            add("AVWpeas.def");
        }});
        put(2, new ArrayList<String>() {{
            add("AvWLCrs.def");
            add("AvWHCrs.def");
            add("AVWdwrf0.def");
            add("AVWdwrx0.def");
            add("AVWgarg0.def");
            add("AVWgarx0.def");
            add("AVWgog0.def");
            add("AVWgogx0.def");
            add("AVWzomb0.def");
            add("AVWzomx0.def");
            add("AVWharp0.def");
            add("AVWharx0.def");
            add("AVWwolf0.def");
            add("AVWwolx0.def");
            add("AvWLizr.def");
            add("AVWlizx0.def");
            add("AVWelmw0.def");
            add("AVWicee.def");
            add("AVWboar.def");
            add("AVWrog.def");
        }});
        put(3, new ArrayList<String>() {{
            add("AvWGrif.def");
            add("AVWgrix0.def");
            add("AVWelfw0.def");
            add("AVWelfx0.def");
            add("AVWgolm0.def");
            add("AVWgolx0.def");
            add("AVWhoun0.def");
            add("AVWhoux0.def");
            add("AvWWigh.def");
            add("AVWwigx0.def");
            add("AVWbehl0.def");
            add("AVWbehx0.def");
            add("AVWorc0.def");
            add("AVWorcx0.def");
            add("AvWDFly.def");
            add("AvWDFir.def");
            add("AVWelme0.def");
            add("AVWstone.def");
            add("AVWmumy.def");
            add("AVWnomd.def");
        }});
        put(4, new ArrayList<String>() {{
            add("AVWswrd0.def");
            add("AVWswrx0.def");
            add("AVWpega0.def");
            add("AVWpegx0.def");
            add("AVWmage0.def");
            add("AVWmagx0.def");
            add("AVWdemn0.def");
            add("AVWdemx0.def");
            add("AVWvamp0.def");
            add("AVWvamx0.def");
            add("AvWMeds.def");
            add("AVWmedx0.def");
            add("AVWogre0.def");
            add("AVWogrx0.def");
            add("AvWBasl.def");
            add("AvWGBas.def");
            add("AVWelma0.def");
            add("AVWstorm.def");
            add("AVWglmg0.def");
            add("AVWsharp.def");
        }});
        put(5, new ArrayList<String>() {{
            add("AvWMonk.def");
            add("AVWmonx0.def");
            add("AVWtree0.def");
            add("AVWtrex0.def");
            add("AVWgeni0.def");
            add("AVWgenx0.def");
            add("AVWpitf0.def");
            add("AVWpitx0.def");
            add("AVWlich0.def");
            add("AVWlicx0.def");
            add("AvWMino.def");
            add("AVWminx0.def");
            add("AVWroc0.def");
            add("AVWrocx0.def");
            add("AvWGorg.def");
            add("AVWgorx0.def");
            add("AVWelmf0.def");
            add("AVWnrg.def");
            add("AVWglmd0.def");
        }});
        put(6, new ArrayList<String>() {{
            add("AVWcvlr0.def");
            add("AVWcvlx0.def");
            add("AVWunic0.def");
            add("AVWunix0.def");
            add("AVWnaga0.def");
            add("AVWnagx0.def");
            add("AVWefre0.def");
            add("AVWefrx0.def");
            add("AVWbkni0.def");
            add("AVWbknx0.def");
            add("AVWmant0.def");
            add("AVWmanx0.def");
            add("AVWcycl0.def");
            add("AVWcycx0.def");
            add("AvWWyvr.def");
            add("AVWwyvx0.def");
            add("AVWpsye.def");
            add("AVWmagel.def");
            add("AVWench.def");
        }});
        put(7, new ArrayList<String>() {{
            add("AvWAngl.def");
            add("AvWArch.def");
            add("AVWdrag0.def");
            add("AVWdrax0.def");
            add("AVWtitn0.def");
            add("AVWtitx0.def");
            add("AVWdevl0.def");
            add("AVWdevx0.def");
            add("AVWbone0.def");
            add("AVWbonx0.def");
            add("AvWRDrg.def");
            add("AVWddrx0.def");
            add("AVWbhmt0.def");
            add("AVWbhmx0.def");
            add("AvWHydr.def");
            add("AVWhydx0.def");
            add("AVWfbird.def");
            add("AVWphx.def");
        }});
        put(10, new ArrayList<String>() {{
            add("AVWfdrg.def");
            add("AVWazure.def");
            add("AVWcdrg.def");
            add("AVWrust.def");
        }});
    }};
    public int x;
    public int y;
    public int z;
    public DefInfo def;
    public Obj obj;
    public Player owner;

    public static String getRandomMonsterDefName() {
        List<String> monstersDefs = MapObject.monsters.get(new Random(TimeUtils.millis()).nextInt(6) + 1);
        return monstersDefs.get(new Random(TimeUtils.millis()).nextInt(monstersDefs.size() - 1));
    }

    public static String getRandomMonsterDefNameByLevel(int level) {
        List<String> monstersDefs = MapObject.monsters.get(level);
        return monstersDefs.get(new Random(TimeUtils.millis()).nextInt(monstersDefs.size() - 1));
    }

    public static String getRandomDwellingDefNameByLevel(int level) {
        List<String> levelDwellings = new ArrayList<String>();
        for (List<String> defsList : dwellings.values()) {
            levelDwellings.add(defsList.get(level));
        }
        return levelDwellings.get(new Random(TimeUtils.millis()).nextInt(levelDwellings.size() - 1));
    }

    public static String getRandomDwellingDefNameByFactionAndLevel(int faction, int level) {

        List<String> factionDwellings = MapObject.dwellings.get(faction);
        return factionDwellings.get(level);
    }

    public static int getRandomTown() {
        return new Random(TimeUtils.millis()).nextInt(8);
    }

    public static String getRandomResourceDefName() {
        return RESOURCES[new Random(TimeUtils.millis()).nextInt(RESOURCES.length - 1)];
    }

    public static String getTownDefName(int townId, Boolean hasFort) {
        switch (townId) {
            case CASTLE:
                return hasFort ? "avccasx0.def" : "avccast0.def";
            case RAMPART:
                return hasFort ? "avcramx0.def" : "avcramp0.def";
            case TOWER:
                return hasFort ? "avctowx0.def" : "avctowr0.def";
            case INFERNO:
                return hasFort ? "avcinfx0.def" : "avcinfc0.def";
            case NECROPOLIS:
                return hasFort ? "avcnecx0.def" : "avcnecr0.def";
            case DUNGEON:
                return hasFort ? "avcdunx0.def" : "avcdung0.def";
            case STRONGHOLD:
                return hasFort ? "avcstrx0.def" : "avcstro0.def";
            case FORTRESS:
                return hasFort ? "avcftrx0.def" : "avcftrt0.def";
            case CONFLUX:
                return hasFort ? "avchforx.def" : "avchfor0.def";
            default:
                return "avcrand0.def";
        }
    }

    @Override
    public int compareTo(MapObject secondObject) {
        if (this.def.placementOrder != secondObject.def.placementOrder) {
            return this.def.placementOrder >= secondObject.def.placementOrder ? 1 : -1;
        }

        if (this.y != secondObject.y) {
            return this.y >= secondObject.y ? -1 : 1;
        }

        if (secondObject.obj == MapObject.Obj.HERO && this.obj != MapObject.Obj.HERO) {
            return 1;
        }
        if (secondObject.obj != MapObject.Obj.HERO && this.obj == MapObject.Obj.HERO) {
            return -1;
        }

        if (!this.def.isVisitable() && secondObject.def.isVisitable()) {
            return 1;
        }
        if (!secondObject.def.isVisitable() && this.def.isVisitable()) {
            return -1;
        }

        if (this.x <= secondObject.x) {
            return 1;
        }

        return -1;
    }

    public enum Obj {
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

        private int value;

        private Obj(int value) {
            this.value = value;
        }

        static public Obj fromInt(int searchValue) {
            for (Obj v : values()) {
                if (v.value == searchValue) {
                    return v;
                }
            }
            return NO_OBJ;
        }

        public int getValue() {
            return this.value;
        }
    }

    public enum SeerHutRewardType {
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

        int value;

        private SeerHutRewardType(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public static class Art {
        final static int ART_SPECIAL = 0;
        final static int ART_TREASURE = 1;
        final static int ART_MINOR = 2;
        final static int ART_MAJOR = 3;
        final static int ART_RELIC = 4;
        final static int ART_ANY = 5;

        static public String[] classes = {"S", "T", "M", "J", "R"};
        public String rarityClass;
        public String name;

        public static int getRandomArtId(int artLevel) {
            int min, max;
            switch (artLevel) {
                case ART_RELIC:
                    min = 129;
                    max = 141;
                    break;
                default:
                    min = 10;
                    max = 128;
                    break;
            }

            int randomInt = new Random(TimeUtils.millis()).nextInt(max - min) + min;

            if (randomInt == 128) {
                // 128 - pandora's box. shouldn't be generated randomly
                return getRandomArtId(artLevel);
            }
            return randomInt;
        }

        public String toString() {
            return rarityClass + " " + name;
        }
    }
}