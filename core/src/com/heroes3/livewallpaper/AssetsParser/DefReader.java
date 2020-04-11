package com.heroes3.livewallpaper.AssetsParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

class DefReader {
    private Def def = new Def();
    private Reader rdr;
    private int streamLength;

    Def read(InputStream in) throws IOException {
        rdr = new Reader(in);
        streamLength = in.available();

        readHeader();
        readPalette();
        readGroups();
        readFrames();

        return def;
    }

    private void readFrames() throws IOException {
        for (Def.Group group : def.groups) {
            group.frames = new Def.Frame[group.framesCount];
            for (int i = 0; i < group.framesCount; i++) {
                rdr.seek(group.framesOffsets[i]);
                group.frames[i] = readFrame(group);
            }
        }
    }

    private void readGroups() throws IOException {
        def.groups = new Def.Group[def.groupsCount];
        for (int i = 0; i < def.groupsCount; i++) {
            def.groups[i] = readGroup();
        }
    }

    private void readHeader() throws IOException {
        def.type = rdr.readInt();
        def.fullWidth = rdr.readInt();
        def.fullHeight = rdr.readInt();
        def.groupsCount = rdr.readInt();
    }

    private void readPalette() throws IOException {
        def.rawPalette = rdr.readBytes(def.rawPalette.length);

        for (int i = 0; i < def.palette.length; i++) {
            int[] color = {
                Byte.toUnsignedInt(def.rawPalette[i * 3]),
                Byte.toUnsignedInt(def.rawPalette[i * 3 + 1]),
                Byte.toUnsignedInt(def.rawPalette[i * 3 + 2]),
            };
            def.palette[i] = color;
        }
    }

    private Def.Group readGroup() throws IOException {
        Def.Group group = new Def.Group();
        group.groupType = rdr.readInt();
        group.framesCount = rdr.readInt();
        group.unknown = rdr.readBytes(group.unknown.length);

        group.filenames = new String[group.framesCount];
        for (int i = 0; i < group.framesCount; i++) {
            group.filenames[i] = rdr.readString(13);
        }

        group.framesOffsets = new int[group.framesCount];
        for (int i = 0; i < group.framesCount; i++) {
            group.framesOffsets[i] = rdr.readInt();
        }

        group.legacy = readIsLegacy(group.framesOffsets);

        return group;
    }

    private boolean readIsLegacy(int[] framesOffsets) throws IOException {
        long initialPosition = rdr.pos;

        for (int framesOffset : framesOffsets) {
            rdr.seek(framesOffset);
            int size = rdr.readInt() + 32;
            int frameEnd = size + framesOffset;
//            System.out.printf(
//                "frameOffset: %d size: %d frameEnd: %d streamLength: %d\n",
//                framesOffset,
//                size,
//                frameEnd,
//                streamLength
//            );
            if (streamLength != 0 && frameEnd > streamLength) {
//                System.out.printf("isLegacy\n");
                return true;
            }
        }

        rdr.seek(initialPosition);

        return false;
    }

    private Def.Frame readFrame(Def.Group group) throws IOException {
        Def.Frame frame = new Def.Frame();
        frame.size = rdr.readInt();
        frame.compression = rdr.readInt();
        frame.fullWidth = rdr.readInt();
        frame.fullHeight = rdr.readInt();

        if (group.legacy) {
            frame.width = frame.fullWidth;
            frame.height = frame.fullHeight;
            frame.x = 0;
            frame.y = 0;
        } else {
            frame.width = rdr.readInt();
            frame.height = rdr.readInt();
            frame.x = rdr.readInt();
            frame.y = rdr.readInt();
        }

//        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(frame));

        frame._offset = rdr.pos;

        switch (frame.compression) {
            case 0:
                frame.data = frameCompression0(frame);
                break;
            case 1:
                frame.data = frameCompression1(frame);
                break;
            case 2:
                frame.data = frameCompression2(frame);
                break;
            case 3:
                frame.data = frameCompression3(frame);
                break;
        }

        return frame;
    }

    private byte[] frameCompression0(Def.Frame frame) throws IOException {
        return rdr.readBytes(frame.size);
    }

    private byte[] frameCompression1(Def.Frame frame) throws IOException {
        int[] offsets = new int[frame.height];
        for (int i = 0; i < frame.height; i++) {
            offsets[i] = rdr.readInt();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream(frame.size);

        for (int offset : offsets) {
            rdr.seek(frame._offset + offset);

            int left = frame.width;
            do {
                int index = rdr.readByte();
                int length = rdr.readByte() + 1;
                if (index == 0xFF) {
                    output.write(rdr.readBytes(length));
                } else {
                    byte[] array = new byte[length];
                    Arrays.fill(array, (byte) index);
                    output.write(array);
                }
                left -= length;
            } while (left != 0);
        }

        return output.toByteArray();
    }

    private byte[] frameCompression2(Def.Frame frame) throws IOException {
        int[] offsets = new int[frame.height];
        for (int i = 0; i < frame.height; i++) {
            offsets[i] = rdr.readShort();
        }

        rdr.skip(2);

        ByteArrayOutputStream output = new ByteArrayOutputStream(frame.size);

        for (int offset : offsets) {
            rdr.seek(frame._offset + offset);

            int left = frame.width;
            do {
                int code = rdr.readByte();
                int index = (code >> 5);
                int length = (code & 0x1F) + 1;
                if (index == 0x7) {
                    output.write(rdr.readBytes(length));
                } else {
                    byte[] line = new byte[length];
                    Arrays.fill(line, (byte) index);
                    output.write(line);
                }
                left -= length;
            } while (left != 0);
        }

        return output.toByteArray();
    }

    private byte[] frameCompression3(Def.Frame frame) throws IOException {
        int offsetsCount = frame.height * frame.width / 32;
        int[] offsets = new int[offsetsCount];
        for (int i = 0; i < offsetsCount; i++) {
            offsets[i] = rdr.readShort();
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream(frame.size);

        for (int offset : offsets) {
            rdr.seek(frame._offset + offset);

            int left = 32;
            do {
                int code = rdr.readByte();
                int index = (code >> 5);
                int length = (code & 0x1F) + 1;
//                System.out.printf("offset: %d code: %d index: %d length: %d left: %d\n", offset, code, index, length, left);
                if (index == 0x7) {
                    output.write(rdr.readBytes(length));
                } else {
                    byte[] line = new byte[length];
                    Arrays.fill(line, (byte) index);
                    output.write(line);
                }
                left -= length;
            } while (left != 0);
        }

        return output.toByteArray();
    }
}
