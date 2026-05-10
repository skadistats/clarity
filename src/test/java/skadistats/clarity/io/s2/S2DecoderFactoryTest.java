package skadistats.clarity.io.s2;

import com.google.protobuf.ByteString;
import org.testng.annotations.Test;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.CUtlBinaryBlockDecoder;
import skadistats.clarity.io.decoder.StringZeroTerminatedDecoder;

import java.nio.charset.StandardCharsets;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * Regression tests for the S2 field types added in CS2 build 10772+ that
 * previously fell through to the {@code IntVarUnsignedDecoder} fallback and
 * desynchronised the entity decoder. See discussion under issue:
 * "Entity not found for update at index N. Entity update cannot be parsed!".
 */
public class S2DecoderFactoryTest {

    @Test
    public void cUtlBinaryBlockResolvesToBinaryBlobDecoder() {
        var holder = S2DecoderFactory.createDecoder("CUtlBinaryBlock");
        assertTrue(
                holder.getDecoder() instanceof CUtlBinaryBlockDecoder,
                "CUtlBinaryBlock must resolve to CUtlBinaryBlockDecoder, not the int fallback"
        );
    }

    @Test
    public void cUtlBinaryBlockReadsLengthPrefixedPayloadAndAdvancesByLengthPlusBytes() {
        // Wire format: varint length N, then N raw bytes.
        // Build a stream: length=4 (single varint byte 0x04), then 4 bytes [0xDE, 0xAD, 0xBE, 0xEF].
        var raw = new byte[]{0x04, (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
        var bs = BitStream.createBitStream(ByteString.copyFrom(raw));

        var decoded = (byte[]) S2DecoderFactory
                .createDecoder("CUtlBinaryBlock")
                .getDecoder()
                .decode(bs);

        assertEquals(decoded.length, 4, "decoded payload length");
        assertEquals(decoded, new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF}, "payload bytes");
        assertEquals(bs.pos(), 5 * 8,
                "decoder must advance the bitstream by length-varint + payload bytes; "
                        + "the int fallback would only advance by 8 bits and leave the payload behind, desyncing the rest of the entity"
        );
    }

    @Test
    public void cGlobalSymbolResolvesToStringDecoderAndReadsNullTerminated() {
        var holder = S2DecoderFactory.createDecoder("CGlobalSymbol");
        assertTrue(
                holder.getDecoder() instanceof StringZeroTerminatedDecoder,
                "CGlobalSymbol must resolve to a string decoder, not the int fallback"
        );

        // ASCII "hello" + NUL + a trailing byte that must NOT be consumed.
        var raw = new byte[]{'h', 'e', 'l', 'l', 'o', 0x00, (byte) 0xAA};
        var bs = BitStream.createBitStream(ByteString.copyFrom(raw));

        var decoded = (String) holder.getDecoder().decode(bs);

        assertEquals(decoded, "hello", "decoded string");
        assertEquals(bs.pos(), 6 * 8,
                "decoder must consume exactly the string bytes plus the terminator (6 bytes), "
                        + "not the entire 7-byte buffer"
        );
        // Confirm the trailing byte survived by reading the next 8 bits manually.
        assertEquals(bs.readUBitInt(8), 0xAA, "next byte after terminator must remain readable");
    }

    @Test
    public void registeredTypesAreNotTheDefaultIntFallback() {
        var fallback = S2DecoderFactory.createDecoder("ThisTypeDoesNotExistAndNeverWill_t").getDecoder();

        var binaryBlock = S2DecoderFactory.createDecoder("CUtlBinaryBlock").getDecoder();
        var globalSymbol = S2DecoderFactory.createDecoder("CGlobalSymbol").getDecoder();

        assertTrue(binaryBlock != fallback, "CUtlBinaryBlock must not be the int fallback");
        assertTrue(globalSymbol != fallback, "CGlobalSymbol must not be the int fallback");

        var fallback2 = S2DecoderFactory.createDecoder("AnotherUnknown_t").getDecoder();
        assertSame(fallback, fallback2, "unknown types must share the single default decoder instance");
    }
}
