package skadistats.clarity.state;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.Serializer;
import skadistats.clarity.state.s2.S2EntityState;
import skadistats.clarity.state.s2.S2FlatEntityState;
import skadistats.clarity.state.s2.S2NestedArrayEntityState;

import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static skadistats.clarity.state.TestFields.*;

public class EntityStateTest {

    @DataProvider(name = "impls")
    public Object[][] impls() {
        return new Object[][] {
            {TestStateFactory.NESTED_ARRAY},
            {TestStateFactory.TREE_MAP},
            {TestStateFactory.FLAT},
        };
    }

    // ---------- helpers ----------

    private EntityState makeState(String impl, Serializer rootSerializer) {
        return makeState(impl, rootSerializer, 1024);
    }

    private EntityState makeState(String impl, Serializer rootSerializer, int pointerCount) {
        var root = rootField(rootSerializer);
        return TestStateFactory.of(impl).create(root, pointerCount);
    }

    private static boolean write(EntityState s, FieldPath fp, Object v) {
        return ((S2EntityState) s).applyMutation((S2FieldPath) fp, new StateMutation.WriteValue(v));
    }

    private static boolean resize(EntityState s, FieldPath fp, int count) {
        return ((S2EntityState) s).applyMutation((S2FieldPath) fp, new StateMutation.ResizeVector(count));
    }

    private static boolean switchPtr(EntityState s, FieldPath fp, Serializer ser) {
        return ((S2EntityState) s).applyMutation((S2FieldPath) fp, new StateMutation.SwitchPointer(ser));
    }

    private static Object read(EntityState s, FieldPath fp) {
        return EntityState.getValueForFieldPath(s, fp);
    }

    private static Set<FieldPath> paths(EntityState s) {
        var set = new HashSet<FieldPath>();
        var it = s.fieldPathIterator();
        while (it.hasNext()) set.add(it.next());
        return set;
    }

    // ---------- 7.3.1 Write + read primitives ----------

    @Test(dataProvider = "impls")
    public void writeReadInt(String impl) {
        var ser = serializer("S", named("a", intField()));
        var st = makeState(impl, ser);
        write(st, fp(0), 42);
        assertEquals(read(st, fp(0)), 42);
    }

    @Test(dataProvider = "impls")
    public void writeReadFloat(String impl) {
        var ser = serializer("S", named("a", floatField()));
        var st = makeState(impl, ser);
        write(st, fp(0), 3.14f);
        assertEquals(read(st, fp(0)), 3.14f);
    }

    @Test(dataProvider = "impls")
    public void writeReadLong(String impl) {
        var ser = serializer("S", named("a", longField()));
        var st = makeState(impl, ser);
        write(st, fp(0), 1234567890123L);
        assertEquals(read(st, fp(0)), 1234567890123L);
    }

    @Test(dataProvider = "impls")
    public void writeReadBool(String impl) {
        var ser = serializer("S", named("a", boolField()));
        var st = makeState(impl, ser);
        write(st, fp(0), true);
        assertEquals(read(st, fp(0)), true);
    }

    // ---------- 7.3.2 Write + read String ----------

    @Test(dataProvider = "impls")
    public void writeReadString(String impl) {
        var ser = serializer("S", named("a", stringField()));
        var st = makeState(impl, ser);
        write(st, fp(0), "hello");
        assertEquals(read(st, fp(0)), "hello");
    }

    // ---------- 7.3.3 Write + read Vector 2D/3D/4D ----------

    @Test(dataProvider = "impls")
    public void writeReadVector2(String impl) {
        var ser = serializer("S", named("a", vectorField(2)));
        var st = makeState(impl, ser);
        write(st, fp(0), new Vector(1f, 2f));
        assertEquals(read(st, fp(0)), new Vector(1f, 2f));
    }

    @Test(dataProvider = "impls")
    public void writeReadVector3(String impl) {
        var ser = serializer("S", named("a", vectorField(3)));
        var st = makeState(impl, ser);
        write(st, fp(0), new Vector(1f, 2f, 3f));
        assertEquals(read(st, fp(0)), new Vector(1f, 2f, 3f));
    }

    @Test(dataProvider = "impls")
    public void writeReadVector4(String impl) {
        var ser = serializer("S", named("a", vectorField(4)));
        var st = makeState(impl, ser);
        write(st, fp(0), new Vector(1f, 2f, 3f, 4f));
        assertEquals(read(st, fp(0)), new Vector(1f, 2f, 3f, 4f));
    }

