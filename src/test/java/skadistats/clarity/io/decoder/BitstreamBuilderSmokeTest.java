package skadistats.clarity.io.decoder;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static skadistats.clarity.io.decoder.BitstreamBuilder.bitstream;

public class BitstreamBuilderSmokeTest {

    @Test
    public void byteAlignedReadback() {
        var bs = bitstream()
                .add(0xA5, 8)
                .add(0x3C, 8)
                .build();
        assertEquals(bs.readUBitInt(8), 0xA5);
        assertEquals(bs.readUBitInt(8), 0x3C);
        assertEquals(bs.pos(), 16);
    }

    @Test
    public void bitMisalignedReadback() {
        var bs = bitstream()
                .skip(3)
                .add(0xA, 4)
                .add(0x123, 12)
                .build();
        assertEquals(bs.pos(), 0);
        bs.skip(3);
        assertEquals(bs.readUBitInt(4), 0xA);
        assertEquals(bs.readUBitInt(12), 0x123);
        assertEquals(bs.pos(), 19);
    }

    @Test
    public void varUIntRoundTrip() {
        var bs = bitstream()
                .addVarUInt(0)
                .addVarUInt(127)
                .addVarUInt(128)
                .addVarUInt(300)
                .addVarUInt(0x7FFF_FFFF)
                .addVarUInt(0xFFFF_FFFF)
                .build();
        assertEquals(bs.readVarUInt(), 0);
        assertEquals(bs.readVarUInt(), 127);
        assertEquals(bs.readVarUInt(), 128);
        assertEquals(bs.readVarUInt(), 300);
        assertEquals(bs.readVarUInt(), 0x7FFF_FFFF);
        assertEquals(bs.readVarUInt(), 0xFFFF_FFFF);
    }

    @Test
    public void varULongRoundTrip() {
        var bs = bitstream()
                .addVarULong(0L)
                .addVarULong(127L)
                .addVarULong(1L << 35)
                .addVarULong(-1L)
                .build();
        assertEquals(bs.readVarULong(), 0L);
        assertEquals(bs.readVarULong(), 127L);
        assertEquals(bs.readVarULong(), 1L << 35);
        assertEquals(bs.readVarULong(), -1L);
    }

    @Test
    public void stringZRoundTrip() {
        var bs = bitstream()
                .addStringZ("hello")
                .add(0xDEAD, 16)
                .build();
        assertEquals(bs.readString(64), "hello");
        assertEquals(bs.readUBitInt(16), 0xDEAD);
    }

    @Test
    public void crossByteBoundary() {
        var bs = bitstream()
                .add(0x1, 1)
                .add(0xFFFF_FFFFL, 32)
                .build();
        assertEquals(bs.readBit(), 1);
        assertEquals(bs.readUBitLong(32), 0xFFFF_FFFFL);
        assertEquals(bs.pos(), 33);
    }
}
