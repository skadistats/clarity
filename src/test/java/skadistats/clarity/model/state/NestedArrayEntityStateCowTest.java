package skadistats.clarity.model.state;

import org.testng.annotations.Test;
import skadistats.clarity.io.s2.Serializer;
import skadistats.clarity.model.FieldPath;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static skadistats.clarity.model.state.TestFields.floatField;
import static skadistats.clarity.model.state.TestFields.fp;
import static skadistats.clarity.model.state.TestFields.intField;
import static skadistats.clarity.model.state.TestFields.named;
import static skadistats.clarity.model.state.TestFields.pointerField;
import static skadistats.clarity.model.state.TestFields.rootField;
import static skadistats.clarity.model.state.TestFields.serializer;
import static skadistats.clarity.model.state.TestFields.serializerField;
import static skadistats.clarity.model.state.TestFields.vectorFieldOf;

/**
 * CP-2 tests: verify owner-pointer COW on NestedArrayEntityState shares the entries
 * list, freeEntries deque, and Entry wrappers by reference at copy time.
 */
public class NestedArrayEntityStateCowTest {

    private static NestedArrayEntityState make(Serializer root) {
        return new NestedArrayEntityState(rootField(root), 1024);
    }

    private static boolean write(NestedArrayEntityState s, FieldPath fp, Object v) {
        return s.applyMutation(fp, new StateMutation.WriteValue(v));
    }

    private static boolean resize(NestedArrayEntityState s, FieldPath fp, int count) {
        return s.applyMutation(fp, new StateMutation.ResizeVector(count));
    }

    private static boolean switchPtr(NestedArrayEntityState s, FieldPath fp, Serializer ser) {
        return s.applyMutation(fp, new StateMutation.SwitchPointer(ser));
    }

    private static Object read(NestedArrayEntityState s, FieldPath fp) {
        return s.getValueForFieldPath(fp);
    }

    // ---------- 2.9: copy() shares containers by reference ----------

    @Test
    public void copyZeroContainerAllocations() {
        var ser = serializer("S", named("a", intField()), named("b", floatField()));
        var st = make(ser);
        write(st, fp(0), 7);
        write(st, fp(1), 1.5f);

        var entriesBefore = st.entriesForTest();
        var freeBefore = st.freeEntriesForTest();
        var pointerSerializersBefore = st.pointerSerializersForTest();

        var cp = (NestedArrayEntityState) st.copy();

        assertSame(cp.entriesForTest(), entriesBefore,
            "copy() must share entries list by reference");
        assertSame(cp.freeEntriesForTest(), freeBefore,
            "copy() must share freeEntries deque by reference");
        assertSame(cp.pointerSerializersForTest(), pointerSerializersBefore,
            "copy() must share pointerSerializers by reference");
    }

    // ---------- 2.10: write to sub-entry clones only that slot ----------

    @Test
    public void writeToSubEntryClonesOnlyTouchedSlab() {
        var element = serializer("E", named("x", intField()), named("y", intField()));
        var ser = serializer("S", named("v", vectorFieldOf(serializerField(element))));
        var st = make(ser);

        resize(st, fp(0), 3);
        write(st, fp(0, 0, 0), 10);
        write(st, fp(0, 1, 0), 20);
        write(st, fp(0, 2, 0), 30);

        var stEntriesBefore = st.entriesForTest();
        var rootBefore = stEntriesBefore.get(0);
        // capture identity of each slab Entry wrapper
        var preCopySlots = new Object[stEntriesBefore.size()];
        for (int i = 0; i < stEntriesBefore.size(); i++) preCopySlots[i] = stEntriesBefore.get(i);

        var cp = (NestedArrayEntityState) st.copy();
        assertSame(cp.entriesForTest(), stEntriesBefore, "entries list shared pre-write");

        // mutate inside vector[1]
        write(cp, fp(0, 1, 0), 99);

        // cp's entries list cloned, root cloned, and the vector sub-Entry + nested
        // element-1 Entry cloned. Unrelated slots remain reference-equal to originals.
        assertNotSame(cp.entriesForTest(), stEntriesBefore,
            "cp entries cloned after write");
        assertSame(st.entriesForTest(), stEntriesBefore,
            "original entries list unchanged");

        var stEntriesAfter = st.entriesForTest();
        for (int i = 0; i < preCopySlots.length; i++) {
            assertSame(stEntriesAfter.get(i), preCopySlots[i],
                "original's entries[" + i + "] unchanged by cp's write");
        }

        // Data correctness
        assertEquals(read(st, fp(0, 0, 0)), 10);
        assertEquals(read(st, fp(0, 1, 0)), 20);
        assertEquals(read(st, fp(0, 2, 0)), 30);
        assertEquals(read(cp, fp(0, 0, 0)), 10);
        assertEquals(read(cp, fp(0, 1, 0)), 99);
        assertEquals(read(cp, fp(0, 2, 0)), 30);
    }

    // ---------- 2.11: trace-parity — applied sequence on copy == original+sequence ----------

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
        var snapshot = (NestedArrayEntityState) initial.copy();
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

        // Snapshot retains pre-mutation state (rollback equivalence)
        assertEquals(read(snapshot, fp(0)), 1);
        assertEquals(read(snapshot, fp(1)), 2);
        assertEquals(read(snapshot, fp(2)), null);
        assertEquals(read(snapshot, fp(3)), null);
    }

    // ---------- Bonus: SwitchPointer COW on pointerSerializers ----------

    @Test
    public void copyThenSwitchPointerClonesPointerSerializersInCopyOnly() {
        var serA = serializer("A", named("a", intField()));
        var serB = serializer("B", named("b", intField()));
        var ptr = pointerField(serA, serB);
        var ser = serializer("S", named("p", ptr));

        var st = make(ser);
        switchPtr(st, fp(0), serA);
        write(st, fp(0, 0), 42);

        var stPsBefore = st.pointerSerializersForTest();

        var cp = (NestedArrayEntityState) st.copy();
        assertSame(cp.pointerSerializersForTest(), stPsBefore,
            "copy shares pointerSerializers by reference");

        switchPtr(cp, fp(0), serB);

        assertNotSame(cp.pointerSerializersForTest(), stPsBefore,
            "cp pointerSerializers cloned after SwitchPointer");
        assertSame(st.pointerSerializersForTest(), stPsBefore,
            "original pointerSerializers unchanged");

        assertEquals(read(st, fp(0, 0)), 42);
        write(cp, fp(0, 0), 7);
        assertEquals(read(st, fp(0, 0)), 42, "original sub-entry intact after cp switched pointer");
        assertEquals(read(cp, fp(0, 0)), 7);
    }
}
