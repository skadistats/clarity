package skadistats.clarity.model.state;

import org.testng.annotations.Test;
import skadistats.clarity.io.s2.Serializer;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;

import static org.testng.Assert.assertEquals;
import static skadistats.clarity.model.state.TestFields.floatField;
import static skadistats.clarity.model.state.TestFields.fp;
import static skadistats.clarity.model.state.TestFields.intField;
import static skadistats.clarity.model.state.TestFields.named;
import static skadistats.clarity.model.state.TestFields.pointerField;
import static skadistats.clarity.model.state.TestFields.rootField;
import static skadistats.clarity.model.state.TestFields.serializer;
import static skadistats.clarity.model.state.TestFields.serializerField;
import static skadistats.clarity.model.state.TestFields.vectorFieldOf;

public class S2NestedArrayEntityStateCopyTest {

    private static S2NestedArrayEntityState make(Serializer root) {
        return new S2NestedArrayEntityState(rootField(root), 1024);
    }

    private static boolean write(S2NestedArrayEntityState s, S2FieldPath fp, Object v) {
        return s.applyMutation(fp, new StateMutation.WriteValue(v));
    }

    private static boolean resize(S2NestedArrayEntityState s, S2FieldPath fp, int count) {
        return s.applyMutation(fp, new StateMutation.ResizeVector(count));
    }

    private static boolean switchPtr(S2NestedArrayEntityState s, S2FieldPath fp, Serializer ser) {
        return s.applyMutation(fp, new StateMutation.SwitchPointer(ser));
    }

    private static Object read(S2NestedArrayEntityState s, S2FieldPath fp) {
        return s.getValueForFieldPath(fp);
    }

    @Test
    public void primitiveWritesAfterCopyAreIndependent() {
        var ser = serializer("S", named("a", intField()), named("b", floatField()));
        var st = make(ser);
        write(st, fp(0), 7);
        write(st, fp(1), 1.5f);

        var cp = (S2NestedArrayEntityState) st.copy();
        write(cp, fp(0), 42);
        write(st, fp(1), 9.9f);

        assertEquals(read(st, fp(0)), 7);
        assertEquals(read(st, fp(1)), 9.9f);
        assertEquals(read(cp, fp(0)), 42);
        assertEquals(read(cp, fp(1)), 1.5f);
    }

    @Test
    public void writeToSubEntryAfterCopyIsIndependent() {
        var element = serializer("E", named("x", intField()), named("y", intField()));
        var ser = serializer("S", named("v", vectorFieldOf(serializerField(element))));
        var st = make(ser);

        resize(st, fp(0), 3);
        write(st, fp(0, 0, 0), 10);
        write(st, fp(0, 1, 0), 20);
        write(st, fp(0, 2, 0), 30);

        var cp = (S2NestedArrayEntityState) st.copy();
        write(cp, fp(0, 1, 0), 99);

        assertEquals(read(st, fp(0, 0, 0)), 10);
        assertEquals(read(st, fp(0, 1, 0)), 20);
        assertEquals(read(st, fp(0, 2, 0)), 30);
        assertEquals(read(cp, fp(0, 0, 0)), 10);
        assertEquals(read(cp, fp(0, 1, 0)), 99);
        assertEquals(read(cp, fp(0, 2, 0)), 30);
    }

    @Test
    public void tracedWritesOnCopyMatchApplyingToFresh() {
        var ser = serializer("S",
            named("a", intField()),
            named("b", intField()),
            named("c", intField()),
            named("d", intField())
        );

        // Seed both states to the same "pre-packet" shape
        var initial = make(ser);
        write(initial, fp(0), 1);
        write(initial, fp(1), 2);

        // The "snapshot + mutate" pattern
        var snapshot = (S2NestedArrayEntityState) initial.copy();
        write(initial, fp(2), 30);
        write(initial, fp(3), 40);

        // Parallel path: fresh state + same seed + same mutations
        var expected = make(ser);
        write(expected, fp(0), 1);
        write(expected, fp(1), 2);
        write(expected, fp(2), 30);
        write(expected, fp(3), 40);

        for (int i = 0; i < 4; i++) {
            assertEquals(read(initial, fp(i)), read(expected, fp(i)),
                "initial[" + i + "] matches fresh-path");
        }

        // Snapshot retains pre-mutation state
        assertEquals(read(snapshot, fp(0)), 1);
        assertEquals(read(snapshot, fp(1)), 2);
        assertEquals(read(snapshot, fp(2)), null);
        assertEquals(read(snapshot, fp(3)), null);
    }

    @Test
    public void freedSlotReuseAfterCopyIsIndependent() {
        // Create a state with a vector, shrink it to free slots, copy, then
        // grow on both sides — both should reuse the freed slots without
        // cross-contamination.
        var element = serializer("E", named("x", intField()));
        var ser = serializer("S", named("v", vectorFieldOf(serializerField(element))));
        var st = make(ser);

        resize(st, fp(0), 3);
        write(st, fp(0, 0, 0), 100);
        write(st, fp(0, 1, 0), 200);
        write(st, fp(0, 2, 0), 300);
        // Shrink to 1 — frees slots for indices 1 and 2.
        resize(st, fp(0), 1);

        var cp = (S2NestedArrayEntityState) st.copy();

        // Grow st and cp back to 3 — both should reuse the freed slots,
        // each into its own entries list.
        resize(st, fp(0), 3);
        write(st, fp(0, 1, 0), 11);
        write(st, fp(0, 2, 0), 22);

        resize(cp, fp(0), 3);
        write(cp, fp(0, 1, 0), 999);
        write(cp, fp(0, 2, 0), 888);

        assertEquals(read(st, fp(0, 0, 0)), 100);
        assertEquals(read(st, fp(0, 1, 0)), 11);
        assertEquals(read(st, fp(0, 2, 0)), 22);

        assertEquals(read(cp, fp(0, 0, 0)), 100);
        assertEquals(read(cp, fp(0, 1, 0)), 999);
        assertEquals(read(cp, fp(0, 2, 0)), 888);
    }

    @Test
    public void switchPointerAfterCopyIsIndependent() {
        var serA = serializer("A", named("a", intField()));
        var serB = serializer("B", named("b", intField()));
        var ptr = pointerField(serA, serB);
        var ser = serializer("S", named("p", ptr));

        var st = make(ser);
        switchPtr(st, fp(0), serA);
        write(st, fp(0, 0), 42);

        var cp = (S2NestedArrayEntityState) st.copy();
        switchPtr(cp, fp(0), serB);

        assertEquals(read(st, fp(0, 0)), 42);

        write(cp, fp(0, 0), 7);
        assertEquals(read(st, fp(0, 0)), 42, "original sub-entry intact after cp switched pointer");
        assertEquals(read(cp, fp(0, 0)), 7);
    }
}
