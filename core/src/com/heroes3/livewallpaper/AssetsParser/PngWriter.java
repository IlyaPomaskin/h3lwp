package com.heroes3.livewallpaper.AssetsParser;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterInputStream;

class PngWriter {
    private ByteArrayOutputStream out = new ByteArrayOutputStream();

    byte[] create(int width, int height, byte[] palette, byte[] transparent, byte[] data) throws IOException {
        out.write(new byte[]{-119, 80, 78, 71, 13, 10, 26, 10});
        writeChunk("IHDR", createIHDR(width, height));
        writeChunk("PLTE", palette);
        writeChunk("tRNS", transparent);
        writeChunk("IDAT", createIDAT(data, width, height));
        writeChunk("IEND", new byte[0]);
        return out.toByteArray();
    }

    private byte[] createIDAT(byte[] data, int width, int height) throws IOException {
        ByteArrayOutputStream scanlines = new ByteArrayOutputStream();
        for (int i = 0; i < height; i++) {
            scanlines.write((byte) 0);
            scanlines.write(
                Arrays.copyOfRange(data, width * i, width * i + width)
            );
        }

        byte[] input = scanlines.toByteArray();
        byte[] output = new byte[input.length];
        Deflater deflater = new Deflater();
        deflater.setInput(input);
        deflater.finish();
        deflater.deflate(output);
        deflater.end();
        return output;
    }

    private byte[] createIHDR(int width, int height) throws IOException {
        ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
        ihdr.write(
            ByteBuffer
                .allocate(4)
                .putInt(width)
                .array()
        );
        ihdr.write(
            ByteBuffer
                .allocate(4)
                .putInt(height)
                .array()
        );
        ihdr.write((byte) 8); // bitDepth
        ihdr.write((byte) 3); // colorType
        ihdr.write(0); // compressionMethod
        ihdr.write(0); // filterMethod
        ihdr.write(0); // interlaceMethod
        return ihdr.toByteArray();
    }

    private void writeChunk(String type, byte[] content) throws IOException {
        out.write(
            ByteBuffer
                .allocate(4)
                .putInt(content.length)
                .array());
        out.write(type.getBytes());
        out.write(content);
        out.write(
            ByteBuffer
                .allocate(4)
                .putInt((int) getCRC(type, content))
                .array()
        );
    }

    private long getCRC(String type, byte[] content) {
        CRC32 crc32 = new CRC32();
        crc32.update(type.getBytes(), 0, type.getBytes().length);
        crc32.update(content, 0, content.length);
        return crc32.getValue();
    }
}