    // ---------- 7.3.4 Unwritten field returns null ----------

    @Test(dataProvider = "impls")
    public void unwrittenFieldIsNull(String impl) {
        var ser = serializer("S", named("a", intField()));
        var st = makeState(impl, ser);
        assertNull(read(st, fp(0)));
    }

    // ---------- 7.3.5 Multiple fields independent ----------

    @Test(dataProvider = "impls")
    public void multipleFieldsIndependent(String impl) {
        var ser = serializer("S",
            named("a", intField()),
            named("b", floatField()),
            named("c", stringField()));
        var st = makeState(impl, ser);
        write(st, fp(0), 10);
        write(st, fp(1), 2.5f);
        write(st, fp(2), "x");
        assertEquals(read(st, fp(0)), 10);
        assertEquals(read(st, fp(1)), 2.5f);
        assertEquals(read(st, fp(2)), "x");
    }

    // ---------- 7.3.6 Overwrite existing value ----------

    @Test(dataProvider = "impls")
    public void overwriteReturnsNewValue(String impl) {
        var ser = serializer("S", named("a", intField()));
        var st = makeState(impl, ser);
        write(st, fp(0), 1);
        write(st, fp(0), 2);
        assertEquals(read(st, fp(0)), 2);
    }

    // ---------- 7.3.7 Sub-serializer access ----------

    @Test(dataProvider = "impls")
    public void subSerializerAccess(String impl) {
        var inner = serializer("Inner", named("x", intField()));
        var outer = serializer("Outer", named("s", serializerField(inner)));
        var st = makeState(impl, outer);
        write(st, fp(0, 0), 99);
        assertEquals(read(st, fp(0, 0)), 99);
    }

    // ---------- 7.3.8 Array element access ----------

    @Test(dataProvider = "impls")
    public void arrayElementAccess(String impl) {
        var arr = arrayField(intField(), 4);
        var ser = serializer("S", named("a", arr));
        var st = makeState(impl, ser);
        write(st, fp(0, 0), 10);
        write(st, fp(0, 2), 30);
        assertEquals(read(st, fp(0, 0)), 10);
        assertEquals(read(st, fp(0, 2)), 30);
        assertNull(read(st, fp(0, 1)));
        assertNull(read(st, fp(0, 3)));
    }

    // ---------- 7.3.9 Nested: array inside sub-ser, sub-ser inside array ----------

    @Test(dataProvider = "impls")
    public void arrayInsideSubSerializer(String impl) {
        var inner = serializer("Inner", named("arr", arrayField(intField(), 3)));
        var outer = serializer("Outer", named("s", serializerField(inner)));
        var st = makeState(impl, outer);
        write(st, fp(0, 0, 0), 10);
        write(st, fp(0, 0, 2), 30);
        assertEquals(read(st, fp(0, 0, 0)), 10);
        assertEquals(read(st, fp(0, 0, 2)), 30);
    }

    @Test(dataProvider = "impls")
    public void subSerializerInsideArray(String impl) {
        var inner = serializer("Inner", named("x", intField()), named("y", intField()));
        var arr = arrayField(serializerField(inner), 3);
        var outer = serializer("Outer", named("a", arr));
        var st = makeState(impl, outer);
        write(st, fp(0, 0, 0), 1);
        write(st, fp(0, 0, 1), 2);
        write(st, fp(0, 2, 0), 5);
        assertEquals(read(st, fp(0, 0, 0)), 1);
        assertEquals(read(st, fp(0, 0, 1)), 2);
        assertEquals(read(st, fp(0, 2, 0)), 5);
        assertNull(read(st, fp(0, 1, 0)));
    }

    // ---------- 7.3.9a Array with String element ----------

    @Test(dataProvider = "impls")
    public void arrayWithStringElements(String impl) {
        var arr = arrayField(stringField(), 3);
        var ser = serializer("S", named("a", arr));
        var st = makeState(impl, ser);
        write(st, fp(0, 0), "zero");
        write(st, fp(0, 1), "one");
        write(st, fp(0, 2), "two");
        assertEquals(read(st, fp(0, 0)), "zero");
        assertEquals(read(st, fp(0, 1)), "one");
        assertEquals(read(st, fp(0, 2)), "two");
    }

    // ---------- 7.3.9b Array with composite element (String + int) ----------

