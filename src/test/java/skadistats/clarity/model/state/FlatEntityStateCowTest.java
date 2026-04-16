package skadistats.clarity.model.state;

import org.testng.annotations.Test;
import skadistats.clarity.io.s2.Serializer;
import skadistats.clarity.model.FieldPath;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static skadistats.clarity.model.state.TestFields.floatField;
import static skadistats.clarity.model.state.TestFields.fp;
import static skadistats.clarity.model.state.TestFields.intField;
import static skadistats.clarity.model.state.TestFields.named;
import static skadistats.clarity.model.state.TestFields.pointerField;
import static skadistats.clarity.model.state.TestFields.rootField;
import static skadistats.clarity.model.state.TestFields.serializer;
import static skadistats.clarity.model.state.TestFields.stringField;
import static skadistats.clarity.model.state.TestFields.vectorFieldOf;

/**
 * CP-1 tests: verify owner-pointer COW on FlatEntityState shares by reference at
 * copy time and clones only the touched path on write.
 */
public class FlatEntityStateCowTest {

    private static FlatEntityState makeFlat(Serializer root) {
        return makeFlat(root, 1024);
    }

    private static FlatEntityState makeFlat(Serializer root, int pointerCount) {
        var rf = rootField(root);
        var built = new FieldLayoutBuilder().buildSerializer(rf.getSerializer());
        return new FlatEntityState(rf, pointerCount, built.layout(), built.totalBytes());
    }

    private static boolean write(FlatEntityState s, FieldPath fp, Object v) {
        return s.applyMutation(fp, new StateMutation.WriteValue(v));
    }

    private static boolean resize(FlatEntityState s, FieldPath fp, int count) {
        return s.applyMutation(fp, new StateMutation.ResizeVector(count));
    }

    private static boolean switchPtr(FlatEntityState s, FieldPath fp, Serializer ser) {
        return s.applyMutation(fp, new StateMutation.SwitchPointer(ser));
    }

    // ---------- 1.10: copy() shares containers by reference ----------

    @Test
    public void copyPerformsNoByteClone() {
        var ser = serializer("S", named("a", intField()), named("b", floatField()));
        var st = makeFlat(ser);
        write(st, fp(0), 7);
        write(st, fp(1), 1.5f);

        var rootDataBefore = st.rootDataForTest();
        var refsBefore = st.refsArrayForTest();
        var pointerSerializersBefore = st.pointerSerializersForTest();

        var cp = (FlatEntityState) st.copy();

        assertSame(cp.rootDataForTest(), rootDataBefore,
            "copy() must share rootEntry.data by reference");
        assertSame(cp.refsArrayForTest(), refsBefore,
            "copy() must share refs array by reference");
        assertSame(cp.pointerSerializersForTest(), pointerSerializersBefore,
            "copy() must share pointerSerializers by reference");
    }

    // ---------- 1.11: deep-nested write clones only the touched path ----------

    @Test
    public void deepNestedWriteClonesOnlyTouchedPath() {
        var element = serializer("E", named("x", intField()), named("y", intField()));
        var ser = serializer("S", named("v", vectorFieldOf(TestFields.serializerField(element))));
        var st = makeFlat(ser);

        resize(st, fp(0), 3);
        write(st, fp(0, 0, 0), 10);
        write(st, fp(0, 1, 0), 20);
        write(st, fp(0, 2, 0), 30);

        // capture pre-copy byte[] identities of every sub-Entry the test will probe
        var stRootBefore = st.rootDataForTest();
        var vectorSlot = findSingleEntrySlot(st);
        var stVectorDataBefore = st.subEntryDataForTest(vectorSlot);

        var cp = (FlatEntityState) st.copy();

        // capture cp's slot-1 byte[] (pre-write) via traversal; should match original
        assertSame(cp.rootDataForTest(), stRootBefore, "root shared pre-write");

        // mutate inside vector[1]; only vector sub-Entry and root should COW
        write(cp, fp(0, 1, 0), 99);

        assertNotSame(cp.rootDataForTest(), stRootBefore,
            "cp root cloned after write (owner path walks through root SubState)");
        assertSame(st.rootDataForTest(), stRootBefore,
            "original root byte[] unchanged");

        // The vector sub-Entry backing byte[] on cp also cloned
        assertNotSame(cp.subEntryDataForTest(vectorSlot), stVectorDataBefore,
            "cp vector sub-Entry cloned after inner write");
        assertSame(st.subEntryDataForTest(vectorSlot), stVectorDataBefore,
            "original vector sub-Entry byte[] unchanged");

        // Data correctness
        assertEquals(read(st, fp(0, 0, 0)), 10);
        assertEquals(read(st, fp(0, 1, 0)), 20);
        assertEquals(read(st, fp(0, 2, 0)), 30);
        assertEquals(read(cp, fp(0, 0, 0)), 10);
        assertEquals(read(cp, fp(0, 1, 0)), 99);
        assertEquals(read(cp, fp(0, 2, 0)), 30);
    }

