package skadistats.clarity.state;

import org.testng.annotations.Test;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.Serializer;
import skadistats.clarity.state.s2.FieldLayoutBuilder;
import skadistats.clarity.state.s2.S2FlatEntityState;

import static org.testng.Assert.assertEquals;
import static skadistats.clarity.state.TestFields.floatField;
import static skadistats.clarity.state.TestFields.fp;
import static skadistats.clarity.state.TestFields.intField;
import static skadistats.clarity.state.TestFields.named;
import static skadistats.clarity.state.TestFields.pointerField;
import static skadistats.clarity.state.TestFields.rootField;
import static skadistats.clarity.state.TestFields.serializer;
import static skadistats.clarity.state.TestFields.stringField;
import static skadistats.clarity.state.TestFields.vectorFieldOf;

public class S2FlatEntityStateCopyTest {

    private static S2FlatEntityState makeFlat(Serializer root) {
        return makeFlat(root, 1024);
    }

    private static S2FlatEntityState makeFlat(Serializer root, int pointerCount) {
        var rf = rootField(root);
        var built = new FieldLayoutBuilder().buildSerializer(rf.getSerializer());
        return new S2FlatEntityState(rf, pointerCount, built.layout(), built.totalBytes());
    }

    private static boolean write(S2FlatEntityState s, S2FieldPath fp, Object v) {
        return s.applyMutation(fp, new StateMutation.WriteValue(v));
    }

    private static boolean resize(S2FlatEntityState s, S2FieldPath fp, int count) {
        return s.applyMutation(fp, new StateMutation.ResizeVector(count));
    }

    private static boolean switchPtr(S2FlatEntityState s, S2FieldPath fp, Serializer ser) {
        return s.applyMutation(fp, new StateMutation.SwitchPointer(ser));
    }

    private static Object read(S2FlatEntityState s, S2FieldPath fp) {
        return s.getValueForFieldPath(fp);
    }

    @Test
    public void primitiveWritesAfterCopyAreIndependent() {
        var ser = serializer("S", named("a", intField()), named("b", floatField()));
        var st = makeFlat(ser);
        write(st, fp(0), 7);
        write(st, fp(1), 1.5f);

        var cp = (S2FlatEntityState) st.copy();

        write(cp, fp(0), 42);
        write(st, fp(1), 9.9f);

        assertEquals(read(st, fp(0)), 7);
        assertEquals(read(st, fp(1)), 9.9f);
        assertEquals(read(cp, fp(0)), 42);
        assertEquals(read(cp, fp(1)), 1.5f);
    }

    @Test
    public void deepNestedWriteAfterCopyIsIndependent() {
        var element = serializer("E", named("x", intField()), named("y", intField()));
        var ser = serializer("S", named("v", vectorFieldOf(TestFields.serializerField(element))));
        var st = makeFlat(ser);

        resize(st, fp(0), 3);
        write(st, fp(0, 0, 0), 10);
        write(st, fp(0, 1, 0), 20);
        write(st, fp(0, 2, 0), 30);

        var cp = (S2FlatEntityState) st.copy();
        write(cp, fp(0, 1, 0), 99);

        assertEquals(read(st, fp(0, 0, 0)), 10);
        assertEquals(read(st, fp(0, 1, 0)), 20);
        assertEquals(read(st, fp(0, 2, 0)), 30);
        assertEquals(read(cp, fp(0, 0, 0)), 10);
        assertEquals(read(cp, fp(0, 1, 0)), 99);
        assertEquals(read(cp, fp(0, 2, 0)), 30);
    }

    @Test
    public void inlineStringWritesAfterCopyAreIndependent() {
        var ser = serializer("S", named("s", stringField()));
        var st = makeFlat(ser);
        write(st, fp(0), "original");

        var cp = (S2FlatEntityState) st.copy();
        write(cp, fp(0), "replaced");

        assertEquals(read(st, fp(0)), "original");
        assertEquals(read(cp, fp(0)), "replaced");
    }

    @Test
    public void switchPointerAfterCopyIsIndependent() {
        var serA = serializer("A", named("a", intField()));
        var serB = serializer("B", named("b", intField()));
        var ptr = pointerField(serA, serB);
        var ser = serializer("S", named("p", ptr));

        var st = makeFlat(ser);
        switchPtr(st, fp(0), serA);
        write(st, fp(0, 0), 42);

        var cp = (S2FlatEntityState) st.copy();
        switchPtr(cp, fp(0), serB);

        assertEquals(read(st, fp(0, 0)), 42);

        write(cp, fp(0, 0), 7);
        assertEquals(read(st, fp(0, 0)), 42, "original sub-entry intact after cp switched pointer");
        assertEquals(read(cp, fp(0, 0)), 7);
    }
}
