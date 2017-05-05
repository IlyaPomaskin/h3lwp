package MapReader;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class H3InputStream extends FilterInputStream {
    public byte[] buffer;
    private long pos = 0;
    private long mark = 0;

    public H3InputStream(InputStream in) throws IOException {
        super(in);
    }

    public ByteBuffer toByteBuffer(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    public int readInt(int bytesCount) throws IOException {
        ByteBuffer readBuffer = toByteBuffer(readBytes(bytesCount));
        if (bytesCount == 1) {
            return (int) readBuffer.get() & 0xFF;
        } else if (bytesCount == 2) {
            return (int) readBuffer.getShort();
        } else {
            return readBuffer.getInt();
        }
    }

    public int readByte() throws IOException {
        return toByteBuffer(readBytes(1)).get();
    }

    public Boolean readBool() throws IOException {
        return readByte() == 1;
    }

    private int readStringLength() throws IOException {
        buffer = new byte[4];
        super.read(buffer, 0, 4);
        return toByteBuffer(buffer).getInt();
    }

    public String readString() throws IOException {
        int length = readStringLength();
        buffer = new byte[length];
        super.read(buffer, 0, length);
        return new String(buffer);
    }

    public byte[] readBytes(int length) throws IOException {
        buffer = new byte[length];
        super.read(buffer, 0, length);
        return buffer;
    }

    public synchronized long getPosition() {
        return pos;
    }

    @Override
    public synchronized int read() throws IOException {
        int b = super.read();
        if (b >= 0)
            pos += 1;
        return b;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n > 0)
            pos += len;
        return n;
    }

    @Override
    public synchronized long skip(long skip) throws IOException {
        long n = super.skip(skip);
        if (n > 0)
            pos += skip;
        return n;
    }

    @Override
    public synchronized void mark(int readlimit) {
        super.mark(readlimit);
        mark = pos;
    }

    @Override
    public synchronized void reset() throws IOException {
        /* A call to reset can still succeed if mark is not supported, but the
         * resulting stream position is undefined, so it's not allowed here. */
        if (!markSupported())
            throw new IOException("Mark not supported.");
        super.reset();
        pos = mark;
    }
}