    // ---------- 1.12 / CP-5: copy then inline-string write clones root only ----------

    @Test
    public void copyThenStringWriteClonesRootOnly() {
        var ser = serializer("S", named("s", stringField()));
        var st = makeFlat(ser);
        write(st, fp(0), "original");

        var stRefsBefore = st.refsArrayForTest();
        var stRootBefore = st.rootDataForTest();

        var cp = (FlatEntityState) st.copy();
        assertSame(cp.refsArrayForTest(), stRefsBefore, "copy shares refs by reference");

        write(cp, fp(0), "replaced");

        assertSame(cp.refsArrayForTest(), stRefsBefore,
            "inline-string write must NOT clone refs — strings live inline in the root byte[]");
        assertNotSame(cp.rootDataForTest(), stRootBefore,
            "cp root cloned (inline-string bytes and flag written)");
        assertSame(st.rootDataForTest(), stRootBefore,
            "original root byte[] unchanged");
        assertEquals(read(st, fp(0)), "original");
        assertEquals(read(cp, fp(0)), "replaced");
    }

    // ---------- 1.13: SwitchPointer clones pointerSerializers in copy only ----------

    @Test
    public void copyThenSwitchPointerClonesPointerSerializersInCopyOnly() {
        var serA = serializer("A", named("a", intField()));
        var serB = serializer("B", named("b", intField()));
        var ptr = pointerField(serA, serB);
        var ser = serializer("S", named("p", ptr));

        var st = makeFlat(ser);
        switchPtr(st, fp(0), serA);
        write(st, fp(0, 0), 42);

        var stPsBefore = st.pointerSerializersForTest();
        var stRootBefore = st.rootDataForTest();

        var cp = (FlatEntityState) st.copy();
        assertSame(cp.pointerSerializersForTest(), stPsBefore,
            "copy shares pointerSerializers by reference");

        switchPtr(cp, fp(0), serB);

        assertNotSame(cp.pointerSerializersForTest(), stPsBefore,
            "cp pointerSerializers cloned after SwitchPointer");
        assertSame(st.pointerSerializersForTest(), stPsBefore,
            "original pointerSerializers unchanged");
        assertNotSame(cp.rootDataForTest(), stRootBefore,
            "cp root cloned (sub-Entry slot reassigned)");

        // Correctness
        assertEquals(read(st, fp(0, 0)), 42);
        write(cp, fp(0, 0), 7);
        assertEquals(read(st, fp(0, 0)), 42, "original sub-entry intact after cp switched pointer");
        assertEquals(read(cp, fp(0, 0)), 7);
    }

    // ---------- helpers ----------

    private static Object read(FlatEntityState s, FieldPath fp) {
        return s.getValueForFieldPath(fp);
    }

    /** Find the one occupied refs slot (for tests with a single sub-Entry). */
    private static int findSingleEntrySlot(FlatEntityState st) {
        var refs = st.refsArrayForTest();
        int found = -1;
        for (var i = 0; i < refs.length; i++) {
            if (refs[i] != null) {
                if (found != -1) throw new AssertionError("expected exactly one occupied slot");
                found = i;
            }
        }
        assertTrue(found >= 0, "no occupied refs slot found");
        return found;
    }
}
