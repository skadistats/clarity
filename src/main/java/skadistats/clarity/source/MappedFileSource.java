package skadistats.clarity.source;

import sun.nio.ch.DirectBuffer;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MappedFileSource extends Source {

    private final File file;
    private final RandomAccessFile raf;
    private final MappedByteBuffer buf;

    public MappedFileSource(String fileName) throws IOException {
        this(new File(fileName));
    }

    public MappedFileSource(File file) throws IOException {
        this.file = file;
        raf = new RandomAccessFile(file, "r");
        buf = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, raf.length());
        raf.close();
    }

    @Override
    public int getPosition() {
        return buf.position();
    }

    @Override
    public void setPosition(int position) throws IOException {
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
    public int getLastTick() throws IOException {
        if (lastTick == null) {
            int backup = getPosition();
            super.getLastTick();
            setPosition(backup);
        }
        return lastTick;
    }

    @Override
    public void close() throws IOException {
        // see http://stackoverflow.com/questions/2972986/how-to-unmap-a-file-from-memory-mapped-using-filechannel-in-java
        ((DirectBuffer) buf).cleaner().clean();
    }
}
