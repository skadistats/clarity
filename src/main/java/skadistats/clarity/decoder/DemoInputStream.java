package skadistats.clarity.decoder;

import com.google.protobuf.CodedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class DemoInputStream {

    private final InputStream stream;
    private int offset = 0;

    public DemoInputStream(InputStream stream) {
        this.stream = stream;
    }

    public CodedInputStream newCodedInputStream() {
        CodedInputStream cis = CodedInputStream.newInstance(stream);
        cis.setSizeLimit(Integer.MAX_VALUE);
        return cis;
    }

    public int getOffset() {
        return offset;
    }

    public byte readByte() throws IOException {
        int i = stream.read();
        if (i == -1) {
            throw new IOException("unexpected end of file!");
        }
        offset++;
        return (byte) i;
    }

    public byte[] readBytes(int size) throws IOException {
        byte[] result = new byte[size];
        int n = size;
        while (n > 0) {
            int r = stream.read(result, size - n, n);
            if (r == -1) {
                throw new IOException("unexpected end of file!");
            }
            offset += r;
            n -= r;
        }
        return result;
    }

    public void skipBytes(int num) throws IOException {
        int n = num;
        while (n > 0) {
            long r = stream.skip(n);
            if (r == 0) {
                throw new IOException("unexpected end of file!");
            }
            offset += r;
            n -= r;
        }
    }

    public int ensureDemHeader() throws IOException {
        byte[] header = readBytes(12);
        if (!"PBUFDEM\0".equals(new String(Arrays.copyOfRange(header, 0, 8)))) {
            throw new IOException("given stream does not seem to contain a valid replay");
        }
        return ByteBuffer.wrap(header, 8, 4).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get();
    }

}