    @Test(dataProvider = "impls")
    public void arrayWithCompositeElements(String impl) {
        var inner = serializer("Inner",
            named("name", stringField()),
            named("n", intField()));
        var arr = arrayField(serializerField(inner), 3);
        var ser = serializer("S", named("a", arr));
        var st = makeState(impl, ser);
        write(st, fp(0, 0, 0), "zero");
        write(st, fp(0, 0, 1), 0);
        write(st, fp(0, 1, 0), "one");
        write(st, fp(0, 1, 1), 1);
        write(st, fp(0, 2, 0), "two");
        write(st, fp(0, 2, 1), 2);
        assertEquals(read(st, fp(0, 0, 0)), "zero");
        assertEquals(read(st, fp(0, 0, 1)), 0);
        assertEquals(read(st, fp(0, 1, 0)), "one");
        assertEquals(read(st, fp(0, 1, 1)), 1);
        assertEquals(read(st, fp(0, 2, 0)), "two");
        assertEquals(read(st, fp(0, 2, 1)), 2);
    }

    // ---------- 7.3.10 copy() then write on original ----------

    @Test(dataProvider = "impls")
    public void copyThenWriteOriginalCopyUnchanged(String impl) {
        var ser = serializer("S", named("a", intField()));
        var st = makeState(impl, ser);
        write(st, fp(0), 1);
        var cp = st.copy();
        write(st, fp(0), 2);
        assertEquals(read(cp, fp(0)), 1);
        assertEquals(read(st, fp(0)), 2);
    }

    // ---------- 7.3.11 copy() then write on copy ----------

    @Test(dataProvider = "impls")
    public void copyThenWriteCopyOriginalUnchanged(String impl) {
        var ser = serializer("S", named("a", intField()));
        var st = makeState(impl, ser);
        write(st, fp(0), 1);
        var cp = st.copy();
        write(cp, fp(0), 2);
        assertEquals(read(st, fp(0)), 1);
        assertEquals(read(cp, fp(0)), 2);
    }

    // ---------- 7.3.12 Both sides writable after copy ----------

    @Test(dataProvider = "impls")
    public void bothSidesIndependentAfterCopy(String impl) {
        var ser = serializer("S", named("a", intField()), named("b", intField()));
        var st = makeState(impl, ser);
        write(st, fp(0), 1);
        write(st, fp(1), 10);
        var cp = st.copy();
        write(st, fp(0), 2);
        write(cp, fp(1), 20);
        assertEquals(read(st, fp(0)), 2);
        assertEquals(read(st, fp(1)), 10);
        assertEquals(read(cp, fp(0)), 1);
        assertEquals(read(cp, fp(1)), 20);
    }

    // ---------- 7.3.13 Multiple copies from same source ----------

    @Test(dataProvider = "impls")
    public void multipleCopiesIndependent(String impl) {
        var ser = serializer("S", named("a", intField()));
        var st = makeState(impl, ser);
        write(st, fp(0), 1);
        var c1 = st.copy();
        var c2 = st.copy();
        var c3 = st.copy();
        write(c1, fp(0), 10);
        write(c2, fp(0), 20);
        write(c3, fp(0), 30);
        assertEquals(read(st, fp(0)), 1);
        assertEquals(read(c1, fp(0)), 10);
        assertEquals(read(c2, fp(0)), 20);
        assertEquals(read(c3, fp(0)), 30);
    }

    // ---------- 7.3.14 ResizeVector: grow, write, read back ----------

    @Test(dataProvider = "impls")
    public void resizeVectorGrowAndWrite(String impl) {
        var vec = vectorFieldOf(intField());
        var ser = serializer("S", named("v", vec));
        var st = makeState(impl, ser);
        resize(st, fp(0), 3);
        write(st, fp(0, 0), 10);
        write(st, fp(0, 1), 20);
        write(st, fp(0, 2), 30);
        assertEquals(read(st, fp(0, 0)), 10);
        assertEquals(read(st, fp(0, 1)), 20);
        assertEquals(read(st, fp(0, 2)), 30);
    }

    // ---------- 7.3.15 ResizeVector: shrink preserves data ----------

    @Test(dataProvider = "impls")
    public void resizeVectorShrinkPreserves(String impl) {
        var vec = vectorFieldOf(intField());
        var ser = serializer("S", named("v", vec));
        var st = makeState(impl, ser);
        resize(st, fp(0), 4);
        write(st, fp(0, 0), 1);
        write(st, fp(0, 1), 2);
        write(st, fp(0, 2), 3);
        write(st, fp(0, 3), 4);
        resize(st, fp(0), 2);
        assertEquals(read(st, fp(0, 0)), 1);
        assertEquals(read(st, fp(0, 1)), 2);
    }

