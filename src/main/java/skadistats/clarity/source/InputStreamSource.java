package skadistats.clarity.source;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

public class InputStreamSource extends Source {

    private final InputStream stream;
    private int position;
    private final byte[] dummy = new byte[65536];

    public InputStreamSource(String fileName) throws IOException {
        this(new BufferedInputStream(new FileInputStream(fileName)));
    }

    public InputStreamSource(File file) throws IOException {
        this(new BufferedInputStream(new FileInputStream(file)));
    }

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
            var r = Math.min(dummy.length, newPosition - position);
            readBytes(dummy, 0, r);
        }
    }

    @Override
    public byte readByte() throws IOException {
        var i = stream.read();
        if (i == -1) {
            throw new EOFException();
        }
        position++;
        return (byte) i;
    }

    @Override
    public void readBytes(byte[] dest, int offset, int length) throws IOException {
        while (length > 0) {
            var r = stream.read(dest, offset, length);
            if (r == -1) {
                throw new EOFException();
            }
            position += r;
            offset += r;
            length -= r;
        }
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

}
