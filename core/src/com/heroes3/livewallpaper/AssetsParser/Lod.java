package com.heroes3.livewallpaper.AssetsParser;

import java.util.HashMap;

class Lod {
    byte[] magic = new byte[8];
    int filesCount;
    byte[] unknown = new byte[80];
    File[] files;

    static class File {
        String name;
        int offset;
        int size;
        FileType fileType;
        int compressedSize;
    }

    public enum FileType {
        SPELL(0x40),
        SPRITE(0x41),
        CREATURE(0x42),
        MAP(0x43),
        MAP_HERO(0x44),
        TERRAIN(0x45),
        CURSOR(0x46),
        INTERFACE(0x47),
        SPRITE_FRAME(0x48),
        BATTLE_HERO(0x49);

        private final int fileType;

        FileType(int fileType) {
            this.fileType = fileType;
        }

        public Integer getFileType() {
            return fileType;
        }

        private static final HashMap<Integer, FileType> lookup = new HashMap<Integer, FileType>();

        static {
            for (FileType env : FileType.values()) {
                lookup.put(env.getFileType(), env);
            }
        }

        public static FileType get(Integer fileType) {
            return lookup.get(fileType);
        }
    }
}
