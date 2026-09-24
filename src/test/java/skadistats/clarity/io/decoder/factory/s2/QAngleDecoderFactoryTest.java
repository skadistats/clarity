package skadistats.clarity.io.decoder.factory.s2;

import org.testng.annotations.Test;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.DecoderDispatch;
import skadistats.clarity.io.decoder.QAngleNoScaleDecoder;
import skadistats.clarity.io.decoder.QAnglePreciseDecoder;
import skadistats.clarity.io.s2.S2DecoderFactory;
import skadistats.clarity.model.s2.SerializerId;
import skadistats.clarity.model.s2.SerializerProperties;
import skadistats.clarity.model.Vector;
import skadistats.clarity.protobuf.ByteString;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Regression tests for QAngle fields that declare the qangle_precise encoder together
 * with a bit count of 32. CS2 sends CBodyComponentBaseModelEntity.m_angRotation this way
 * (used by e.g. CFuncConveyor), and the server writes three full floats for it. Decoding
 * it with the precise encoding read 3 bits instead of 96 and desynchronised the rest of
 * the entity, surfacing as "decoder desync: vector length ... exceeds the structural maximum".
 */
public class QAngleDecoderFactoryTest {

    @Test
    public void precise32BitResolvesToNoScaleDecoder() {
        var decoder = S2DecoderFactory.createDecoder(props("qangle_precise", 32), "QAngle");
        assertTrue(
                decoder instanceof QAngleNoScaleDecoder,
                "qangle_precise with 32 bits must resolve to QAngleNoScaleDecoder, got " + decoder.getClass().getSimpleName()
        );
    }

    @Test
    public void precise32BitReadsThreeFullFloats() {
        var raw = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN)
                .putFloat(90.0f)
                .putFloat(0.0f)
                .putFloat(-45.5f)
                .put((byte) 0xAA)
                .array();
        var bs = BitStream.createBitStream(ByteString.copyFrom(raw));

        var decoded = (Vector) DecoderDispatch.decode(bs, S2DecoderFactory.createDecoder(props("qangle_precise", 32), "QAngle"));

        assertEquals(decoded.getElement(0), 90.0f, "pitch");
        assertEquals(decoded.getElement(1), 0.0f, "yaw");
        assertEquals(decoded.getElement(2), -45.5f, "roll");
        assertEquals(bs.pos(), 96,
                "decoder must consume three 32 bit floats; the precise decoder would only consume 3 bits for a zero angle"
        );
        assertEquals(bs.readUBitInt(8), 0xAA, "next byte after the angle must remain readable");
    }

    @Test
    public void precise32BitSkipsThreeFullFloats() {
        var bs = BitStream.createBitStream(ByteString.copyFrom(new byte[13]));
        DecoderDispatch.skip(bs, S2DecoderFactory.createDecoder(props("qangle_precise", 32), "QAngle"));
        assertEquals(bs.pos(), 96);
    }

    @Test
    public void preciseWithoutBitCountStillUsesPreciseDecoder() {
        var decoder = S2DecoderFactory.createDecoder(props("qangle_precise", 0), "QAngle");
        assertTrue(
                decoder instanceof QAnglePreciseDecoder,
                "qangle_precise without a bit count must keep using QAnglePreciseDecoder, got " + decoder.getClass().getSimpleName()
        );
    }

    private static SerializerProperties props(String encoderType, int bitCount) {
        return new SerializerProperties() {
            @Override public Integer getEncodeFlags() { return null; }
            @Override public Integer getBitCount() { return bitCount; }
            @Override public Float getLowValue() { return null; }
            @Override public Float getHighValue() { return null; }
            @Override public String getEncoderType() { return encoderType; }
            @Override public int getEncodeFlagsOrDefault(int defaultValue) { return defaultValue; }
            @Override public int getBitCountOrDefault(int defaultValue) { return bitCount; }
            @Override public float getLowValueOrDefault(float defaultValue) { return defaultValue; }
            @Override public float getHighValueOrDefault(float defaultValue) { return defaultValue; }
            @Override public SerializerId[] getPolymorphicTypes() { return new SerializerId[0]; }
        };
    }

}
