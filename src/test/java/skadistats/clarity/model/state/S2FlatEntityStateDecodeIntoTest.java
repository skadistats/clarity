package skadistats.clarity.model.state;

import com.google.protobuf.ByteString;
import org.testng.annotations.Test;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.DecoderDispatch;
import skadistats.clarity.io.s2.Serializer;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;

import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static skadistats.clarity.model.state.TestFields.*;

public class S2FlatEntityStateDecodeIntoTest {

    private static final long SEED = 0xC01D_CAFEL;

    private static S2FlatEntityState makeFlat(Serializer root) {
        var rf = rootField(root);
        var built = new FieldLayoutBuilder().buildSerializer(rf.getSerializer());
        return new S2FlatEntityState(rf, 1024, built.layout(), built.totalBytes());
    }

    private static BitStream freshStream() {
        var r = new Random(SEED);
        var b = new byte[256];
        r.nextBytes(b);
        return BitStream.createBitStream(ByteString.copyFrom(b));
    }

    private static BitStream streamFor(long seed) {
        var r = new Random(seed);
        var b = new byte[256];
        r.nextBytes(b);
        return BitStream.createBitStream(ByteString.copyFrom(b));
    }

    private static Object read(S2FlatEntityState s, S2FieldPath fp) {
        return s.getValueForFieldPath(fp);
    }

    // ---------- 4.6 / 4.8: decodeInto parity + capacity-change ----------

    @Test
    public void decodeIntoPrimitiveAtRootMatchesApplyMutation() {
        var ser = serializer("S", named("a", intField()));
        var stA = makeFlat(ser);
        var stB = makeFlat(ser);

        var decoder = intDecoder();
        var bsA = freshStream();
        var bsB = freshStream();

        var decoded = DecoderDispatch.decode(bsA, decoder);
        var capA = stA.applyMutation(fp(0), new StateMutation.WriteValue(decoded));
        var capB = stB.decodeInto(fp(0), decoder, bsB);

        assertEquals(bsB.pos(), bsA.pos(), "both paths consume identical bits");
        assertEquals(stB.rootDataForTest(), stA.rootDataForTest(), "byte-identical root data");
        assertEquals(capB, capA, "capacity-change return matches");
        assertTrue(capB, "null→value transition returns true");
    }

    @Test
    public void decodeIntoFloatPrimitiveMatchesApplyMutation() {
        var ser = serializer("S", named("f", floatField()));
        var stA = makeFlat(ser);
        var stB = makeFlat(ser);

        var decoder = floatDecoder();
        var bsA = freshStream();
        var bsB = freshStream();

        var decoded = DecoderDispatch.decode(bsA, decoder);
        stA.applyMutation(fp(0), new StateMutation.WriteValue(decoded));
        stB.decodeInto(fp(0), decoder, bsB);

        assertEquals(stB.rootDataForTest(), stA.rootDataForTest(), "byte-identical root data");
    }

    @Test
    public void decodeIntoPrimitiveInsideVectorMatchesApplyMutation() {
        var element = serializer("E", named("x", intField()));
        var ser = serializer("S", named("v", vectorFieldOf(serializerField(element))));

        var stA = makeFlat(ser);
        var stB = makeFlat(ser);
        stA.applyMutation(fp(0), new StateMutation.ResizeVector(4));
        stB.applyMutation(fp(0), new StateMutation.ResizeVector(4));

        var decoder = intDecoder();
        var bsA = freshStream();
        var bsB = freshStream();
        var decoded = DecoderDispatch.decode(bsA, decoder);
        stA.applyMutation(fp(0, 2, 0), new StateMutation.WriteValue(decoded));
        stB.decodeInto(fp(0, 2, 0), decoder, bsB);

        assertEquals(bsB.pos(), bsA.pos());
        assertEquals(read(stA, fp(0, 2, 0)), read(stB, fp(0, 2, 0)));
    }

    @Test
    public void decodeIntoPrimitiveViaPointerMatchesApplyMutation() {
        var inner = serializer("I", named("x", intField()));
        var ptr = pointerField(inner);
        var ser = serializer("S", named("p", ptr));

        var stA = makeFlat(ser);
        var stB = makeFlat(ser);
        stA.applyMutation(fp(0), new StateMutation.SwitchPointer(inner));
        stB.applyMutation(fp(0), new StateMutation.SwitchPointer(inner));

        var decoder = intDecoder();
        var bsA = freshStream();
        var bsB = freshStream();
        var decoded = DecoderDispatch.decode(bsA, decoder);
        stA.applyMutation(fp(0, 0), new StateMutation.WriteValue(decoded));
        stB.decodeInto(fp(0, 0), decoder, bsB);

        assertEquals(read(stA, fp(0, 0)), read(stB, fp(0, 0)));
    }

    @Test
    public void decodeIntoOnRefLeafFallsBackToDecode() {
        var ser = serializer("S", named("s", stringField()));
        var stA = makeFlat(ser);
        var stB = makeFlat(ser);

        var decoder = stringDecoder();
        var bsA = streamFor(0xBEEFL);
        var bsB = streamFor(0xBEEFL);

        var decoded = DecoderDispatch.decode(bsA, decoder);
        stA.applyMutation(fp(0), new StateMutation.WriteValue(decoded));
        var capB = stB.decodeInto(fp(0), decoder, bsB);

        assertEquals(read(stB, fp(0)), read(stA, fp(0)),
            "decodeInto on Ref leaf matches WriteValue(decode) result");
        assertTrue(capB, "first Ref write returns capacity-change");
    }

