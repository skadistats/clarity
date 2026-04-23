package skadistats.clarity.state;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.Serializer;
import skadistats.clarity.state.s2.S2EntityState;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static skadistats.clarity.state.TestFields.fp;
import static skadistats.clarity.state.TestFields.floatField;
import static skadistats.clarity.state.TestFields.intField;
import static skadistats.clarity.state.TestFields.longField;
import static skadistats.clarity.state.TestFields.named;
import static skadistats.clarity.state.TestFields.rootField;
import static skadistats.clarity.state.TestFields.serializer;
import static skadistats.clarity.state.TestFields.stringField;

/**
 * Coverage for the primitive-state-accessors API: getInt/Long/Float/Object,
 * captureChanged, applyFrom, applyAll. Parameterized over every S2 entity-state
 * impl so the contract is verified uniformly.
 */
public class PrimitiveAccessorsTest {

    @DataProvider(name = "impls")
    public Object[][] impls() {
        return new Object[][] {
            {TestStateFactory.NESTED_ARRAY},
            {TestStateFactory.TREE_MAP},
            {TestStateFactory.FLAT},
        };
    }

    private EntityState makeMixed(String impl) {
        var ser = serializer("S",
                named("i", intField()),
                named("l", longField()),
                named("f", floatField()),
                named("s", stringField()));
        var root = rootField(ser);
        return TestStateFactory.of(impl).create(root, 1024);
    }

    private static void writeRaw(EntityState s, FieldPath fp, Object v) {
        ((S2EntityState) s).applyMutation((S2FieldPath) fp, new StateMutation.WriteValue(v));
    }

    // ---------- primitive getters ----------

    @Test(dataProvider = "impls")
    public void getIntReturnsWrittenValue(String impl) {
        var st = makeMixed(impl);
        writeRaw(st, fp(0), 42);
        assertEquals(EntityState.getInt(st, fp(0)), 42);
    }

    @Test(dataProvider = "impls")
    public void getLongReturnsWrittenValue(String impl) {
        var st = makeMixed(impl);
        writeRaw(st, fp(1), 9_999_999_999L);
        assertEquals(EntityState.getLong(st, fp(1)), 9_999_999_999L);
    }

    @Test(dataProvider = "impls")
    public void getFloatReturnsWrittenValue(String impl) {
        var st = makeMixed(impl);
        writeRaw(st, fp(2), 3.14f);
        assertEquals(EntityState.getFloat(st, fp(2)), 3.14f);
    }

    @Test(dataProvider = "impls")
    public void getObjectReturnsWrittenValue(String impl) {
        var st = makeMixed(impl);
        writeRaw(st, fp(3), "hello");
        assertEquals(EntityState.getObject(st, fp(3)), "hello");
    }

    @Test(dataProvider = "impls")
    public void getIntOnUnsetReturnsZero(String impl) {
        var st = makeMixed(impl);
        assertEquals(EntityState.getInt(st, fp(0)), 0);
    }

    @Test(dataProvider = "impls")
    public void getIntOnFloatFieldReturnsZero(String impl) {
        var st = makeMixed(impl);
        writeRaw(st, fp(2), 3.14f);
        assertEquals(EntityState.getInt(st, fp(2)), 0);
    }

    // ---------- captureChanged ----------

    @Test(dataProvider = "impls")
    public void captureChangedRoundTripsPrimitives(String impl) {
        var st = makeMixed(impl);
        writeRaw(st, fp(0), 42);
        writeRaw(st, fp(1), 7_000_000_000L);
        writeRaw(st, fp(2), 1.5f);
        var fps = new S2FieldPath[]{fp(0), fp(1), fp(2)};
        var delta = EntityState.captureChanged(st, fps, 3);
        assertEquals(delta.getInt(fp(0)), 42);
        assertEquals(delta.getLong(fp(1)), 7_000_000_000L);
        assertEquals(delta.getFloat(fp(2)), 1.5f);
    }

    @Test(dataProvider = "impls")
    public void captureChangedSurvivesSourceMutation(String impl) {
        var st = makeMixed(impl);
        writeRaw(st, fp(0), 100);
        var delta = EntityState.captureChanged(st, new S2FieldPath[]{fp(0)}, 1);
        writeRaw(st, fp(0), 200);
        assertEquals(delta.getInt(fp(0)), 100);
    }

    @Test(dataProvider = "impls")
    public void deltaUnknownFieldReturnsDefault(String impl) {
        var st = makeMixed(impl);
        writeRaw(st, fp(0), 42);
        var delta = EntityState.captureChanged(st, new S2FieldPath[]{fp(0)}, 1);
        assertEquals(delta.getInt(fp(2)), 0);
        assertNull(delta.getObject(fp(3)));
    }

    @Test(dataProvider = "impls")
    public void deltaWrongTypeReturnsDefault(String impl) {
        var st = makeMixed(impl);
        writeRaw(st, fp(2), 1.5f);
        var delta = EntityState.captureChanged(st, new S2FieldPath[]{fp(2)}, 1);
        assertEquals(delta.getInt(fp(2)), 0);
    }

    // ---------- applyFrom / applyAll ----------

    @Test(dataProvider = "impls")
    public void applyFromMergesSingleField(String impl) {
        var src = makeMixed(impl);
        var tgt = makeMixed(impl);
        writeRaw(src, fp(0), 99);
        var delta = EntityState.captureChanged(src, new S2FieldPath[]{fp(0)}, 1);
        EntityState.applyFrom(tgt, delta, fp(0));
        assertEquals(EntityState.getInt(tgt, fp(0)), 99);
    }

    @Test(dataProvider = "impls")
    public void applyAllMergesEveryCoveredField(String impl) {
        var src = makeMixed(impl);
        var tgt = makeMixed(impl);
        writeRaw(src, fp(0), 11);
        writeRaw(src, fp(2), 2.5f);
        var delta = EntityState.captureChanged(src, new S2FieldPath[]{fp(0), fp(2)}, 2);
        EntityState.applyAll(tgt, delta);
        assertEquals(EntityState.getInt(tgt, fp(0)), 11);
        assertEquals(EntityState.getFloat(tgt, fp(2)), 2.5f);
    }

    @Test(dataProvider = "impls")
    public void applyAllDoesNotTouchUncoveredFields(String impl) {
        var src = makeMixed(impl);
        var tgt = makeMixed(impl);
        writeRaw(tgt, fp(1), 555L);   // pre-existing in target
        writeRaw(src, fp(0), 11);
        var delta = EntityState.captureChanged(src, new S2FieldPath[]{fp(0)}, 1);
        EntityState.applyAll(tgt, delta);
        assertEquals(EntityState.getInt(tgt, fp(0)), 11);
        assertEquals(EntityState.getLong(tgt, fp(1)), 555L);
    }

    @Test(dataProvider = "impls")
    public void applyFromObjectMergesString(String impl) {
        var src = makeMixed(impl);
        var tgt = makeMixed(impl);
        writeRaw(src, fp(3), "hello");
        var delta = EntityState.captureChanged(src, new S2FieldPath[]{fp(3)}, 1);
        EntityState.applyFrom(tgt, delta, fp(3));
        assertEquals(EntityState.getObject(tgt, fp(3)), "hello");
    }
}
