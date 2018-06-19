package skadistats.clarity.source;

import sun.nio.ch.DirectBuffer;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MappedFileSource extends Source {

    private FileChannel channel;
    private MappedByteBuffer buf;

    public MappedFileSource(String fileName) throws IOException {
        this(Paths.get(fileName));
    }

    public MappedFileSource(File file) throws IOException {
        this(file.toPath());
    }

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

    @Override
    public void close() throws IOException {
        // see http://stackoverflow.com/questions/2972986/how-to-unmap-a-file-from-memory-mapped-using-filechannel-in-java
        if (channel != null) {
            channel.close();
            channel = null;
        }
        if (buf != null) {
            ((DirectBuffer) buf).cleaner().clean();
            buf = null;
        }
    }

}
