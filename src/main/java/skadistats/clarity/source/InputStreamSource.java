package skadistats.clarity.source;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamSource extends Source {

    private final InputStream stream;
    private int position;

    public InputStreamSource(InputStream stream) {
        this.stream = stream;
        this.position = 0;
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public void setPosition(int newPosition) throws IOException {
        if (position > newPosition) {
            throw new UnsupportedOperationException("cannot rewind input stream");
        }
        int n = newPosition - position;
        while (n > 0) {
            long r = stream.skip(n);
            if (r == 0) {
                throw new EOFException();
            }
            position += r;
            n -= r;
        }
    }

    @Override
    public byte readByte() throws IOException {
        int i = stream.read();
        if (i == -1) {
            throw new EOFException();
        }
        position++;
        return (byte) i;
    }

    @Override
    public void readBytes(byte[] dst, int offset, int len) throws IOException {
        while (len > 0) {
            int r = stream.read(dst, offset, len);
            if (r == -1) {
                throw new EOFException();
            }
            position += r;
            offset += r;
            len -= r;
        }
    }

    @Override
    public void skipBytes(int num) throws IOException {
        int n = num;
        while (n > 0) {
            long r = stream.skip(n);
            if (r == 0) {
                throw new EOFException();
            }
            position += r;
            n -= r;
        }
    }

}
