package MapReader;

import java.util.*;
import java.util.List;

public class Map {

    public Version version;
    public int size;
    public boolean hasUnderground;
    public String title;
    public String description;
    public List<Player> players = new ArrayList<Player>();
    public BitSet availableArtifacts;
    public List<Tile> tiles = new ArrayList<Tile>();
    public List<MapObject> objects = new ArrayList<MapObject>();
    public java.util.Map<Integer, Integer> towns = new HashMap<Integer, Integer>();

    public enum Version {
        Unknown(0),
        ROE(0x0e), // 14
        AB(0x15), // 21
        SOD(0x1c), // 28
        WOG(0x33); // 51

        private int value;

        private Version(int value) {
            this.value = value;
        }

        public static Version fromInt(int searchValue) {
            for (Version v : values()) {
                if (v.value == searchValue) {
                    return v;
                }
            }
            return Unknown;
        }

        public int getValue() {
            return this.value;
        }
    }

//    public int getColorByTownCoords(int[] coords) {
//        for (Player player : players) {
//            if (player.hasMainTown) {
//                if (Arrays.equals(coords, player.mainTownCoords)) {
//                    return player.playerColor.getColor();
//                }
//            }
//        }
//        return Player.grey;
//    }
}
