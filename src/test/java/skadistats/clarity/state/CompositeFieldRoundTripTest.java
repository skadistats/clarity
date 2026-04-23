package skadistats.clarity.state;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.Serializer;
import skadistats.clarity.model.s2.field.PolymorphicPointerField;
import skadistats.clarity.model.s2.field.SerializerField;
import skadistats.clarity.state.s2.S2EntityState;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static skadistats.clarity.state.TestFields.fp;
import static skadistats.clarity.state.TestFields.intField;
import static skadistats.clarity.state.TestFields.named;
import static skadistats.clarity.state.TestFields.pointerField;
import static skadistats.clarity.state.TestFields.polymorphicPointerField;
import static skadistats.clarity.state.TestFields.rootField;
import static skadistats.clarity.state.TestFields.serializer;
import static skadistats.clarity.state.TestFields.vectorFieldOf;

/**
 * Round-trip contract for hidden composite field terminals (VectorField,
 * FixedPointerField, PolymorphicPointerField): {@code write(fp, x)} and
 * {@code get(fp) → x} must agree, so that
 * {@link EntityState#captureChanged} / {@link EntityState#applyFrom}
 * transport structural updates (vector resize, pointer set/clear/switch)
 * between independent state instances.
 *
 * Covers {@code NESTED_ARRAY} and {@code FLAT}. {@code TREE_MAP} is
 * omitted: it stores nothing at composite terminals and has no place to
 * record vector length — a separate gap to address alongside primitive
 * state accessors.
 */
public class CompositeFieldRoundTripTest {

    @DataProvider(name = "impls")
    public Object[][] impls() {
        return new Object[][]{
            {TestStateFactory.NESTED_ARRAY},
            {TestStateFactory.FLAT},
        };
    }

    private static EntityState make(String impl, SerializerField root) {
        return TestStateFactory.of(impl).create(root, 1024);
    }

    private static boolean write(EntityState s, FieldPath fp, Object v) {
        return ((S2EntityState) s).applyMutation((S2FieldPath) fp, new StateMutation.WriteValue(v));
    }

    private static boolean resize(EntityState s, FieldPath fp, int count) {
        return ((S2EntityState) s).applyMutation((S2FieldPath) fp, new StateMutation.ResizeVector(count));
    }

    private static boolean switchFixedPtr(EntityState s, FieldPath fp, Serializer ser) {
        return ((S2EntityState) s).applyMutation((S2FieldPath) fp, new StateMutation.SwitchFixedPointer(ser));
    }

    private static boolean switchPolyPtr(EntityState s, FieldPath fp, PolymorphicPointerField ppf, Serializer ser) {
        return ((S2EntityState) s).applyMutation((S2FieldPath) fp, new StateMutation.SwitchPolymorphicPointer(ppf.getPointerId(), ser));
    }

    private static Object read(EntityState s, FieldPath fp) {
        return EntityState.getValueForFieldPath(s, fp);
    }

    // ---------- VectorField: get returns current length ----------

    @Test(dataProvider = "impls")
    public void vectorFieldGetOnFreshReturnsZero(String impl) {
        var root = rootField(serializer("S", named("v", vectorFieldOf(intField()))));
        var st = make(impl, root);
        assertEquals(read(st, fp(0)), 0);
    }

    @Test(dataProvider = "impls")
    public void vectorFieldGetAfterResizeReturnsLength(String impl) {
        var root = rootField(serializer("S", named("v", vectorFieldOf(intField()))));
        var st = make(impl, root);
        resize(st, fp(0), 7);
        assertEquals(read(st, fp(0)), 7);
    }

    @Test(dataProvider = "impls")
    public void vectorFieldGetAfterShrinkReturnsNewLength(String impl) {
        var root = rootField(serializer("S", named("v", vectorFieldOf(intField()))));
        var st = make(impl, root);
        resize(st, fp(0), 5);
        resize(st, fp(0), 2);
        assertEquals(read(st, fp(0)), 2);
    }

    @Test(dataProvider = "impls")
    public void vectorFieldGetAfterResizeToZeroReturnsZero(String impl) {
        var root = rootField(serializer("S", named("v", vectorFieldOf(intField()))));
        var st = make(impl, root);
        resize(st, fp(0), 3);
        resize(st, fp(0), 0);
        assertEquals(read(st, fp(0)), 0);
    }

    // ---------- FixedPointerField: get returns Serializer|null ----------

