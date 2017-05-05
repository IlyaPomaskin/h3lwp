package MapReader;

import java.util.BitSet;

public class Tile {
    final static public String[] terrainTiles = {
            "dirttl",
            "sandtl",
            "grastl",
            "snowtl",
            "swmptl",
            "rougtl",
            "subbtl",
            "lavatl",
            "watrtl",
            "rocktl",
    };
    final static public String[] riverTiles = {
            "clrrvr",
            "icyrvr",
            "mudrvr",
            "lavrvr"
    };
    final static public String[] roadTiles = {
            "dirtrd",
            "gravrd",
            "cobbrd",
    };

    public TerrainType terrain;
    public int terrainImageIndex;
    public RiverType river;
    public int riverImageIndex;
    public RoadType road;
    public int roadImageIndex;
    public BitSet flipConf;

    public String toFilename(TilePart tilePart) {
        switch (tilePart) {
            case Terrain:
                return terrain.toString();
            case River:
                return river.toString();
            case Road:
                return road.toString();
            default:
                return "NONE";
        }
    }

    public enum TerrainType {
        Dirt(0),
        Sand(1),
        Grass(2),
        Snow(3),
        Swamp(4),
        Rough(5),
        Subterranean(6),
        Lava(7),
        Water(8),
        Rock(9);

        private int value;

        private TerrainType(int value) {
            this.value = value;
        }

        public String toString() {
            return Tile.terrainTiles[this.value];
        }
    }

    public enum RiverType {
        No(0),
        Clear(1),
        Icy(2),
        Muddy(3),
        Lava(4);

        private int value;

        private RiverType(int value) {
            this.value = value;
        }

        public String toString() {
            return Tile.riverTiles[this.value - 1];
        }
    }

    public enum RoadType {
        No(0),
        Dirt(1),
        Gravel(2),
        Cobblestone(3);

        private int value;

        private RoadType(int value) {
            this.value = value;
        }

        public String toString() {
            return Tile.roadTiles[this.value - 1];
        }
    }

    public enum TilePart {
        Terrain,
        Road,
        River
    }
}