    // ---------- 7.3.16 ResizeVector: resize to 0 ----------

    @Test(dataProvider = "impls")
    public void resizeVectorToZero(String impl) {
        var vec = vectorFieldOf(intField());
        var ser = serializer("S", named("v", vec));
        var st = makeState(impl, ser);
        resize(st, fp(0), 3);
        write(st, fp(0, 0), 10);
        resize(st, fp(0), 0);
        assertNull(read(st, fp(0, 0)));
    }

    // ---------- 7.3.17 SwitchPointer: set, write, read ----------

    @Test(dataProvider = "impls")
    public void switchPointerSetAndWrite(String impl) {
        var inner = serializer("Inner", named("x", intField()));
        var ptr = pointerField(inner);
        var outer = serializer("Outer", named("p", ptr));
        var st = makeState(impl, outer);
        switchPtr(st, fp(0), inner);
        write(st, fp(0, 0), 42);
        assertEquals(read(st, fp(0, 0)), 42);
    }

    // ---------- 7.3.18 SwitchPointer: switch to different serializer clears old ----------

    @Test(dataProvider = "impls")
    public void switchPointerSwitchClearsOld(String impl) {
        var innerA = serializer("A", named("x", intField()));
        var innerB = serializer("B", named("y", intField()));
        var ptr = pointerField(innerA, innerB);
        var outer = serializer("Outer", named("p", ptr));
        var st = makeState(impl, outer);
        switchPtr(st, fp(0), innerA);
        write(st, fp(0, 0), 42);
        switchPtr(st, fp(0), innerB);
        assertNull(read(st, fp(0, 0)));
    }

    // ---------- 7.3.19 SwitchPointer: set to null ----------

    @Test(dataProvider = "impls")
    public void switchPointerSetNull(String impl) {
        var inner = serializer("Inner", named("x", intField()));
        var ptr = pointerField(inner);
        var outer = serializer("Outer", named("p", ptr));
        var st = makeState(impl, outer);
        switchPtr(st, fp(0), inner);
        write(st, fp(0, 0), 42);
        switchPtr(st, fp(0), null);
        assertNull(read(st, fp(0, 0)));
    }

    // ---------- 7.3.20 copy() then write sub-state: original unchanged ----------

    @Test(dataProvider = "impls")
    public void copyThenWriteSubStateOriginalUnchanged(String impl) {
        var vec = vectorFieldOf(intField());
        var ser = serializer("S", named("v", vec));
        var st = makeState(impl, ser);
        resize(st, fp(0), 3);
        write(st, fp(0, 0), 1);
        var cp = st.copy();
        write(cp, fp(0, 0), 99);
        assertEquals(read(st, fp(0, 0)), 1);
        assertEquals(read(cp, fp(0, 0)), 99);
    }

    // ---------- 7.3.21 copy() then resize vector on copy ----------

    @Test(dataProvider = "impls")
    public void copyThenResizeVectorOnCopy(String impl) {
        var vec = vectorFieldOf(intField());
        var ser = serializer("S", named("v", vec));
        var st = makeState(impl, ser);
        resize(st, fp(0), 3);
        write(st, fp(0, 0), 1);
        write(st, fp(0, 1), 2);
        write(st, fp(0, 2), 3);
        var cp = st.copy();
        resize(cp, fp(0), 1);
        assertEquals(read(st, fp(0, 0)), 1);
        assertEquals(read(st, fp(0, 1)), 2);
        assertEquals(read(st, fp(0, 2)), 3);
        assertEquals(read(cp, fp(0, 0)), 1);
    }

    // ---------- 7.3.22 Both sides write same sub-state path after copy ----------

    @Test(dataProvider = "impls")
    public void bothSidesWriteSubStateAfterCopy(String impl) {
        var vec = vectorFieldOf(intField());
        var ser = serializer("S", named("v", vec));
        var st = makeState(impl, ser);
        resize(st, fp(0), 3);
        write(st, fp(0, 0), 1);
        var cp = st.copy();
        write(st, fp(0, 1), 200);
        write(cp, fp(0, 1), 300);
        assertEquals(read(st, fp(0, 1)), 200);
        assertEquals(read(cp, fp(0, 1)), 300);
    }

    // ---------- 7.3.22a WriteValue on unset Primitive → true ----------

