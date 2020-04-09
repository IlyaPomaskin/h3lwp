package com.heroes3.livewallpaper.AssetsParser;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Reader {
    InputStream stream;
    public long pos = 0;
    protected long mark = 0;

    public Reader(InputStream input) throws IOException {
        if (!input.markSupported()) {
            throw new IOException("Mark not supported.");
        }

        input.mark(input.available());

        stream = input;
    }

    public ByteBuffer toByteBuffer(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    public int readByte() throws IOException {
        return Byte.toUnsignedInt(
            toByteBuffer(readBytes(1)).array()[0]
        );
    }

    public int readShort() throws IOException {
        return Short.toUnsignedInt(toByteBuffer(readBytes(2)).getShort());
    }

    public int readInt() throws IOException {
        return (int) Integer.toUnsignedLong(toByteBuffer(readBytes(4)).getInt());
    }

    public String readPrefixedLengthString() throws IOException {
        long length = readInt();
        return new String(readBytes((int) length));
    }

    public String readString(int length) throws IOException {
        return new String(readBytes(length)).replaceAll("\u0000.*", "");
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

    public synchronized int read(byte[] b, int offset, int length) throws IOException {
        int n = stream.read(b, offset, length);
        if (n > 0) {
            pos += length;
        }
        return n;
    }

    public synchronized long skip(long skip) throws IOException {
        long n = stream.skip(skip);
        if (n > 0) {
            pos += skip;
        }
        return n;
    }

    public synchronized void mark(int readlimit) throws IOException {
        stream.mark(readlimit);
        mark = pos;
    }

    public synchronized void reset() throws IOException {
        stream.reset();
        pos = mark;
    }

    public void seek(long offset) throws IOException {
        reset();
        skip(offset);
    }
}