    @Test
    public void decodeIntoOnSubStateLeafThrows() {
        var element = serializer("E", named("x", intField()));
        var ser = serializer("S", named("v", vectorFieldOf(serializerField(element))));
        var st = makeFlat(ser);

        // fp(0) targets the vector substate as a leaf — structural mutation path
        assertThrows(IllegalStateException.class,
            () -> st.decodeInto(fp(0), intDecoder(), freshStream()));
    }

    @Test
    public void decodeIntoCapacityChangeReturnsFalseOnSecondWrite() {
        var ser = serializer("S", named("a", intField()));
        var st = makeFlat(ser);

        var cap1 = st.decodeInto(fp(0), intDecoder(), freshStream());
        var cap2 = st.decodeInto(fp(0), intDecoder(), freshStream());

        assertTrue(cap1, "first write: null→value returns true");
        assertFalse(cap2, "second write: value→value returns false");
    }

    // ---------- 4.7: write parity ----------

    @Test
    public void writeOnPrimitiveMatchesApplyMutationWriteValue() {
        var ser = serializer("S", named("a", intField()));
        var stA = makeFlat(ser);
        var stB = makeFlat(ser);

        var capA = stA.applyMutation(fp(0), new StateMutation.WriteValue(Integer.valueOf(42)));
        var capB = stB.write(fp(0), Integer.valueOf(42));

        assertEquals(stB.rootDataForTest(), stA.rootDataForTest());
        assertEquals(capB, capA);
    }

    @Test
    public void writeOnRefMatchesApplyMutationWriteValue() {
        var ser = serializer("S", named("s", stringField()));
        var stA = makeFlat(ser);
        var stB = makeFlat(ser);

        var capA = stA.applyMutation(fp(0), new StateMutation.WriteValue("hello"));
        var capB = stB.write(fp(0), "hello");

        assertEquals(read(stB, fp(0)), read(stA, fp(0)));
        assertEquals(capB, capA);
    }

    @Test
    public void writeOnVectorSubStateMatchesResizeVector() {
        var element = serializer("E", named("x", intField()));
        var ser = serializer("S", named("v", vectorFieldOf(serializerField(element))));

        var stA = makeFlat(ser);
        var stB = makeFlat(ser);

        var capA = stA.applyMutation(fp(0), new StateMutation.ResizeVector(5));
        var capB = stB.write(fp(0), Integer.valueOf(5));

        assertEquals(capB, capA, "capacity-change matches");
        assertEquals(stB.rootDataForTest(), stA.rootDataForTest());
    }

    @Test
    public void writeOnPointerSubStateMatchesSwitchPointer() {
        var serA = serializer("A", named("a", intField()));
        var serB = serializer("B", named("b", intField()));
        var ptr = pointerField(serA, serB);
        var ser = serializer("S", named("p", ptr));

        var stA = makeFlat(ser);
        var stB = makeFlat(ser);

        stA.applyMutation(fp(0), new StateMutation.SwitchPointer(serA));
        stB.write(fp(0), serA);
        assertSame(stA.pointerSerializersForTest()[ptr.getPointerId()],
                   stB.pointerSerializersForTest()[ptr.getPointerId()]);

        // switch again — both sides agree on capacity-change
        stA.applyMutation(fp(0, 0), new StateMutation.WriteValue(42));
        stB.applyMutation(fp(0, 0), new StateMutation.WriteValue(42));
        var capA = stA.applyMutation(fp(0), new StateMutation.SwitchPointer(serB));
        var capB = stB.write(fp(0), serB);
        assertEquals(capB, capA, "second switch returns the same capacity-change signal");
    }

    // ---------- decodeInto / write after copy() preserves independence ----------

    @Test
    public void decodeIntoAfterCopyLeavesOriginalUnchanged() {
        var ser = serializer("S", named("a", intField()));
        var st = makeFlat(ser);
        st.applyMutation(fp(0), new StateMutation.WriteValue(1));

        var cp = (S2FlatEntityState) st.copy();
        cp.decodeInto(fp(0), intDecoder(), freshStream());

        assertEquals(read(st, fp(0)), 1, "original unchanged by cp.decodeInto");
    }

    @Test
    public void writeAfterCopyLeavesInlineStringOriginalUnchanged() {
        var ser = serializer("S", named("s", stringField()));
        var st = makeFlat(ser);
        st.write(fp(0), "original");

        var cp = (S2FlatEntityState) st.copy();
        cp.write(fp(0), "replaced");

        assertEquals(read(st, fp(0)), "original");
        assertEquals(read(cp, fp(0)), "replaced");
    }

    @Test
    public void writeAfterCopyIsIndependentForInnerPrimitiveViaPointer() {
        var inner = serializer("I", named("x", intField()));
        var ptr = pointerField(inner);
        var ser = serializer("S", named("p", ptr));
        var st = makeFlat(ser);
        st.applyMutation(fp(0), new StateMutation.SwitchPointer(inner));
        st.write(fp(0, 0), 1);

        var cp = (S2FlatEntityState) st.copy();
        cp.write(fp(0, 0), 99);

        assertEquals(read(cp, fp(0, 0)), 99);
        assertEquals(read(st, fp(0, 0)), 1);
    }
}
