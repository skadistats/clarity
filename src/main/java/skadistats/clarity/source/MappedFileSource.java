package skadistats.clarity.source;

import skadistats.clarity.platform.ClarityPlatform;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads a local file through a read-only memory mapping. Supports arbitrary seeking in both directions.
 *
 * <p>The mapping is released by {@link #close()}; use try-with-resources. The file size is fixed at construction;
 * use {@link LiveSource} for files that are still growing. Reading past the end throws {@link java.io.EOFException}.
 */
public class MappedFileSource extends Source {

    private FileChannel channel;
    private MappedByteBuffer buf;

    /**
     * @param fileName path of the replay file
     * @throws IOException if the file cannot be opened or mapped
     */
    public MappedFileSource(String fileName) throws IOException {
        this(Paths.get(fileName));
    }

    /**
     * @param file the replay file
     * @throws IOException if the file cannot be opened or mapped
     */
    public MappedFileSource(File file) throws IOException {
        this(file.toPath());
    }

    /**
     * @param file path of the replay file
     * @throws IOException if the file cannot be opened or mapped
     */
    public MappedFileSource(Path file) throws IOException {
        channel = FileChannel.open(file);
        buf = channel.map(FileChannel.MapMode.READ_ONLY, 0L, Files.size(file));
    }

    @Override
    public int getPosition() {
        return buf.position();
    }

    @Override
    public void setPosition(int position) throws IOException {
        if (position > buf.limit()) {
            throw new EOFException();
        }
        buf.position(position);
    }

    @Override
    public byte readByte() throws IOException {
        if (buf.remaining() < 1) {
            throw new EOFException();
        }
        return buf.get();
    }

    @Override
    public void readBytes(byte[] dest, int offset, int length) throws IOException {
        if (buf.remaining() < length) {
            throw new EOFException();
        }
        buf.get(dest, offset, length);
    }

    /**
     * Closes the file channel and unmaps the buffer. The source must not be used afterwards.
     *
     * @throws IOException if closing the channel fails
     */
    @Override
    public void close() throws IOException {
        if (channel != null) {
            channel.close();
            channel = null;
        }
        if (buf != null) {
            ClarityPlatform.disposeMappedByteBuffer(buf);
            buf = null;
        }
    }

}
