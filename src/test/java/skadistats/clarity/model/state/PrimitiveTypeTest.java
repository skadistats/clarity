package skadistats.clarity.model.state;

import org.testng.annotations.Test;
import skadistats.clarity.model.Vector;

import static org.testng.Assert.assertEquals;

public class PrimitiveTypeTest {

    // 7.2.1 Scalar roundtrip

    @Test
    public void scalarIntRoundtrip() {
        var buf = new byte[4];
        PrimitiveType.Scalar.INT.write(buf, 0, 42);
        assertEquals(PrimitiveType.Scalar.INT.read(buf, 0), 42);
    }

    @Test
    public void scalarFloatRoundtrip() {
        var buf = new byte[4];
        PrimitiveType.Scalar.FLOAT.write(buf, 0, 3.14f);
        assertEquals(PrimitiveType.Scalar.FLOAT.read(buf, 0), 3.14f);
    }

    @Test
    public void scalarLongRoundtrip() {
        var buf = new byte[8];
        PrimitiveType.Scalar.LONG.write(buf, 0, 1234567890123L);
        assertEquals(PrimitiveType.Scalar.LONG.read(buf, 0), 1234567890123L);
    }

    @Test
    public void scalarBoolRoundtripTrue() {
        var buf = new byte[1];
        PrimitiveType.Scalar.BOOL.write(buf, 0, true);
        assertEquals(PrimitiveType.Scalar.BOOL.read(buf, 0), true);
    }

    @Test
    public void scalarBoolRoundtripFalse() {
        var buf = new byte[1];
        PrimitiveType.Scalar.BOOL.write(buf, 0, false);
        assertEquals(PrimitiveType.Scalar.BOOL.read(buf, 0), false);
    }

    // 7.2.2 VectorType roundtrip

    @Test
    public void vectorType2Roundtrip() {
        var type = new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 2);
        var buf = new byte[type.size()];
        type.write(buf, 0, new Vector(1.5f, -2.5f));
        var result = (Vector) type.read(buf, 0);
        assertEquals(result.getDimension(), 2);
        assertEquals(result.getElement(0), 1.5f);
        assertEquals(result.getElement(1), -2.5f);
    }

    @Test
    public void vectorType3Roundtrip() {
        var type = new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3);
        var buf = new byte[type.size()];
        type.write(buf, 0, new Vector(1.0f, 2.0f, 3.0f));
        var result = (Vector) type.read(buf, 0);
        assertEquals(result, new Vector(1.0f, 2.0f, 3.0f));
    }

    @Test
    public void vectorType4Roundtrip() {
        var type = new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 4);
        var buf = new byte[type.size()];
        type.write(buf, 0, new Vector(1f, 2f, 3f, 4f));
        var result = (Vector) type.read(buf, 0);
        assertEquals(result, new Vector(1f, 2f, 3f, 4f));
    }

    // 7.2.3 Boundary values

    @Test
    public void scalarIntMaxValue() {
        var buf = new byte[4];
        PrimitiveType.Scalar.INT.write(buf, 0, Integer.MAX_VALUE);
        assertEquals(PrimitiveType.Scalar.INT.read(buf, 0), Integer.MAX_VALUE);
    }

    @Test
    public void scalarIntMinValue() {
        var buf = new byte[4];
        PrimitiveType.Scalar.INT.write(buf, 0, Integer.MIN_VALUE);
        assertEquals(PrimitiveType.Scalar.INT.read(buf, 0), Integer.MIN_VALUE);
    }

    @Test
    public void scalarFloatNaN() {
        var buf = new byte[4];
        PrimitiveType.Scalar.FLOAT.write(buf, 0, Float.NaN);
        var v = (Float) PrimitiveType.Scalar.FLOAT.read(buf, 0);
        assertEquals(Float.isNaN(v), true);
    }

    @Test
    public void scalarLongMinValue() {
        var buf = new byte[8];
        PrimitiveType.Scalar.LONG.write(buf, 0, Long.MIN_VALUE);
        assertEquals(PrimitiveType.Scalar.LONG.read(buf, 0), Long.MIN_VALUE);
    }

    @Test
    public void vectorTypeNegativeComponents() {
        var type = new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3);
        var buf = new byte[type.size()];
        type.write(buf, 0, new Vector(-1f, -2f, -3f));
        assertEquals(type.read(buf, 0), new Vector(-1f, -2f, -3f));
    }

    // 7.2.4 Presence semantics: unwritten slot reads as zero bytes

    @Test
    public void scalarIntUnwrittenReadsAsZero() {
        var buf = new byte[4];
        assertEquals(PrimitiveType.Scalar.INT.read(buf, 0), 0);
    }

    @Test
    public void scalarFloatUnwrittenReadsAsZero() {
        var buf = new byte[4];
        assertEquals(PrimitiveType.Scalar.FLOAT.read(buf, 0), 0.0f);
    }

    @Test
    public void scalarLongUnwrittenReadsAsZero() {
        var buf = new byte[8];
        assertEquals(PrimitiveType.Scalar.LONG.read(buf, 0), 0L);
    }

    @Test
    public void scalarBoolUnwrittenReadsAsFalse() {
        var buf = new byte[1];
        assertEquals(PrimitiveType.Scalar.BOOL.read(buf, 0), false);
    }

    @Test
    public void vectorTypeUnwrittenReadsAsZeroes() {
        var type = new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3);
        var buf = new byte[type.size()];
        assertEquals(type.read(buf, 0), new Vector(0f, 0f, 0f));
    }
}
