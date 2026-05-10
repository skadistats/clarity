package skadistats.clarity.io.bitstream;

import skadistats.clarity.model.s2.FieldOpHuffmanTree;
import skadistats.clarity.platform.buffer.Buffer;

public class BitStream64 extends BitStream {

    private final Buffer.B64 buffer;

    public BitStream64(Buffer.B64 buffer) {
        this.buffer = buffer;
    }

    protected int peekBit(int pos) {
        return (int) (buffer.get(pos >> 6) >> (pos & 63) & 1);
    }

    @Override
    public int readUBitInt(int n) {
        assert n <= 32;
        var start = pos >> 6;
        var end = (pos + n - 1) >> 6;
        var s = pos & 63;
        pos += n;
        return (int)(((buffer.get(start) >>> s) | (buffer.get(end) << (64 - s))) & (-1L >>> (64 - n)));
    }

    @Override
    public long readUBitLong(int n) {
        assert n <= 64;
        var start = pos >> 6;
        var end = (pos + n - 1) >> 6;
        var s = pos & 63;
        pos += n;
        return ((buffer.get(start) >>> s) | (buffer.get(end) << (64 - s))) & (-1L >>> (64 - n));
    }

    @Override
    public void readBitsIntoByteArray(byte[] dest, int n) {
        var o = 0;
        var idx = pos >> 6;
        var s = pos & 63;
        var bulk = n >>> 6;
        if (bulk > 0) {
            if (s == 0) {
                buffer.copyBytesInto(idx << 3, dest, o, bulk << 3);
                o += bulk << 3;
            } else {
                var prev = buffer.get(idx);
                for (var k = 0; k < bulk; k++) {
                    var next = buffer.get(idx + k + 1);
                    buffer.put(dest, o, (prev >>> s) | (next << -s));
                    o += 8;
                    prev = next;
                }
            }
            pos += bulk << 6;
            n   -= bulk << 6;
        }
        while (n > 8) {
            dest[o++] = (byte)readUBitInt(8);
            n -= 8;
        }
        if (n > 0) {
            dest[o] = (byte)readUBitInt(n);
        }
    }


    @Override
    public int readFieldOpId() {
        var offs = pos >> 6;
        var s = pos & 63;
        var v = buffer.get(offs);

        // Fast path: 8-bit lookup resolves ~99.7% of ops in one step
        var peek = (v >>> s) | (s > 56 ? buffer.get(offs + 1) << (64 - s) : 0);
        var entry = FieldOpHuffmanTree.lookup[(int)(peek & 0xFFL)];
        var bits = entry & 0xFF;
        if (bits != 0) {
            pos += bits;
            return (entry >>> 8) & 0xFF;
        }

        // Slow path: skip lookup bits, continue tree walk from saved node
        var i = (entry >>> 8) & 0xFF;
        pos += FieldOpHuffmanTree.LOOKUP_BITS;
        offs = pos >> 6;
        s = pos & 63;
        v = buffer.get(offs);
        while (true) {
            pos++;
            i = FieldOpHuffmanTree.tree[(i << 1) | (int)((v >>> s) & 1L)];
            if (i < 0) {
                return -i - 1;
            }
            if (++s == 64) {
                v = buffer.get(++offs);
                s = 0;
            }
        }
    }

}