    @Test(dataProvider = "impls")
    public void fixedPointerGetOnFreshReturnsNull(String impl) {
        var inner = serializer("Inner", named("x", intField()));
        var root = rootField(serializer("S", named("p", pointerField(inner))));
        var st = make(impl, root);
        assertNull(read(st, fp(0)));
    }

    @Test(dataProvider = "impls")
    public void fixedPointerGetAfterSetReturnsFieldSerializer(String impl) {
        var inner = serializer("Inner", named("x", intField()));
        var root = rootField(serializer("S", named("p", pointerField(inner))));
        var st = make(impl, root);
        switchFixedPtr(st, fp(0), inner);
        assertEquals(read(st, fp(0)), inner);
    }

    @Test(dataProvider = "impls")
    public void fixedPointerGetAfterClearReturnsNull(String impl) {
        var inner = serializer("Inner", named("x", intField()));
        var root = rootField(serializer("S", named("p", pointerField(inner))));
        var st = make(impl, root);
        switchFixedPtr(st, fp(0), inner);
        switchFixedPtr(st, fp(0), null);
        assertNull(read(st, fp(0)));
    }

    // ---------- PolymorphicPointerField: get returns active Serializer|null ----------

    @Test(dataProvider = "impls")
    public void polymorphicPointerGetOnFreshReturnsNull(String impl) {
        var a = serializer("A", named("x", intField()));
        var b = serializer("B", named("y", intField()));
        var ppf = polymorphicPointerField(a, b);
        var root = rootField(serializer("S", named("p", ppf)));
        var st = make(impl, root);
        assertNull(read(st, fp(0)));
    }

    @Test(dataProvider = "impls")
    public void polymorphicPointerGetAfterSetReturnsActiveSerializer(String impl) {
        var a = serializer("A", named("x", intField()));
        var b = serializer("B", named("y", intField()));
        var ppf = polymorphicPointerField(a, b);
        var root = rootField(serializer("S", named("p", ppf)));
        var st = make(impl, root);
        switchPolyPtr(st, fp(0), ppf, a);
        assertEquals(read(st, fp(0)), a);
    }

    @Test(dataProvider = "impls")
    public void polymorphicPointerGetAfterSwitchReturnsNewSerializer(String impl) {
        var a = serializer("A", named("x", intField()));
        var b = serializer("B", named("y", intField()));
        var ppf = polymorphicPointerField(a, b);
        var root = rootField(serializer("S", named("p", ppf)));
        var st = make(impl, root);
        switchPolyPtr(st, fp(0), ppf, a);
        switchPolyPtr(st, fp(0), ppf, b);
        assertEquals(read(st, fp(0)), b);
    }

    @Test(dataProvider = "impls")
    public void polymorphicPointerGetAfterClearReturnsNull(String impl) {
        var a = serializer("A", named("x", intField()));
        var ppf = polymorphicPointerField(a);
        var root = rootField(serializer("S", named("p", ppf)));
        var st = make(impl, root);
        switchPolyPtr(st, fp(0), ppf, a);
        switchPolyPtr(st, fp(0), ppf, null);
        assertNull(read(st, fp(0)));
    }

    // ---------- captureChanged + applyFrom round-trip ----------

    @Test(dataProvider = "impls")
    public void captureAndApplyReplaysVectorResize(String impl) {
        var root = rootField(serializer("S", named("v", vectorFieldOf(intField()))));
        var src = make(impl, root);
        var tgt = make(impl, root);
        resize(src, fp(0), 4);
        var delta = EntityState.captureChanged(src, new S2FieldPath[]{fp(0)}, 1);
        EntityState.applyFrom(tgt, delta, fp(0));
        assertEquals(read(tgt, fp(0)), 4);
    }

    @Test(dataProvider = "impls")
    public void captureAndApplyReplaysVectorShrink(String impl) {
        var root = rootField(serializer("S", named("v", vectorFieldOf(intField()))));
        var src = make(impl, root);
        var tgt = make(impl, root);
        // target pre-populated at size 5, source shrinks to 2 — apply must resize target
        resize(tgt, fp(0), 5);
        write(tgt, fp(0, 0), 10);
        write(tgt, fp(0, 1), 20);
        resize(src, fp(0), 2);
        var delta = EntityState.captureChanged(src, new S2FieldPath[]{fp(0)}, 1);
        EntityState.applyFrom(tgt, delta, fp(0));
        assertEquals(read(tgt, fp(0)), 2);
    }

