package skadistats.clarity.io.s2;

import com.google.protobuf.ByteString;
import org.testng.annotations.Test;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.CUtlBinaryBlockDecoder;
import skadistats.clarity.io.decoder.StringZeroTerminatedDecoder;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class S2DecoderFactoryTest {

    @Test
    public void cUtlBinaryBlockResolvesToBinaryBlobDecoder() {
        var decoder = S2DecoderFactory.createDecoder("CUtlBinaryBlock");
        assertTrue(
                decoder instanceof CUtlBinaryBlockDecoder,
                "CUtlBinaryBlock must resolve to CUtlBinaryBlockDecoder, not the int fallback"
        );
    }

    @Test
    public void cUtlBinaryBlockReadsLengthPrefixedPayloadAndAdvancesByLengthPlusBytes() {
        var raw = new byte[]{0x04, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
        var bs = BitStream.createBitStream(ByteString.copyFrom(raw));

        var decoded = CUtlBinaryBlockDecoder.decode(bs);

        assertEquals(decoded.length, 4, "decoded payload length");
        assertEquals(decoded, new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF}, "payload bytes");
        assertEquals(bs.pos(), 5 * 8,
                "decoder must advance the bitstream by length-varint + payload bytes; "
                        + "the int fallback would only advance by 8 bits and leave the payload behind, desyncing the rest of the entity"
        );
    }

    @Test
    public void cGlobalSymbolResolvesToStringDecoder() {
        var decoder = S2DecoderFactory.createDecoder("CGlobalSymbol");
        assertTrue(
                decoder instanceof StringZeroTerminatedDecoder,
                "CGlobalSymbol must resolve to a string decoder, not the int fallback"
        );

        var raw = new byte[]{'h', 'e', 'l', 'l', 'o', 0x00, (byte) 0xAA};
        var bs = BitStream.createBitStream(ByteString.copyFrom(raw));

        var decoded = StringZeroTerminatedDecoder.decode(bs);

        assertEquals(decoded, "hello", "decoded string");
        assertEquals(bs.pos(), 6 * 8,
                "decoder must consume exactly the string bytes plus the terminator (6 bytes), "
                        + "not the entire 7-byte buffer"
        );
        assertEquals(bs.readUBitInt(8), 0xAA, "next byte after terminator must remain readable");
    }

    @Test
    public void registeredTypesAreNotTheDefaultIntFallback() {
        var fallback = S2DecoderFactory.createDecoder("ThisTypeDoesNotExistAndNeverWill_t");

        var binaryBlock = S2DecoderFactory.createDecoder("CUtlBinaryBlock");
        var globalSymbol = S2DecoderFactory.createDecoder("CGlobalSymbol");

        assertTrue(binaryBlock != fallback, "CUtlBinaryBlock must not be the int fallback");
        assertTrue(globalSymbol != fallback, "CGlobalSymbol must not be the int fallback");

        var fallback2 = S2DecoderFactory.createDecoder("AnotherUnknown_t");
        assertSame(fallback, fallback2, "unknown types must share the single default decoder instance");
    }
}
