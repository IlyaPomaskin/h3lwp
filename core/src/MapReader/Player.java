package MapReader;

import com.badlogic.gdx.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class Player {
    public PlayerColor playerColor;
    public List<Town> allowedTowns = new ArrayList<Town>();
    public Boolean isRandomTown;
    public Boolean hasMainTown;
    public Boolean isTownsSet;
    public Boolean generateHeroAtMainTown;
    public Boolean generateHero;
    public Boolean hasRandomHero;
    public Boolean mainCustomHeroId;
    public Town mainTownType;
    public int mainTownX;
    public int mainTownY;
    public int mainTownZ;

    public static int grey = new Color(0.67f, 0.67f, 0.67f, 0.5f).toIntBits();

    public enum Town {
        Castle(0),
        Rampart(1),
        Tower(2),
        Inferno(3),
        Necropolis(4),
        Dungeon(5),
        Stronghold(6),
        Fortress(7),
        Conflux(8),
        Random(255);

        private int value;

        private Town(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public enum PlayerColor {
        Red(0), Blue(1), Tan(2), Green(3), Orange(4), Purple(5), Teal(6), Pink(7);

        private int value;

        private PlayerColor(int value) {
            this.value = value;
        }

        public int getColor() {
            switch (value) {
                case 0:
                    return new Color(0xF3 / 255, 0x00 / 255, 0x12 / 255, 0.5f).toIntBits();
                case 1:
                    return new Color(0x36 / 255, 0x3D / 255, 0xFE / 255, 0.5f).toIntBits();
                case 2:
                    return new Color(0xC2 / 255, 0xA4 / 255, 0x7A / 255, 0.5f).toIntBits();
                case 3:
                    return new Color(0x30 / 255, 0x81 / 255, 0x0B / 255, 0.5f).toIntBits();
                case 4:
                    return new Color(0xF7 / 255, 0x7F / 255, 0x00 / 255, 0.5f).toIntBits();
                case 5:
                    return new Color(0x9B / 255, 0x00 / 255, 0xAF / 255, 0.5f).toIntBits();
                case 6:
                    return new Color(0x1E / 255, 0x72 / 255, 0x71 / 255, 0.5f).toIntBits();
                case 7:
                    return new Color(0xF9 / 255, 0xC0 / 255, 0xD4 / 255, 0.5f).toIntBits();
            }
            return grey;
        }
    }
}
