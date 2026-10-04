package skadistats.clarity.source;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

/**
 * Reads from an {@link InputStream}. Forward only: {@link #setPosition(int)} skips by reading and discarding data.
 * Use it for non-seekable input such as stdin or sockets; prefer {@link MappedFileSource} for local files.
 *
 * <p>Reading past the end of the stream throws {@link java.io.EOFException}. {@link #close()} closes the stream.
 */
public class InputStreamSource extends Source {

    private final InputStream stream;
    private int position;
    private final byte[] dummy = new byte[65536];

    /**
     * Opens the file through a {@link BufferedInputStream}.
     *
     * @param fileName path of the replay file
     * @throws IOException if the file cannot be opened
     */
    public InputStreamSource(String fileName) throws IOException {
        this(new BufferedInputStream(new FileInputStream(fileName)));
    }

    /**
     * Opens the file through a {@link BufferedInputStream}.
     *
     * @param file the replay file
     * @throws IOException if the file cannot be opened
     */
    public InputStreamSource(File file) throws IOException {
        this(new BufferedInputStream(new FileInputStream(file)));
    }

    /**
     * @param stream the stream, positioned at the start of the replay; it is not wrapped in a buffer
     */
    public InputStreamSource(InputStream stream) {
        this.stream = stream;
        this.position = 0;
    }

    @Override
    public int getPosition() {
        return position;
    }

    /**
     * Skips forward to the given position by reading and discarding.
     *
     * @param newPosition the new position
     * @throws UnsupportedOperationException if {@code newPosition} is lower than the current position
     * @throws IOException if the stream ends before the position is reached
     */
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

    /**
     * Closes the underlying stream.
     *
     * @throws IOException if closing the stream fails
     */
    @Override
    public void close() throws IOException {
        stream.close();
    }

}