    @Test(dataProvider = "impls")
    public void writePrimitiveUnsetReturnsTrue(String impl) {
        var ser = serializer("S", named("a", intField()));
        var st = makeState(impl, ser);
        assertTrue(write(st, fp(0), 1));
    }

    // ---------- 7.3.22b WriteValue already-set Primitive with new value → false ----------

    @Test(dataProvider = "impls")
    public void writePrimitiveAlreadySetReturnsFalse(String impl) {
        var ser = serializer("S", named("a", intField()));
        var st = makeState(impl, ser);
        write(st, fp(0), 1);
        assertFalse(write(st, fp(0), 2));
    }

    // ---------- 7.3.22c WriteValue null on set Primitive → true ----------

    @Test(dataProvider = "impls")
    public void writePrimitiveNullOnSetReturnsTrue(String impl) {
        var ser = serializer("S", named("a", intField()));
        var st = makeState(impl, ser);
        write(st, fp(0), 1);
        assertTrue(write(st, fp(0), null));
    }

    // ---------- 7.3.22d WriteValue on unset Ref → true ----------

    @Test(dataProvider = "impls")
    public void writeRefUnsetReturnsTrue(String impl) {
        var ser = serializer("S", named("a", stringField()));
        var st = makeState(impl, ser);
        assertTrue(write(st, fp(0), "x"));
    }

    // ---------- 7.3.22e WriteValue null on set Ref → true ----------

    @Test(dataProvider = "impls")
    public void writeRefNullOnSetReturnsTrue(String impl) {
        var ser = serializer("S", named("a", stringField()));
        var st = makeState(impl, ser);
        write(st, fp(0), "x");
        assertTrue(write(st, fp(0), null));
    }

    // ---------- 7.3.22f ResizeVector same count → false ----------

    @Test(dataProvider = "impls")
    public void resizeVectorSameCountReturnsFalse(String impl) {
        var vec = vectorFieldOf(intField());
        var ser = serializer("S", named("v", vec));
        var st = makeState(impl, ser);
        resize(st, fp(0), 3);
        assertFalse(resize(st, fp(0), 3));
    }

    // ---------- 7.3.22g ResizeVector grow → false; shrink dropping populated → true ----------

    @Test(dataProvider = "impls")
    public void resizeVectorGrowReturnsFalse(String impl) {
        var vec = vectorFieldOf(intField());
        var ser = serializer("S", named("v", vec));
        var st = makeState(impl, ser);
        resize(st, fp(0), 3);
        assertFalse(resize(st, fp(0), 5));
    }

    @Test(dataProvider = "impls")
    public void resizeVectorFreshReturnsFalse(String impl) {
        var vec = vectorFieldOf(intField());
        var ser = serializer("S", named("v", vec));
        var st = makeState(impl, ser);
        assertFalse(resize(st, fp(0), 5));
    }

    @Test(dataProvider = "impls")
    public void resizeVectorShrinkDroppingPopulatedReturnsTrue(String impl) {
        var vec = vectorFieldOf(intField());
        var ser = serializer("S", named("v", vec));
        var st = makeState(impl, ser);
        resize(st, fp(0), 5);
        write(st, fp(0, 3), 30);
        assertTrue(resize(st, fp(0), 2));
    }

    @Test(dataProvider = "impls")
    public void resizeVectorShrinkEmptyTailReturnsFalse(String impl) {
        var vec = vectorFieldOf(intField());
        var ser = serializer("S", named("v", vec));
        var st = makeState(impl, ser);
        resize(st, fp(0), 5);
        write(st, fp(0, 0), 10);
        assertFalse(resize(st, fp(0), 2));
    }

    // ---------- 7.3.22h SwitchPointer: fresh→false, clear populated→true, same→false ----------

    @Test(dataProvider = "impls")
    public void switchPointerFreshReturnsFalse(String impl) {
        var inner = serializer("Inner", named("x", intField()));
        var ptr = pointerField(inner);
        var outer = serializer("Outer", named("p", ptr));
        var st = makeState(impl, outer);
        assertFalse(switchPtr(st, fp(0), inner));
    }

    @Test(dataProvider = "impls")
    public void switchPointerClearPopulatedReturnsTrue(String impl) {
        var inner = serializer("Inner", named("x", intField()));
        var ptr = pointerField(inner);
        var outer = serializer("Outer", named("p", ptr));
        var st = makeState(impl, outer);
        switchPtr(st, fp(0), inner);
        write(st, fp(0, 0), 42);
        assertTrue(switchPtr(st, fp(0), null));
    }

