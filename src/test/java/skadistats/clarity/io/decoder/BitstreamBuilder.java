package skadistats.clarity.io.decoder;

import skadistats.clarity.protobuf.ByteString;
import skadistats.clarity.io.bitstream.BitStream;

import java.nio.charset.StandardCharsets;

/**
 * Test-only declarative builder for constructing wire-format-valid bit patterns.
 * Produces a {@link BitStream} positioned at bit 0 of the constructed pattern.
 *
 * <p>Buffer is little-endian (matches {@code Buffer.get(int)} byte ordering),
 * so {@code add(value, bits)} writes the low {@code bits} bits of {@code value}
 * starting at the current bit cursor.
 */
public final class BitstreamBuilder {

    private byte[] buf = new byte[64];
    private int bitPos;

    public static BitstreamBuilder bitstream() {
        return new BitstreamBuilder();
    }

    public BitstreamBuilder skip(int n) {
        if (n < 0) throw new IllegalArgumentException("n=" + n);
        ensureCapacity(n);
        bitPos += n;
        return this;
    }

    public BitstreamBuilder add(long value, int bits) {
        if (bits < 0 || bits > 64) throw new IllegalArgumentException("bits=" + bits);
        ensureCapacity(bits);
        if (bits < 64) value &= (1L << bits) - 1;
        var remaining = bits;
        while (remaining > 0) {
            var byteIdx = bitPos >>> 3;
            var bitOffs = bitPos & 7;
            var chunk = Math.min(8 - bitOffs, remaining);
            var chunkVal = (int) (value & ((1L << chunk) - 1));
            buf[byteIdx] |= (byte) (chunkVal << bitOffs);
            value >>>= chunk;
            bitPos += chunk;
            remaining -= chunk;
        }
        return this;
    }

    public BitstreamBuilder addVarUInt(int value) {
        return addVarU(value & 0xFFFF_FFFFL);
    }

    public BitstreamBuilder addVarULong(long value) {
        return addVarU(value);
    }

    private BitstreamBuilder addVarU(long value) {
        while (true) {
            var b = value & 0x7FL;
            value >>>= 7;
            if (value != 0L) {
                add(b | 0x80L, 8);
            } else {
                add(b, 8);
                return this;
            }
        }
    }

    public BitstreamBuilder addStringZ(String s) {
        var bytes = s.getBytes(StandardCharsets.UTF_8);
        for (var b : bytes) add(b & 0xFFL, 8);
        return add(0L, 8);
    }

    public int bitLength() {
        return bitPos;
    }

    public BitStream build() {
        // +8 trailing zero bytes so a 64-bit peek at the last logical bit reads zeros
        // past the meaningful payload (rather than going out of bounds).
        var byteLen = ((bitPos + 7) >>> 3) + 8;
        var out = new byte[byteLen];
        System.arraycopy(buf, 0, out, 0, ((bitPos + 7) >>> 3));
        return BitStream.createBitStream(ByteString.copyFrom(out));
    }

    private void ensureCapacity(int bitsNeeded) {
        var needed = ((bitPos + bitsNeeded + 7) >>> 3) + 8;
        if (needed > buf.length) {
            var grown = new byte[Math.max(needed, buf.length * 2)];
            System.arraycopy(buf, 0, grown, 0, buf.length);
            buf = grown;
        }
    }
}
