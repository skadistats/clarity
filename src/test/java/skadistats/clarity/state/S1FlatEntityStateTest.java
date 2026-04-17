package skadistats.clarity.state;

import com.google.protobuf.ByteString;
import org.testng.annotations.Test;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.StringLenDecoder;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.PropType;
import skadistats.clarity.model.s1.ReceiveProp;
import skadistats.clarity.model.s1.S1DTClass;
import skadistats.clarity.model.s1.S1FieldPath;
import skadistats.clarity.model.s1.SendProp;
import skadistats.clarity.model.s1.SendTable;
import skadistats.clarity.state.FieldLayout;
import skadistats.clarity.state.s1.S1FlatEntityState;
import skadistats.clarity.state.s1.S1FlatLayout;
import skadistats.clarity.state.s1.S1ObjectArrayEntityState;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class S1FlatEntityStateTest {

    private static final SendTable TABLE = new SendTable("DT_Test", List.of());

    private static SendProp sp(PropType type, String name, int numBits, int flags, int numElements, SendProp template) {
        return new SendProp(TABLE, template, type.ordinal(), name, flags, 0, "DT_Test", numElements, 0f, 1f, numBits);
    }

    private static ReceiveProp rp(SendProp sp) {
        return new ReceiveProp(sp, sp.getVarName());
    }

    private static S1DTClass dtClass(SendProp... props) {
        var rps = new ReceiveProp[props.length];
        for (var i = 0; i < props.length; i++) rps[i] = rp(props[i]);
        var dt = new S1DTClass("DT_Test", TABLE);
        dt.setReceiveProps(rps);
        return dt;
    }

    private static S1FieldPath fp(int idx) {
        return new S1FieldPath(idx);
    }

    private static BitStream encodedStringStream(String s) {
        var bytes = s.getBytes(StandardCharsets.UTF_8);
        var totalBits = 9 + bytes.length * 8;
        var buf = new byte[(totalBits + 7) / 8 + 32];
        writeBits(buf, 0, bytes.length, 9);
        for (var i = 0; i < bytes.length; i++) {
            writeBits(buf, 9 + i * 8, bytes[i] & 0xFF, 8);
        }
        return BitStream.createBitStream(ByteString.copyFrom(buf));
    }

    private static void writeBits(byte[] buf, int bitPos, int value, int nBits) {
        for (var i = 0; i < nBits; i++) {
            var bit = (value >>> i) & 1;
            var byteIdx = (bitPos + i) >>> 3;
            var bitIdx = (bitPos + i) & 7;
            buf[byteIdx] |= (byte) (bit << bitIdx);
        }
    }

    // -------- S1FlatLayout build classification --------

    @Test
    public void layoutClassifiesPrimitiveString512AndArrayAsRef() {
        var intProp = sp(PropType.INT, "i", 32, 0, 0, null);
        var strProp = sp(PropType.STRING, "s", 0, 0, 0, null);
        var innerInt = sp(PropType.INT, "_inner", 32, 0, 0, null);
        var arrProp = sp(PropType.ARRAY, "a", 0, 0, 4, innerInt);
        var dt = dtClass(intProp, strProp, arrProp);

        var layout = dt.getFlatLayout();
        assertTrue(layout.leaves()[0] instanceof FieldLayout.Primitive);
        assertTrue(layout.leaves()[1] instanceof FieldLayout.InlineString);
        assertTrue(layout.leaves()[2] instanceof FieldLayout.Ref);
        assertEquals(layout.refSlots(), 1, "one REF leaf for the ARRAY prop");
        assertEquals(((FieldLayout.InlineString) layout.leaves()[1]).maxLength(), S1FlatLayout.INLINE_STRING_MAX_LENGTH);
        // offsets: int → 5 bytes (1 flag + 4); string → 515 (1 flag + 2 + 512); ref → 5 (1 flag + 4)
        assertEquals(((FieldLayout.Primitive) layout.leaves()[0]).offset(), 0);
        assertEquals(((FieldLayout.InlineString) layout.leaves()[1]).offset(), 5);
        assertEquals(((FieldLayout.Ref) layout.leaves()[2]).offset(), 5 + 515);
        assertEquals(layout.dataBytes(), 5 + 515 + 5);
    }

    @Test
    public void layoutIsCachedPerDtClass() {
        var dt = dtClass(sp(PropType.INT, "i", 32, 0, 0, null));
        assertSame(dt.getFlatLayout(), dt.getFlatLayout(), "layout cached on first access");
    }

    // -------- primitive round-trip --------

    @Test
    public void primitiveIntRoundTrip() {
        var dt = dtClass(sp(PropType.INT, "i", 32, 0, 0, null));
        var st = new S1FlatEntityState(dt);
        st.write(fp(0), 12345);
        assertEquals((Integer) st.getValueForFieldPath(fp(0)), Integer.valueOf(12345));
    }

    @Test
    public void primitiveFloatRoundTrip() {
        var dt = dtClass(sp(PropType.FLOAT, "f", 32, PropFlag.NO_SCALE, 0, null));
        var st = new S1FlatEntityState(dt);
        st.write(fp(0), 3.14f);
        assertEquals((Float) st.getValueForFieldPath(fp(0)), Float.valueOf(3.14f));
    }

    @Test
    public void primitiveLongRoundTrip() {
        var dt = dtClass(sp(PropType.INT64, "l", 64, 0, 0, null));
        var st = new S1FlatEntityState(dt);
        st.write(fp(0), 9_999_999_999L);
        assertEquals((Long) st.getValueForFieldPath(fp(0)), Long.valueOf(9_999_999_999L));
    }

    @Test
    public void primitiveVectorRoundTrip() {
        var dt = dtClass(sp(PropType.VECTOR, "v", 32, PropFlag.NO_SCALE, 0, null));
        var st = new S1FlatEntityState(dt);
        var vec = new Vector(new float[]{1.0f, 2.0f, 3.0f});
        st.write(fp(0), vec);
        assertEquals((Vector) st.getValueForFieldPath(fp(0)), vec);
    }

    // -------- inline-string round-trip + parity vs StringLenDecoder.decode --------

    @Test
    public void inlineStringWriteThenRead() {
        var dt = dtClass(sp(PropType.STRING, "s", 0, 0, 0, null));
        var st = new S1FlatEntityState(dt);
        st.write(fp(0), "hello, clarity");
        assertEquals((String) st.getValueForFieldPath(fp(0)), "hello, clarity");
    }

    @Test
    public void inlineStringDecodeIntoMatchesStringLenDecoderDecode() {
        var dt = dtClass(sp(PropType.STRING, "s", 0, 0, 0, null));
        var st = new S1FlatEntityState(dt);
        var decoder = new StringLenDecoder();

        var bsA = encodedStringStream("hello, clarity");
        var bsB = encodedStringStream("hello, clarity");
        var viaDecode = StringLenDecoder.decode(bsA);
        st.decodeInto(fp(0), decoder, bsB);

        assertEquals(bsB.pos(), bsA.pos(), "both paths consume identical bits");
        assertEquals((String) st.getValueForFieldPath(fp(0)), viaDecode);
    }

    @Test
    public void inlineStringEmptyDistinguishesFromUnset() {
        var dt = dtClass(sp(PropType.STRING, "s", 0, 0, 0, null));
        var st = new S1FlatEntityState(dt);
        assertNull(st.getValueForFieldPath(fp(0)), "unset slot reads back null");
        st.write(fp(0), "");
        assertEquals((String) st.getValueForFieldPath(fp(0)), "");
    }

    @Test
    public void decodeIntoOnRefThrows() {
        var inner = sp(PropType.INT, "_inner", 32, 0, 0, null);
        var dt = dtClass(sp(PropType.ARRAY, "a", 0, 0, 4, inner));
        var st = new S1FlatEntityState(dt);
        assertThrows(IllegalStateException.class,
            () -> st.decodeInto(fp(0), inner.getDecoder(), encodedStringStream("x")));
    }

    // -------- REF slot lifecycle --------

    @Test
    public void refSlotAllocatesOnceThenOverwrites() {
        var inner = sp(PropType.INT, "_inner", 32, 0, 0, null);
        var dt = dtClass(sp(PropType.ARRAY, "a", 0, 0, 4, inner));
        var st = new S1FlatEntityState(dt);

        var first = new Object[]{1, 2, 3};
        var second = new Object[]{4, 5, 6};
        st.write(fp(0), first);
        assertSame(st.getValueForFieldPath(fp(0)), first);
        st.write(fp(0), second);
        assertSame(st.getValueForFieldPath(fp(0)), second);
    }

    @Test
    public void refSlotIsolatedAcrossMultipleArrayProps() {
        var inner = sp(PropType.INT, "_inner", 32, 0, 0, null);
        var dt = dtClass(
            sp(PropType.ARRAY, "a0", 0, 0, 4, inner),
            sp(PropType.ARRAY, "a1", 0, 0, 4, inner)
        );
        var st = new S1FlatEntityState(dt);
        var v0 = new Object[]{"a"};
        var v1 = new Object[]{"b"};
        st.write(fp(0), v0);
        st.write(fp(1), v1);
        assertSame(st.getValueForFieldPath(fp(0)), v0);
        assertSame(st.getValueForFieldPath(fp(1)), v1);
    }

    // -------- copy independence --------

    @Test
    public void copyIsIndependentForPrimitive() {
        var dt = dtClass(sp(PropType.INT, "i", 32, 0, 0, null));
        var st = new S1FlatEntityState(dt);
        st.write(fp(0), 7);

        var cp = (S1FlatEntityState) st.copy();
        cp.write(fp(0), 9);
        assertEquals((Integer) st.getValueForFieldPath(fp(0)), Integer.valueOf(7));
        assertEquals((Integer) cp.getValueForFieldPath(fp(0)), Integer.valueOf(9));

        st.write(fp(0), 11);
        assertEquals((Integer) cp.getValueForFieldPath(fp(0)), Integer.valueOf(9));
    }

    @Test
    public void copyIsIndependentForInlineString() {
        var dt = dtClass(sp(PropType.STRING, "s", 0, 0, 0, null));
        var st = new S1FlatEntityState(dt);
        st.write(fp(0), "original");

        var cp = (S1FlatEntityState) st.copy();
        cp.write(fp(0), "replaced");
        assertEquals((String) st.getValueForFieldPath(fp(0)), "original");
        assertEquals((String) cp.getValueForFieldPath(fp(0)), "replaced");
    }

    @Test
    public void copyIsIndependentForRef() {
        var inner = sp(PropType.INT, "_inner", 32, 0, 0, null);
        var dt = dtClass(sp(PropType.ARRAY, "a", 0, 0, 4, inner));
        var st = new S1FlatEntityState(dt);
        var orig = new Object[]{"x"};
        st.write(fp(0), orig);

        var cp = (S1FlatEntityState) st.copy();
        var replaced = new Object[]{"y"};
        cp.write(fp(0), replaced);
        assertSame(st.getValueForFieldPath(fp(0)), orig, "original keeps its ref");
        assertSame(cp.getValueForFieldPath(fp(0)), replaced, "copy gets the new ref");
        assertNotSame(st, cp);
    }

    // -------- fieldPathIterator --------

    @Test
    public void fieldPathIteratorMatchesS1ObjectArrayEntityState() {
        var dt = dtClass(
            sp(PropType.INT, "i", 32, 0, 0, null),
            sp(PropType.STRING, "s", 0, 0, 0, null),
            sp(PropType.FLOAT, "f", 32, PropFlag.NO_SCALE, 0, null)
        );
        var flat = new S1FlatEntityState(dt);
        var oa = new S1ObjectArrayEntityState(dt.getReceiveProps().length);

        var it1 = flat.fieldPathIterator();
        var it2 = oa.fieldPathIterator();
        var i = 0;
        while (it1.hasNext() && it2.hasNext()) {
            assertEquals(it1.next(), it2.next(), "fp[" + i + "]");
            i++;
        }
        assertFalse(it1.hasNext());
        assertFalse(it2.hasNext());
        assertEquals(i, 3);
    }

    @Test
    public void fieldPathIteratorYieldsAllSlotsEvenWhenUnset() {
        var dt = dtClass(
            sp(PropType.INT, "i", 32, 0, 0, null),
            sp(PropType.INT, "j", 32, 0, 0, null)
        );
        var st = new S1FlatEntityState(dt);
        // No writes at all
        Iterator<?> it = st.fieldPathIterator();
        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertFalse(it.hasNext());
    }

    // -------- applyMutation delegates --------

    @Test
    public void applyMutationWriteValueDelegatesToWrite() {
        var dt = dtClass(sp(PropType.INT, "i", 32, 0, 0, null));
        var st = new S1FlatEntityState(dt);
        st.applyMutation(fp(0), new StateMutation.WriteValue(42));
        assertEquals((Integer) st.getValueForFieldPath(fp(0)), Integer.valueOf(42));
    }

    @Test
    public void applyMutationNonWriteValueThrowsClassCast() {
        var dt = dtClass(sp(PropType.INT, "i", 32, 0, 0, null));
        var st = new S1FlatEntityState(dt);
        assertThrows(ClassCastException.class,
            () -> st.applyMutation(fp(0), new StateMutation.ResizeVector(0)));
    }
}