    @Test(dataProvider = "impls")
    public void switchPointerClearEmptyReturnsFalse(String impl) {
        var inner = serializer("Inner", named("x", intField()));
        var ptr = pointerField(inner);
        var outer = serializer("Outer", named("p", ptr));
        var st = makeState(impl, outer);
        switchPtr(st, fp(0), inner);
        assertFalse(switchPtr(st, fp(0), null));
    }

    @Test(dataProvider = "impls")
    public void switchPointerSwitchPopulatedReturnsTrue(String impl) {
        var innerA = serializer("A", named("x", intField()));
        var innerB = serializer("B", named("y", intField()));
        var ptr = pointerField(innerA, innerB);
        var outer = serializer("Outer", named("p", ptr));
        var st = makeState(impl, outer);
        switchPtr(st, fp(0), innerA);
        write(st, fp(0, 0), 42);
        assertTrue(switchPtr(st, fp(0), innerB));
    }

    @Test(dataProvider = "impls")
    public void switchPointerSameReturnsFalse(String impl) {
        var inner = serializer("Inner", named("x", intField()));
        var ptr = pointerField(inner);
        var outer = serializer("Outer", named("p", ptr));
        var st = makeState(impl, outer);
        switchPtr(st, fp(0), inner);
        assertFalse(switchPtr(st, fp(0), inner));
    }

    // ---------- 7.3.22i All 3 impls agree on same inputs ----------

    @Test
    public void allImplsAgreeOnMixedInputs() {
        var innerA = serializer("A", named("x", intField()));
        var innerB = serializer("B", named("y", intField()));
        var ptr = pointerField(innerA, innerB);
        var vec = vectorFieldOf(intField());
        var rootSer = serializer("Root",
            named("n", intField()),
            named("s", stringField()),
            named("v", vec),
            named("p", ptr));
        var states = new EntityState[] {
            makeState(TestStateFactory.NESTED_ARRAY, rootSer),
            makeState(TestStateFactory.TREE_MAP, rootSer),
            makeState(TestStateFactory.FLAT, rootSer),
        };
        record Step(String name, java.util.function.Function<EntityState, Boolean> op) {}
        var steps = new Step[] {
            new Step("write n=1", s -> write(s, fp(0), 1)),
            new Step("write n=1 again", s -> write(s, fp(0), 1)),
            new Step("write n=2", s -> write(s, fp(0), 2)),
            new Step("write s='a'", s -> write(s, fp(1), "a")),
            new Step("write s='a' again", s -> write(s, fp(1), "a")),
            new Step("clear n", s -> write(s, fp(0), null)),
            new Step("clear s", s -> write(s, fp(1), null)),
            new Step("resize v=5", s -> resize(s, fp(2), 5)),
            new Step("resize v=5 same", s -> resize(s, fp(2), 5)),
            new Step("resize v=8 grow", s -> resize(s, fp(2), 8)),
            new Step("write v[3]=30", s -> write(s, fp(2, 3), 30)),
            new Step("resize v=2 dropping", s -> resize(s, fp(2), 2)),
            new Step("resize v=5 regrow", s -> resize(s, fp(2), 5)),
            new Step("resize v=2 empty-tail", s -> resize(s, fp(2), 2)),
            new Step("switch p→A fresh", s -> switchPtr(s, fp(3), innerA)),
            new Step("switch p→A same", s -> switchPtr(s, fp(3), innerA)),
            new Step("write p.x=42", s -> write(s, fp(3, 0), 42)),
            new Step("switch p→B populated", s -> switchPtr(s, fp(3), innerB)),
            new Step("switch p→null empty", s -> switchPtr(s, fp(3), null)),
        };
        for (var step : steps) {
            var r0 = step.op.apply(states[0]);
            var r1 = step.op.apply(states[1]);
            var r2 = step.op.apply(states[2]);
            assertEquals(r1, r0, "step '" + step.name + "': TREE_MAP=" + r1 + " vs NESTED_ARRAY=" + r0);
            assertEquals(r2, r0, "step '" + step.name + "': FLAT=" + r2 + " vs NESTED_ARRAY=" + r0);
        }
    }

    // ---------- 7.3.23 Empty state iterator ----------

    @Test(dataProvider = "impls")
    public void emptyStateIteratorYieldsNothing(String impl) {
        var ser = serializer("S", named("a", intField()));
        var st = makeState(impl, ser);
        assertTrue(paths(st).isEmpty());
    }

    // ---------- 7.3.24 Some fields set — only set yielded ----------

