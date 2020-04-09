package com.heroes3.livewallpaper.AssetsParser;

public class Def {
    int type;
    int fullWidth;
    int fullHeight;
    int groupsCount;
    byte[] rawPalette = new byte[256 * 3];
    int[][] palette = new int[256][3];
    Group[] groups;

    static class Group {
        int groupType;
        int framesCount;
        byte[] unknown = new byte[8];
        String[] filenames;
        int[] framesOffsets;
        Frame[] frames;
        boolean legacy;
    }

    static class Frame {
        int size;
        int compression;
        int fullWidth;
        int fullHeight;
        int width;
        int height;
        int x;
        int y;
        long _offset;
        byte[] data;
    }
}
