package MapReader;

import MapReader.Map.Version;
import com.badlogic.gdx.utils.TimeUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class MapReader {
    private H3InputStream stream;
    private Map map = new Map();

    public MapReader(InputStream fileStream) throws IOException {
        stream = new H3InputStream(readWholeFile(new GZIPInputStream(fileStream)));
    }

    private ByteArrayInputStream readWholeFile(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            int bufferSize = 1;
            byte[] buffer = new byte[bufferSize];
            int length;
            while ((length = in.read(buffer, 0, bufferSize)) > 0) {
                out.write(buffer, 0, length);
            }
        } finally {
            out.close();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    protected void readHeader() throws IOException {
        //map version
        map.version = Version.fromInt(stream.readInt(4));
        if (map.version == Version.Unknown) {
            throw new IOException("Wrong map format");
        }

        //areAnyPlayers
        stream.readByte();
        map.size = stream.readInt(4);
        map.hasUnderground = stream.readBool();
        map.title = stream.readString();
        map.description = stream.readString();

        //difficulty
        stream.readByte();

        //level limit
        if (map.version != Version.ROE) {
            stream.readByte();
        }

        readPlayerInfo();
        readVictoryLossConditions();
        readTeamInfo();
        readAllowedHeroes();
    }

    private void readVictoryLossConditions() throws IOException {
        int victoryCondition = stream.readInt(1);
        if (victoryCondition != 0xFF) {
            //allow normal victory
            //applies to ai
            stream.readBytes(2);
        }
        switch (victoryCondition) {
            case 0x00:
                //ARTIFACT
                //obj terrain
                stream.readByte();
                if (map.version != Version.ROE) {
                    stream.readByte();
                }
                break;
            case 0x01:
                //GATHERTROOP
                //obj terrain
                stream.readByte();
                if (map.version != Version.ROE) {
                    stream.readByte();
                }
                //value
                stream.readInt(4);
                break;
            case 0x02:
                //GATHERRESOURCE
                //obj terrain
                stream.readByte();
                //value
                stream.readInt(4);
                break;
            case 0x03:
                //BUILDCITY
                //coords
                stream.readBytes(3);
                //obj terrain village
                stream.readByte();
                //obj terrain form
                stream.readByte();
                break;
            case 0x04:
                //BUILDGRAIL
                //coords
                stream.readBytes(3);
                break;
            case 0x05:
                //BEATHERO
                //coords
                stream.readBytes(3);
                break;
            case 0x06:
                //CAPTURECITY
                //coords
                stream.readBytes(3);
                break;
            case 0x07:
                //BEATMONSTER
                //coords
                stream.readBytes(3);
                break;
            case 0x08:
                //TAKEDWELLINGS
                break;
            case 0x09:
                //TAKEMINES
                break;
            case 0x0A:
                //TRANSPORTITEM
                //obj terrain
                stream.readByte();
                //coords
                stream.readBytes(3);
                break;
            default:
                break;
        }

        int lossCondition = stream.readInt(1);
        switch (lossCondition) {
            case 0x01:
                //LOSSCASTLE
                //coords
                stream.readBytes(3);
                break;
            case 0x02:
                //LOSSHERO
                stream.readBytes(3);
                break;
            case 0x03:
                //TIMEEXPIRES
                stream.readBytes(2);
                break;
            default:
                break;
        }
    }

    private void readPlayerInfo() throws IOException {
        for (int i = 0; i < 8; i++) {
            Player player = new Player();
            player.playerColor = Player.PlayerColor.values()[i];

            // can be played by human
            Boolean canHumanPlay = stream.readBool();
            // can be played by computer
            Boolean canPCPlay = stream.readBool();

            if (!canHumanPlay && !canPCPlay) {
                switch (map.version) {
                    case SOD:
                    case WOG:
                        stream.readBytes(13);
                        break;
                    case AB:
                        stream.readBytes(12);
                        break;
                    case ROE:
                        stream.readBytes(6);
                        break;
                }
                continue;
            }

            //behavior
            stream.readByte();

            //is allowed towns set
            if (map.version == Version.WOG || map.version == Version.SOD) {
                player.isTownsSet = stream.readBool();
            } else {
                player.isTownsSet = true;
            }

            //allowed towns
            BitSet townsBits = Bits.convert(stream.readInt(map.version == Version.ROE ? 1 : 2));
            for (int j = 0; j < 9; j++) {
                Boolean isCurrentTownAllowed = townsBits.get(j);
                if (isCurrentTownAllowed) {
                    player.allowedTowns.add(Player.Town.values()[j]);
                }
            }

            player.isRandomTown = stream.readBool();
            player.hasMainTown = stream.readBool();
            if (player.hasMainTown) {
                if (map.version != Version.ROE) {
                    player.generateHeroAtMainTown = stream.readBool();
                    player.generateHero = stream.readBool();
                } else {
                    player.generateHeroAtMainTown = true;
                    player.generateHero = false;
                }

                player.mainTownX = stream.readByte();
                player.mainTownY = stream.readByte();
                player.mainTownZ = stream.readByte();
            }

            //has random hero
            stream.readBool();
            //main custom hero id
            int heroId = stream.readInt(1);
            if (heroId != 0xFF) {
                //portrait
                stream.readInt(1);
                stream.readString();
            }

            if (map.version != Version.ROE) {
                //unknown byte
                stream.readByte();
                int heroCount = stream.readInt(4);
                for (int k = 0; k < heroCount; k++) {
                    //hero id
                    stream.skip(1);
                    stream.readString();
                }
            }

            map.players.add(player);
        }
    }

    private void readTeamInfo() throws IOException {
        //teams count
        int teamsCount = stream.readInt(1);

        if (teamsCount > 0) {
            for (int i = 0; i < 8; i++) {
                //team num
                stream.readByte();
            }
        }
    }

    private void readAllowedHeroes() throws IOException {
        int bytesCount = map.version == Version.ROE ? 16 : 20;
        stream.readBytes(bytesCount);

        // Probably reserved for further heroes
        if (map.version != Version.ROE) {
            int placeholdersQty = stream.readInt(4);
            stream.readBytes(placeholdersQty);
        }
    }

    private void readDisposedHeroes() throws IOException {
        if (map.version == Version.SOD || map.version == Version.WOG) {
            int heroesCount = stream.readInt(1);
            for (int i = 0; i < heroesCount; i++) {
                //hero id
                stream.readByte();
                //portrait
                stream.readByte();
                //name
                stream.readString();
                //players
                stream.readByte();
            }
        }

        //omitting NULLs
        stream.readBytes(31);
    }

    private void readAllowedArtifacts() throws IOException {
        // Reading allowed artifacts:  17 or 18 bytes
        if (map.version != Version.ROE) {
            int bytesCount = map.version == Version.AB ? 17 : 18;
            stream.readBytes(bytesCount);
        }
    }

    private void readAllowedSpellsAbilities() throws IOException {
        if (map.version == Version.SOD || map.version == Version.WOG) {
            //spells
            stream.readBytes(9);

            //abillities
            stream.readBytes(4);
        }
    }

    private void readRumors() throws IOException {
        int rumorsCount = stream.readInt(4);

        for (int i = 0; i < rumorsCount; i++) {
            //name
            stream.readString();
            //text
            stream.readString();
        }
    }

    private void loadArtifactToSlot() throws IOException {
        if (map.version == Version.ROE) {
            stream.readInt(1);
        } else {
            stream.readInt(2);
        }
    }

    private void readPredefinedHeroes() throws IOException {
        if (map.version == Version.WOG || map.version == Version.SOD) {
            for (int i = 0; i < 156; i++) {
                //skip if hero doesnt have settings
                if (!stream.readBool()) {
                    continue;
                }

                //exp
                if (stream.readBool()) {
                    stream.readInt(4);
                }

                //sec skills
                if (stream.readBool()) {
                    //count
                    int skillsCount = stream.readInt(4);
                    for (int k = 0; k < skillsCount; k++) {
                        stream.readByte();//skill
                        stream.readByte();//value
                    }
                }

                //artifacts
                loadArtifactsOfHero();

                //bio
                if (stream.readBool()) {
                    stream.readString();
                }

                //sex
                stream.readByte();

                //spells
                if (stream.readBool()) {
                    stream.readBytes(9);
                }

                //primary skills
                if (stream.readBool()) {
                    stream.readBytes(4);
                }
            }
        }
    }

    private void readTerrain() throws IOException {
        int size = ((int) Math.pow(map.size, 2)) * (map.hasUnderground ? 2 : 1);

        for (int i = 0; i < size; i++) {
            Tile tile = new Tile();
            tile.terrain = Tile.TerrainType.values()[stream.readInt(1)];
            tile.terrainImageIndex = stream.readInt(1);
            tile.river = Tile.RiverType.values()[stream.readInt(1)];
            tile.riverImageIndex = stream.readInt(1);
            tile.road = Tile.RoadType.values()[stream.readInt(1)];
            tile.roadImageIndex = stream.readInt(1);

            long mirrorConf = stream.readByte();
            tile.flipConf = Bits.convert(mirrorConf);

            map.tiles.add(tile);
        }
    }

    private List<DefInfo> readDefInfo() throws IOException {
        List<DefInfo> defs = new ArrayList<DefInfo>();

        int defsCount = stream.readInt(4);
        for (int i = 0; i < defsCount; i++) {
            DefInfo def = new DefInfo();
            def.spriteName = stream.readString();

            def.passableCells = Bits.convert(((long) stream.readInt(4) << 32) | ((long) stream.readInt(2) & 0xFFFFFFFL));
            def.activeCells = Bits.convert(((long) stream.readInt(4) << 32) | ((long) stream.readInt(2) & 0xFFFFFFFL));

            stream.skip(2); //terrain type
            stream.skip(2); //terrain group
            def.objectId = stream.readInt(4);
            def.objectClassSubId = stream.readInt(4);
            stream.skip(1); //objects group
            def.placementOrder = stream.readInt(1);
            stream.skip(16); //unknown nulls

            defs.add(def);
        }

        return defs;
    }

    private void readCreatureSet(int creaturesCount) throws IOException {
        for (int i = 0; i < creaturesCount; i++) {
            stream.skip(map.version != Version.ROE ? 2 : 1); //creature id
            stream.skip(2); //count
        }
    }

    private void readMessageAndGuards() throws IOException {
        Boolean hasMessage = stream.readBool();
        if (hasMessage) {
            stream.readString();
            Boolean hasGuards = stream.readBool();
            if (hasGuards) {
                readCreatureSet(7);
            }
            stream.skip(4);
        }
    }

    private void readResources() throws IOException {
        for (int i = 0; i < 7; i++) {
            stream.skip(4);
        }
    }

    private void loadArtifactsOfHero() throws IOException {
        //has arts
        if (stream.readBool()) {
            for (int j = 0; j < 16; j++) {
                loadArtifactToSlot();
            }

            if (map.version == Version.SOD || map.version == Version.WOG) {
               loadArtifactToSlot();
            }

            //spellbook
            loadArtifactToSlot();

            //fifth slot
            if (map.version != Version.ROE) {
                loadArtifactToSlot();
            } else {
                stream.readByte();
            }
            //bag
            int artsCount = stream.readInt(2);
            for (int h = 0; h < artsCount; h++) {
                loadArtifactToSlot();
            }
        }
    }

    private void readHero() throws IOException {
        if (map.version != Version.ROE) {
            stream.skip(4); //id
        }
        stream.skip(1); //owner
        stream.skip(1); //sub id

        //hasName
        if (stream.readBool()) {
            stream.readString();
        }
        if (map.version != Version.ROE && map.version != Version.AB) {
            //hasExp
            if (stream.readBool()) {
                stream.readInt(4);
            }
        } else {
            stream.readInt(4);
        }

        //has portait
        if (stream.readBool()) {
            stream.readInt(1);
        }

        //has sec skills
        if (stream.readBool()) {
            int skillsCount = stream.readInt(4);
            stream.skip(skillsCount * 2);
        }

        //has garison
        if (stream.readBool()) {
            readCreatureSet(7);
        }

        stream.readInt(1); //formation

        loadArtifactsOfHero();

        stream.readInt(1); //patrol radius

        if (map.version != Version.ROE) {
            //custom bio
            if (stream.readBool()) {
                stream.readString();
            }
            //sex
            stream.readInt(1);
        }

        //spells
        if (map.version != Version.ROE && map.version != Version.AB) {
            //custom spells
            if (stream.readBool()) {
                stream.skip(9);
            }
        } else if (map.version == Version.AB) {
            //one spell?
            stream.skip(1);
        }

        if (map.version != Version.ROE && map.version != Version.AB) {
            //prim skills
            if (stream.readBool()) {
                stream.skip(4);
            }
        }
        stream.skip(16);
    }

    private void readQuest(int missionType) throws IOException {
        switch (missionType) {
            case 0:
                return;
            case 2:
                stream.skip(4);
                break;
            case 1:
            case 3:
            case 4:
                stream.skip(4);
                break;
            case 5:
                int artNum = stream.readInt(1);
                stream.skip(artNum * 2);
                break;
            case 6:
                int typeNum = stream.readInt(1);
                stream.readInt(typeNum * 2 * 2);
                break;
            case 7:
                stream.skip(7 * 4);
                break;
            case 8:
            case 9:
                stream.skip(1);
                break;
        }
        stream.skip(4);
        stream.readString();//first visit
        stream.readString();//next visit
        stream.readString();//completed
    }

    private int readTown() throws IOException {
        int townIdentifier = 0;
        if (map.version != Version.ROE) {
            townIdentifier = stream.readInt(4); //town identifier
        }

        stream.readInt(1); //owner

        //has name
        if (stream.readBool()) {
            stream.readString();
        }

        //has garison
        if (stream.readBool()) {
            readCreatureSet(7);
        }

        stream.readInt(1); //formation

        //has custom buildings
        if (stream.readBool()) {
            stream.skip(6); //builtBuildings
            stream.skip(6); //forbiddenBuildings
        } else {
            //has form
            stream.readBool();
        }

        if (map.version != Version.ROE) {
            stream.skip(9); //spells?
        }

        stream.skip(9);//more spells?

        int castleEvents = stream.readInt(4);
        for (int i = 0; i < castleEvents; i++) {
            stream.readString();
            stream.readString();
            readResources();

            stream.readInt(1);

            if (map.version != Version.ROE) {
                stream.readInt(1);
            }

            stream.skip(1); //computerAffected
            stream.skip(2); //firstOccurence
            stream.skip(1); //nextOccurence

            stream.skip(17); //gap

            stream.skip(6); //new buildings

            stream.skip(7 * 2); //creatures

            stream.skip(4);
        }

        if (map.version != Version.ROE && map.version != Version.AB) {
            stream.skip(1); //alignment
        }

        stream.skip(3);

        return townIdentifier;
    }

    private void readObjects(List<DefInfo> defs) throws IOException {
        int objectsCount = stream.readInt(4);
        for (int i = 0; i < objectsCount; i++) {
            MapObject object = new MapObject();
            object.x = stream.readInt(1);
            object.y = stream.readInt(1);
            object.z = stream.readInt(1);
            int idx = stream.readInt(4);
            object.def = defs.get(idx).clone();
            object.obj = MapObject.Obj.fromInt(object.def.objectId);

            stream.skip(5);

            switch (object.obj) {
                case EVENT:
                    readMessageAndGuards();

                    stream.skip(4); //exp
                    stream.skip(4); //mana
                    stream.skip(1); //morale
                    stream.skip(1); //luck

                    readResources();

                    stream.skip(4); //prim skills

                    int gainedAbilities = stream.readInt(1);
                    stream.skip(gainedAbilities * 2);

                    int gainedArts = stream.readInt(1);
                    stream.skip(gainedArts * (map.version == Version.ROE ? 1 : 2));

                    int gainedSpells = stream.readInt(1);
                    stream.skip(gainedSpells);

                    int gainedCreatures = stream.readInt(1);
                    readCreatureSet(gainedCreatures);

                    stream.skip(8);

                    stream.skip(1); //available for
                    stream.skip(1); //computer activate
                    stream.skip(1); //remove after visit

                    stream.skip(4);

                    break;
                case HERO:
                case RANDOM_HERO:
                case PRISON: {
                    readHero();
                    break;
                }
                case MONSTER:
                case RANDOM_MONSTER:
                case RANDOM_MONSTER_L1:
                case RANDOM_MONSTER_L2:
                case RANDOM_MONSTER_L3:
                case RANDOM_MONSTER_L4:
                case RANDOM_MONSTER_L5:
                case RANDOM_MONSTER_L6:
                case RANDOM_MONSTER_L7: {
                    if (map.version != Version.ROE) {
                        stream.skip(4);
                    }

                    stream.readInt(2); //count
                    stream.readInt(1); //character
                    //has msg
                    if (stream.readBool()) {
                        stream.readString();
                        readResources();

                        if (map.version == Version.ROE) {
                            stream.readInt(1);
                        } else {
                            stream.readInt(2);
                        }
                    }

                    stream.readInt(1); //never fless
                    stream.readInt(1); //not grown
                    stream.skip(2);

                    switch (object.obj) {
                        case RANDOM_MONSTER:
                            object.def.spriteName = MapObject.getRandomMonsterDefName();
                            break;
                        case RANDOM_MONSTER_L1:
                            object.def.spriteName = MapObject.getRandomMonsterDefNameByLevel(1);
                            break;
                        case RANDOM_MONSTER_L2:
                            object.def.spriteName = MapObject.getRandomMonsterDefNameByLevel(2);
                            break;
                        case RANDOM_MONSTER_L3:
                            object.def.spriteName = MapObject.getRandomMonsterDefNameByLevel(3);
                            break;
                        case RANDOM_MONSTER_L4:
                            object.def.spriteName = MapObject.getRandomMonsterDefNameByLevel(4);
                            break;
                        case RANDOM_MONSTER_L5:
                            object.def.spriteName = MapObject.getRandomMonsterDefNameByLevel(5);
                            break;
                        case RANDOM_MONSTER_L6:
                            object.def.spriteName = MapObject.getRandomMonsterDefNameByLevel(6);
                            break;
                        case RANDOM_MONSTER_L7:
                            object.def.spriteName = MapObject.getRandomMonsterDefNameByLevel(7);
                            break;
                    }
                    break;
                }
                case OCEAN_BOTTLE:
                case SIGN: {
                    stream.readString();
                    stream.skip(4);
                    break;
                }
                case SEER_HUT: {
                    int missionType = 0;
                    if (map.version != Version.ROE) {
                        missionType = stream.readInt(1);
                        readQuest(missionType);
                    } else {
                        int artId = stream.readInt(1);
                        if (artId != 255) {
                            missionType = 1;
                        } else {
                            missionType = 0;
                        }
                    }

                    if (missionType > 0) {
                        MapObject.SeerHutRewardType rewardType = MapObject.SeerHutRewardType.values()[stream.readInt(1)];
                        switch (rewardType) {
                            case EXPERIENCE:
                                stream.readInt(4);
                                break;
                            case MANA_POINTS:
                                stream.readInt(4);
                                break;
                            case MORALE_BONUS:
                                stream.readInt(1);
                                break;
                            case LUCK_BONUS:
                                stream.readInt(1);
                                break;
                            case RESOURCES:
                                stream.readInt(1);
                                stream.readInt(4);
                                break;
                            case PRIMARY_SKILL:
                                stream.readInt(1);
                                stream.readInt(1);
                                break;
                            case SECONDARY_SKILL:
                                stream.readInt(1);
                                stream.readInt(1);
                                break;
                            case ARTIFACT:
                                stream.readInt(map.version == Version.ROE ? 1 : 2);
                                break;
                            case SPELL:
                                stream.readInt(1);
                                break;
                            case CREATURE:
                                stream.skip(map.version == Version.ROE ? 3 : 4);
                                break;
                        }
                        stream.skip(2);
                    } else {
                        stream.skip(3);
                    }
                    break;
                }
                case WITCH_HUT:
                    if (map.version != Version.ROE) {
                        stream.skip(4);
                    }
                    break;
                case SCHOLAR:
                    stream.skip(2);
                    stream.skip(6);
                    break;
                case GARRISON:
                case GARRISON2:
                    stream.skip(1);
                    stream.skip(3);
                    readCreatureSet(7);
                    if (map.version != Version.ROE) {
                        stream.readBool();
                    }
                    stream.skip(8);
                    break;
                case ARTIFACT:
                case RANDOM_ART:
                case RANDOM_TREASURE_ART:
                case RANDOM_MINOR_ART:
                case RANDOM_MAJOR_ART:
                case RANDOM_RELIC_ART:
                case SPELL_SCROLL:
                    if (object.obj != MapObject.Obj.ARTIFACT && object.obj != MapObject.Obj.SPELL_SCROLL) {
                        int artId;
                        switch (object.obj) {
                            case RANDOM_TREASURE_ART:
                                artId = MapObject.Art.getRandomArtId(MapObject.Art.ART_TREASURE);
                                break;
                            case RANDOM_MINOR_ART:
                                artId = MapObject.Art.getRandomArtId(MapObject.Art.ART_MINOR);
                                break;
                            case RANDOM_MAJOR_ART:
                                artId = MapObject.Art.getRandomArtId(MapObject.Art.ART_MAJOR);
                                break;
                            case RANDOM_RELIC_ART:
                                artId = MapObject.Art.getRandomArtId(MapObject.Art.ART_RELIC);
                                break;
                            default:
                                artId = MapObject.Art.getRandomArtId(MapObject.Art.ART_ANY);
                        }
                        object.def.spriteName = String.format("AVA%04d.def", artId);
                    }
                    readMessageAndGuards();
                    if (object.obj == MapObject.Obj.SPELL_SCROLL) {
                        stream.readInt(4);
                    }
                    break;
                case RANDOM_RESOURCE:
                case RESOURCE:
                    readMessageAndGuards();
                    stream.readInt(4);
                    stream.skip(4);
                    if (object.obj == MapObject.Obj.RANDOM_RESOURCE) {
                        object.def.spriteName = MapObject.getRandomResourceDefName();
                    }
                    break;
                case RANDOM_TOWN:
                case TOWN:
                    if (object.obj == MapObject.Obj.RANDOM_TOWN) {
                        object.def.objectClassSubId = MapObject.getRandomTown();
                        object.obj = MapObject.Obj.TOWN;
                    }
                    object.def.spriteName = MapObject.getTownDefName(object.def.objectClassSubId, true);
                    int townIdentifier = readTown();
                    map.towns.put(townIdentifier, object.def.objectClassSubId);
                    break;
                case MINE:
                case ABANDONED_MINE:
                case SHRINE_OF_MAGIC_INCANTATION:
                case SHRINE_OF_MAGIC_GESTURE:
                case SHRINE_OF_MAGIC_THOUGHT:
                case GRAIL:
                    stream.skip(4);
                    break;
                case CREATURE_GENERATOR1:
                case CREATURE_GENERATOR2:
                case CREATURE_GENERATOR3:
                case CREATURE_GENERATOR4:
                    int playerColor = stream.readInt(1);
                    stream.skip(3);
                    break;
                case PANDORAS_BOX:
                    readMessageAndGuards();

                    stream.skip(4); //exp
                    stream.skip(4); //mana
                    stream.skip(1); //morale
                    stream.skip(1); //luck

                    readResources();

                    stream.skip(4); //prim skills

                    int gainedAbilitiesPandora = stream.readInt(1);
                    stream.skip(gainedAbilitiesPandora * 2);

                    int gainedArtsPandora = stream.readInt(1);
                    stream.skip(gainedArtsPandora * (map.version == Version.ROE ? 1 : 2));

                    int gainedSpellsPandora = stream.readInt(1);
                    stream.skip(gainedSpellsPandora);

                    int gainedCreaturesPandora = stream.readInt(1);
                    readCreatureSet(gainedCreaturesPandora);

                    stream.skip(8);
                    break;
                case RANDOM_DWELLING:
                case RANDOM_DWELLING_LVL:
                case RANDOM_DWELLING_FACTION:
                    stream.skip(4);
                    int min = 0;
                    int max = 6;
                    int randomLevel;
                    if (object.obj == MapObject.Obj.RANDOM_DWELLING || object.obj == MapObject.Obj.RANDOM_DWELLING_LVL) {
                        int castleIndex = stream.readInt(4); // dwelling faction same as a town #castleIndex faction
                        if (castleIndex == 0) {
                            BitSet allowedTowns = Bits.convert(stream.readInt(2));
                        }
                    }

                    if (object.obj == MapObject.Obj.RANDOM_DWELLING || object.obj == MapObject.Obj.RANDOM_DWELLING_FACTION) {
                        min = stream.readInt(1); //min lvl
                        max = stream.readInt(1); //max lvl
                    }

                    randomLevel = new Random(TimeUtils.millis()).nextInt(max - min) + min;
                    switch (object.obj) {
                        case RANDOM_DWELLING_LVL:
                            object.def.spriteName = MapObject.getRandomDwellingDefNameByLevel(randomLevel);
                            break;
                        case RANDOM_DWELLING_FACTION:
                            object.def.spriteName = MapObject.getRandomDwellingDefNameByFactionAndLevel(new Random(TimeUtils.millis()).nextInt(8), randomLevel);
                            break;
                        case RANDOM_DWELLING:
                            int randomCastleId = new Random(TimeUtils.millis()).nextInt(8);
                            object.def.spriteName = MapObject.getRandomDwellingDefNameByFactionAndLevel(randomCastleId, randomLevel);
                            break;
                    }

                    break;
                case QUEST_GUARD:
                    int missionType = stream.readInt(1);
                    readQuest(missionType);
                    break;
                case SHIPYARD:
                    stream.readInt(4);
                    break;
                case HERO_PLACEHOLDER:
                    stream.readInt(1);
                    int heroTypeId = stream.readInt(1);
                    if (heroTypeId == 255) {
                        stream.skip(1);
                    }
                    break;
                case LIGHTHOUSE:
                    stream.readInt(4);
                    break;
            }

            if (object.obj != MapObject.Obj.GRAIL) {
                map.objects.add(object);
            }
        }

        Collections.sort(map.objects);
        Collections.reverse(map.objects);

        System.out.printf("Map contains %d objects\r\n", map.objects.size());
    }

    public Map read() throws IOException {
        readHeader();
        readDisposedHeroes();
        readAllowedArtifacts();
        readAllowedSpellsAbilities();
        readRumors();
        readPredefinedHeroes();
        readTerrain();
        List<DefInfo> defs = readDefInfo();
        readObjects(defs);
        return map;
    }
}