    @Test(dataProvider = "impls")
    public void iteratorYieldsOnlySetFields(String impl) {
        var ser = serializer("S",
            named("a", intField()),
            named("b", intField()),
            named("c", intField()));
        var st = makeState(impl, ser);
        write(st, fp(0), 1);
        write(st, fp(2), 3);
        var ps = paths(st);
        assertEquals(ps.size(), 2);
        assertTrue(ps.contains(fp(0)));
        assertTrue(ps.contains(fp(2)));
    }

    // ---------- 7.3.25 Array sparse — only written yielded ----------

    @Test(dataProvider = "impls")
    public void iteratorYieldsSparseArray(String impl) {
        var arr = arrayField(intField(), 4);
        var ser = serializer("S", named("a", arr));
        var st = makeState(impl, ser);
        write(st, fp(0, 1), 10);
        write(st, fp(0, 3), 30);
        var ps = paths(st);
        assertEquals(ps.size(), 2);
        assertTrue(ps.contains(fp(0, 1)));
        assertTrue(ps.contains(fp(0, 3)));
    }

    // ---------- 7.3.26 Sub-state fields included ----------

    @Test(dataProvider = "impls")
    public void iteratorIncludesSubStateFields(String impl) {
        var vec = vectorFieldOf(intField());
        var ser = serializer("S", named("v", vec));
        var st = makeState(impl, ser);
        resize(st, fp(0), 2);
        write(st, fp(0, 0), 10);
        write(st, fp(0, 1), 20);
        var ps = paths(st);
        assertTrue(ps.contains(fp(0, 0)));
        assertTrue(ps.contains(fp(0, 1)));
    }

    // ---------- 7.3.27 After ResizeVector shrink — removed paths gone ----------

    @Test(dataProvider = "impls")
    public void iteratorAfterShrinkExcludesRemoved(String impl) {
        var vec = vectorFieldOf(intField());
        var ser = serializer("S", named("v", vec));
        var st = makeState(impl, ser);
        resize(st, fp(0), 4);
        write(st, fp(0, 0), 1);
        write(st, fp(0, 1), 2);
        write(st, fp(0, 2), 3);
        write(st, fp(0, 3), 4);
        resize(st, fp(0), 2);
        var ps = paths(st);
        assertFalse(ps.contains(fp(0, 2)));
        assertFalse(ps.contains(fp(0, 3)));
    }

    // ---------- Slab hygiene: recursive release of freelist slots ----------

    @DataProvider(name = "slabImpls")
    public Object[][] slabImpls() {
        return new Object[][] {
            {TestStateFactory.NESTED_ARRAY},
            {TestStateFactory.FLAT},
        };
    }

    private static int slabSize(EntityState st) {
        if (st instanceof S2NestedArrayEntityState n) return n.slabSize();
        if (st instanceof S2FlatEntityState f) return f.slabSize();
        throw new UnsupportedOperationException(st.getClass().getName());
    }

    private static int freeSlotCount(EntityState st) {
        if (st instanceof S2NestedArrayEntityState n) return n.freeSlotCount();
        if (st instanceof S2FlatEntityState f) return f.freeSlotCount();
        throw new UnsupportedOperationException(st.getClass().getName());
    }

    private static int liveSlabCount(EntityState st) {
        return slabSize(st) - freeSlotCount(st);
    }

    @Test(dataProvider = "slabImpls")
    public void switchPointerToNullReleasesNestedSubtree(String impl) {
        var leaf = serializer("Leaf", named("x", intField()));
        var mid = serializer("Mid", named("p", pointerField(leaf)));
        var outer = serializer("Outer", named("p", pointerField(mid)));
        var st = makeState(impl, outer);

        var baseline = liveSlabCount(st);

        switchPtr(st, fp(0), mid);
        switchPtr(st, fp(0, 0), leaf);
        write(st, fp(0, 0, 0), 42);

        assertTrue(liveSlabCount(st) > baseline, "slab grew while building subtree");

        switchPtr(st, fp(0), null);

        assertEquals(liveSlabCount(st), baseline,
            "clearing outer pointer must recursively release inner mid and leaf slots");
    }

