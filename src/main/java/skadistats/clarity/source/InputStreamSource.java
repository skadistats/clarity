package skadistats.clarity.source;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamSource extends Source {

    private final InputStream stream;
    private int position;
    private byte[] dummy = new byte[32768];

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
        while (position != newPosition) {
            int r = Math.min(dummy.length, newPosition - position);
            readBytes(dummy, 0, r);
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

}