    @Test(dataProvider = "impls")
    public void captureAndApplyReplaysFixedPointerSet(String impl) {
        var inner = serializer("Inner", named("x", intField()));
        var root = rootField(serializer("S", named("p", pointerField(inner))));
        var src = make(impl, root);
        var tgt = make(impl, root);
        switchFixedPtr(src, fp(0), inner);
        var delta = EntityState.captureChanged(src, new S2FieldPath[]{fp(0)}, 1);
        EntityState.applyFrom(tgt, delta, fp(0));
        assertEquals(read(tgt, fp(0)), inner);
    }

    @Test(dataProvider = "impls")
    public void captureAndApplyReplaysFixedPointerClear(String impl) {
        var inner = serializer("Inner", named("x", intField()));
        var root = rootField(serializer("S", named("p", pointerField(inner))));
        var src = make(impl, root);
        var tgt = make(impl, root);
        switchFixedPtr(tgt, fp(0), inner); // target starts set
        // src is fresh (pointer unset) — apply must clear target
        var delta = EntityState.captureChanged(src, new S2FieldPath[]{fp(0)}, 1);
        EntityState.applyFrom(tgt, delta, fp(0));
        assertNull(read(tgt, fp(0)));
    }

    @Test(dataProvider = "impls")
    public void captureAndApplyReplaysPolymorphicPointerSet(String impl) {
        var a = serializer("A", named("x", intField()));
        var b = serializer("B", named("y", intField()));
        var ppf = polymorphicPointerField(a, b);
        var root = rootField(serializer("S", named("p", ppf)));
        var src = make(impl, root);
        var tgt = make(impl, root);
        switchPolyPtr(src, fp(0), ppf, a);
        var delta = EntityState.captureChanged(src, new S2FieldPath[]{fp(0)}, 1);
        EntityState.applyFrom(tgt, delta, fp(0));
        assertEquals(read(tgt, fp(0)), a);
    }

    @Test(dataProvider = "impls")
    public void captureAndApplyReplaysPolymorphicPointerSwitch(String impl) {
        var a = serializer("A", named("x", intField()));
        var b = serializer("B", named("y", intField()));
        var ppf = polymorphicPointerField(a, b);
        var root = rootField(serializer("S", named("p", ppf)));
        var src = make(impl, root);
        var tgt = make(impl, root);
        switchPolyPtr(tgt, fp(0), ppf, a);   // target on A
        switchPolyPtr(src, fp(0), ppf, b);   // source on B
        var delta = EntityState.captureChanged(src, new S2FieldPath[]{fp(0)}, 1);
        EntityState.applyFrom(tgt, delta, fp(0));
        assertEquals(read(tgt, fp(0)), b);
    }

    @Test(dataProvider = "impls")
    public void captureAndApplyReplaysPolymorphicPointerClear(String impl) {
        var a = serializer("A", named("x", intField()));
        var ppf = polymorphicPointerField(a);
        var root = rootField(serializer("S", named("p", ppf)));
        var src = make(impl, root);
        var tgt = make(impl, root);
        switchPolyPtr(tgt, fp(0), ppf, a);   // target set
        // src fresh — apply must clear target
        var delta = EntityState.captureChanged(src, new S2FieldPath[]{fp(0)}, 1);
        EntityState.applyFrom(tgt, delta, fp(0));
        assertNull(read(tgt, fp(0)));
    }

    @Test(dataProvider = "impls")
    public void captureAndApplyAllReplaysMixedCompositeUpdates(String impl) {
        var inner = serializer("Inner", named("x", intField()));
        var a = serializer("A", named("x", intField()));
        var b = serializer("B", named("y", intField()));
        var ppf = polymorphicPointerField(a, b);
        var root = rootField(serializer("Root",
            named("vec", vectorFieldOf(intField())),
            named("fix", pointerField(inner)),
            named("poly", ppf)));
        var src = make(impl, root);
        var tgt = make(impl, root);

        resize(src, fp(0), 3);
        switchFixedPtr(src, fp(1), inner);
        switchPolyPtr(src, fp(2), ppf, b);

        var delta = EntityState.captureChanged(src,
            new S2FieldPath[]{fp(0), fp(1), fp(2)}, 3);
        EntityState.applyAll(tgt, delta);

        assertEquals(read(tgt, fp(0)), 3);
        assertEquals(read(tgt, fp(1)), inner);
        assertEquals(read(tgt, fp(2)), b);
    }
}