    @Test(dataProvider = "slabImpls")
    public void switchPointerToDifferentSerializerReleasesOldSubtree(String impl) {
        var leaf = serializer("Leaf", named("x", intField()));
        var a = serializer("A", named("p", pointerField(leaf)));
        var b = serializer("B", named("n", intField()));
        var outer = serializer("Outer", named("p", pointerField(a, b)));
        var st = makeState(impl, outer);

        switchPtr(st, fp(0), a);
        switchPtr(st, fp(0, 0), leaf);
        write(st, fp(0, 0, 0), 1);

        var afterA = liveSlabCount(st);

        switchPtr(st, fp(0), b);
        write(st, fp(0, 0), 2);

        var afterB = liveSlabCount(st);
        assertTrue(afterB < afterA,
            "switching to a simpler serializer should release the deeper old subtree");
    }

    @Test(dataProvider = "slabImpls")
    public void resizeVectorShrinkReleasesDroppedSubEntries(String impl) {
        var leaf = serializer("Leaf", named("x", intField()));
        var element = serializer("E", named("p", pointerField(leaf)));
        var ser = serializer("S", named("v", vectorFieldOf(serializerField(element))));
        var st = makeState(impl, ser);

        resize(st, fp(0), 5);
        for (var i = 0; i < 5; i++) {
            switchPtr(st, fp(0, i, 0), leaf);
            write(st, fp(0, i, 0, 0), i);
        }

        var afterGrow = liveSlabCount(st);

        resize(st, fp(0), 2);

        var afterShrink = liveSlabCount(st);
        assertTrue(afterShrink < afterGrow,
            "shrinking a vector of sub-entries must release the dropped tail's slab slots");
        assertEquals(read(st, fp(0, 0, 0, 0)), 0);
        assertEquals(read(st, fp(0, 1, 0, 0)), 1);
    }

    @Test(dataProvider = "slabImpls")
    public void resizeVectorToZeroReleasesAllElementSubEntries(String impl) {
        var leaf = serializer("Leaf", named("x", intField()));
        var element = serializer("E", named("p", pointerField(leaf)));
        var ser = serializer("S", named("v", vectorFieldOf(serializerField(element))));
        var st = makeState(impl, ser);

        resize(st, fp(0), 4);
        for (var i = 0; i < 4; i++) {
            switchPtr(st, fp(0, i, 0), leaf);
            write(st, fp(0, i, 0, 0), i);
        }

        var populated = liveSlabCount(st);

        resize(st, fp(0), 0);

        var emptied = liveSlabCount(st);
        assertTrue(emptied < populated, "resize to 0 must release all element slots");
    }

    @Test(dataProvider = "slabImpls")
    public void freedSlotsAreReusedBySubsequentAllocations(String impl) {
        var leaf = serializer("Leaf", named("x", intField()));
        var outer = serializer("Outer", named("p", pointerField(leaf)));
        var st = makeState(impl, outer);

        switchPtr(st, fp(0), leaf);
        write(st, fp(0, 0), 1);
        var afterFirstAlloc = slabSize(st);
        var liveAfterFirst = liveSlabCount(st);

        switchPtr(st, fp(0), null);
        assertTrue(freeSlotCount(st) > 0, "freelist populated after clearing pointer");

        switchPtr(st, fp(0), leaf);
        write(st, fp(0, 0), 2);

        assertEquals(slabSize(st), afterFirstAlloc,
            "slab size stable — freed slots were reused instead of appending new ones");
        assertEquals(liveSlabCount(st), liveAfterFirst,
            "live slab count returns to the pre-clear baseline");
    }

    @Test(dataProvider = "slabImpls")
    public void releaseOnCopyDoesNotAffectOriginal(String impl) {
        var leaf = serializer("Leaf", named("x", intField()));
        var element = serializer("E", named("p", pointerField(leaf)));
        var ser = serializer("S", named("v", vectorFieldOf(serializerField(element))));
        var st = makeState(impl, ser);

        resize(st, fp(0), 4);
        for (var i = 0; i < 4; i++) {
            switchPtr(st, fp(0, i, 0), leaf);
            write(st, fp(0, i, 0, 0), i);
        }

        var stLiveBefore = liveSlabCount(st);
        var cp = st.copy();

        resize(cp, fp(0), 1);

        assertTrue(liveSlabCount(cp) < stLiveBefore,
            "release on the copy reduced its live slab count");
        assertEquals(liveSlabCount(st), stLiveBefore,
            "original's live slab count is unchanged after release on copy");
        for (var i = 0; i < 4; i++) {
            assertEquals(read(st, fp(0, i, 0, 0)), i,
                "original's data at index " + i + " intact");
        }
        assertEquals(read(cp, fp(0, 0, 0, 0)), 0, "copy retained surviving element");
    }
}
