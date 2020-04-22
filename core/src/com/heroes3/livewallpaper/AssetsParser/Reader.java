package com.heroes3.livewallpaper.AssetsParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Reader {
    private InputStream stream;
    long pos = 0;
    private long mark = 0;

    public Reader(InputStream input) throws IOException {
        if (!input.markSupported()) {
            throw new IOException("Mark not supported.");
        }

        input.mark(input.available());

        stream = input;
    }

    private ByteBuffer toByteBuffer(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    public int readByte() throws IOException {
        return Byte.toUnsignedInt(
            toByteBuffer(readBytes(1)).array()[0]
        );
    }

    public boolean readBool() throws IOException {
        return readByte() == 1;
    }

    public int readShort() throws IOException {
        return Short.toUnsignedInt(toByteBuffer(readBytes(2)).getShort());
    }

    public int readInt() throws IOException {
        return (int) Integer.toUnsignedLong(toByteBuffer(readBytes(4)).getInt());
    }

    String readString(int length) throws IOException {
        return new String(readBytes(length)).replaceAll("\u0000.*", "");
    }

    public String readString() throws IOException {
        return readString(readInt());
    }

    public byte[] readBytes(int length) throws IOException {
        byte[] buffer = new byte[length];
        read(buffer, 0, length);
        return buffer;
    }

    public synchronized int read() throws IOException {
        int b = stream.read();
        if (b >= 0) {
            pos += 1;
        }
        return b;
    }

    private synchronized int read(byte[] b, int offset, int length) throws IOException {
        int n = stream.read(b, offset, length);
        if (n > 0) {
            pos += length;
        }
        return n;
    }

    public long skip(int amount) throws IOException {
        return skip((long) amount);
    }

    public synchronized long skip(long amount) throws IOException {
        long n = stream.skip(amount);
        if (n > 0) {
            pos += amount;
        }
        return n;
    }

    public synchronized void mark(int readlimit) throws IOException {
        stream.mark(readlimit);
        mark = pos;
    }

    private synchronized void reset() throws IOException {
        stream.reset();
        pos = mark;
    }

    void seek(long offset) throws IOException {
        reset();
        skip(offset);
    }
}