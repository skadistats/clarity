package skadistats.clarity.state;

import com.sun.management.ThreadMXBean;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.state.s2.S2EntityState;

import java.lang.management.ManagementFactory;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static skadistats.clarity.state.TestFields.floatField;
import static skadistats.clarity.state.TestFields.fp;
import static skadistats.clarity.state.TestFields.intField;
import static skadistats.clarity.state.TestFields.longField;
import static skadistats.clarity.state.TestFields.named;
import static skadistats.clarity.state.TestFields.rootField;
import static skadistats.clarity.state.TestFields.serializer;

/**
 * Asserts that the primitive getters do not allocate per call. Values are
 * chosen outside the {@code Integer}/{@code Long} caches, so a boxing read
 * would show up as allocation.
 */
public class PrimitiveAccessorsAllocationTest {

    private static final int CALLS = 100_000;

    private static final ThreadMXBean THREADS = (ThreadMXBean) ManagementFactory.getThreadMXBean();

    @DataProvider(name = "impls")
    public Object[][] impls() {
        return new Object[][] {
            {TestStateFactory.NESTED_ARRAY},
            {TestStateFactory.TREE_MAP},
            {TestStateFactory.FLAT},
        };
    }

    private EntityState makeState(String impl) {
        var ser = serializer("S",
                named("i", intField()),
                named("l", longField()),
                named("f", floatField()));
        var st = TestStateFactory.of(impl).create(rootField(ser), 1024);
        writeRaw(st, fp(0), 1_000_000);
        writeRaw(st, fp(1), 9_999_999_999L);
        writeRaw(st, fp(2), 3.14f);
        return st;
    }

    private static void writeRaw(EntityState s, FieldPath fp, Object v) {
        ((S2EntityState) s).applyMutation((S2FieldPath) fp, new StateMutation.WriteValue(v));
    }

    private static long readAll(EntityState st, FieldPath i, FieldPath l, FieldPath f) {
        long sum = 0;
        for (int n = 0; n < CALLS; n++) {
            sum += EntityState.getInt(st, i);
            sum += EntityState.getLong(st, l);
            sum += Float.floatToRawIntBits(EntityState.getFloat(st, f));
        }
        return sum;
    }

    @Test(dataProvider = "impls")
    public void primitiveGettersDoNotAllocate(String impl) {
        assertTrue(THREADS.isThreadAllocatedMemorySupported(), "allocation tracking unsupported on this JVM");
        var st = makeState(impl);
        FieldPath i = fp(0), l = fp(1), f = fp(2);
        long expected = (long) CALLS * (1_000_000L + 9_999_999_999L + Float.floatToRawIntBits(3.14f));

        assertEquals(readAll(st, i, l, f), expected);

        var tid = Thread.currentThread().threadId();
        long before = THREADS.getThreadAllocatedBytes(tid);
        long sum = readAll(st, i, l, f);
        long allocated = THREADS.getThreadAllocatedBytes(tid) - before;

        assertEquals(sum, expected);
        assertTrue(allocated < CALLS, impl + ": " + allocated + " bytes allocated over " + (3L * CALLS) + " primitive reads");
    }

}
