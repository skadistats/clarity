package skadistats.clarity.io.bitstream;

import org.testng.annotations.Test;
import skadistats.clarity.io.decoder.BitstreamBuilder;

import static org.testng.Assert.assertEquals;
import static skadistats.clarity.io.decoder.BitstreamBuilder.bitstream;

public class BitStreamSkipVarTest {

    private static void assertVarUIntParity(int leadingSkip, long value) {
        var b = bitstream();
        b.skip(leadingSkip);
        b.addVarULong(value);
        var read = b.build();
        var skip = b.build();
        read.skip(leadingSkip);
        skip.skip(leadingSkip);
        if ((value & 0xFFFF_FFFF_0000_0000L) == 0) {
            read.readVarUInt();
            skip.skipVarUInt();
        } else {
            read.readVarULong();
            skip.skipVarULong();
        }
        assertEquals(skip.pos(), read.pos(),
                "leadingSkip=" + leadingSkip + " value=" + Long.toHexString(value));
    }

    @Test
    public void varUIntByteAligned() {
        for (var v : new long[] {0, 1, 0x7F, 0x80, 300, 0x3FFF, 0x4000, 0x7FFF_FFFFL, 0xFFFF_FFFFL}) {
            assertVarUIntParity(0, v);
        }
    }

    @Test
    public void varUIntBitMisaligned() {
        for (var skip = 1; skip < 8; skip++) {
            for (var v : new long[] {0, 0x7F, 128, 0x4000, 0xFFFF_FFFFL}) {
                assertVarUIntParity(skip, v);
            }
        }
    }

    @Test
    public void varUIntCrossWordBoundary() {
        for (var skip = 55; skip < 72; skip++) {
            for (var v : new long[] {0, 0x7F, 128, 0xFFFF_FFFFL}) {
                assertVarUIntParity(skip, v);
            }
        }
    }

    private static void assertVarULongParity(int leadingSkip, long value) {
        var b = bitstream();
        b.skip(leadingSkip);
        b.addVarULong(value);
        var read = b.build();
        var skip = b.build();
        read.skip(leadingSkip);
        skip.skip(leadingSkip);
        read.readVarULong();
        skip.skipVarULong();
        assertEquals(skip.pos(), read.pos(),
                "leadingSkip=" + leadingSkip + " value=" + Long.toHexString(value));
    }

    @Test
    public void varULongByteAligned() {
        for (var v : new long[] {
                0L, 1L, 0x7FL, 0x80L,
                1L << 14, 1L << 21, 1L << 28, 1L << 35, 1L << 42, 1L << 49, 1L << 56,
                Long.MAX_VALUE, -1L
        }) {
            assertVarULongParity(0, v);
        }
    }

    @Test
    public void varULongBitMisaligned() {
        for (var skip = 1; skip < 8; skip++) {
            for (var v : new long[] {0L, 1L << 35, 1L << 56, -1L}) {
                assertVarULongParity(skip, v);
            }
        }
    }

    @Test
    public void varULongFastPathBoundary() {
        // 8-byte varU64: high bit cleared in byte 7; SWAR fast path resolves.
        assertVarULongParity(0, (1L << 49) - 1);
    }

    @Test
    public void varULongSlowPath9Bytes() {
        // 9-byte varU64: byte 7's continuation bit is set, byte 8's is clear → fast SWAR
        // window resolves at byte 8.
        assertVarULongParity(0, 1L << 56);
    }

    @Test
    public void varULongSlowPath10BytesByteAligned() {
        // 10-byte varU64: all continuation bits set in bytes 0..8; byte 9 always-last.
        // -1L encodes as 10 bytes.
        assertVarULongParity(0, -1L);
    }

    @Test
    public void varULongSlowPath10BytesBitMisaligned() {
        for (var skip = 1; skip < 64; skip++) {
            assertVarULongParity(skip, -1L);
        }
    }
}
