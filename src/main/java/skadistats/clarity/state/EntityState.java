package skadistats.clarity.state;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s1.S1FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.state.s1.S1EntityState;
import skadistats.clarity.state.s2.S2EntityState;
import skadistats.clarity.util.TextTable;

import java.util.Iterator;
import java.util.function.Function;

/**
 * The property values of one entity, addressed by {@link FieldPath}. The
 * implementations are {@code S1EntityState} (Source 1) and
 * {@code S2EntityState} (Source 2); the static accessors dispatch on the
 * implementation and cast the {@link FieldPath} to the matching type.
 *
 * <p>The storage strategy is chosen per run: Source 1 uses
 * {@code S1EntityStateType.FLAT} by default (alternative {@code OBJECT_ARRAY}),
 * Source 2 uses {@code S2EntityStateType.NESTED_ARRAY} by default
 * (alternatives {@code TREE_MAP}, {@code FLAT}), configurable on the runner with
 * {@code withS1EntityState} / {@code withS2EntityState}.
 *
 * <p>States are mutated by the parser while it reads packets. Hold on to a
 * {@link #copy()} or a {@link StateDelta} if values must survive later updates.
 */
public sealed interface EntityState permits S1EntityState, S2EntityState {

    /**
     * @return an iterator over the field paths of this state's fields
     */
    Iterator<FieldPath> fieldPathIterator();

    /**
     * Reads the value for {@code fp}, boxing primitives. The value is cast
     * unchecked to {@code T}; a wrong {@code T} fails with a
     * {@link ClassCastException} at the call site. Prefer the primitive getters
     * on hot paths.
     */
    @SuppressWarnings("unchecked")
    static <T> T getValueForFieldPath(EntityState state, FieldPath fp) {
        return (T) switch (state) {
            case S1EntityState s1 -> s1.getValueForFieldPath((S1FieldPath) fp);
            case S2EntityState s2 -> s2.getValueForFieldPath((S2FieldPath) fp);
        };
    }

    /**
     * Renders all fields as a text table with field path, property name and value.
     *
     * @param title the table title
     * @param nameResolver maps a field path to its property name, e.g. {@code entity::getNameForFieldPath}
     */
    default String dump(String title, Function<FieldPath, String> nameResolver) {
        final var table = new TextTable.Builder()
                .setFrame(TextTable.FRAME_COMPAT)
                .addColumn("FP")
                .addColumn("Property")
                .addColumn("Value")
                .setTitle(title)
                .build();

        var i = 0;
        final var iter = fieldPathIterator();
        while (iter.hasNext()) {
            var fp = iter.next();
            table.setData(i, 0, fp);
            table.setData(i, 1, nameResolver.apply(fp));
            table.setData(i, 2, getValueForFieldPath(this, fp));
            i++;
        }

        return table.toString();
    }

    /**
     * @return an independent copy of this state
     */
    EntityState copy();

    /** Internal to the parser: applies a decoded mutation to the field at {@code fp}. */
    static boolean applyMutation(EntityState state, FieldPath fp, StateMutation mutation) {
        return switch (state) {
            case S1EntityState s1 -> s1.applyMutation((S1FieldPath) fp, mutation);
            case S2EntityState s2 -> s2.applyMutation((S2FieldPath) fp, mutation);
        };
    }

    /**
     * Reads an {@code int} property without boxing. Returns {@code 0} if the
     * field is unset, the path does not resolve, or the field is not
     * int-typed. Prefer this over {@link #getValueForFieldPath} on hot read
     * paths; the latter boxes every primitive on the way out.
     */
    static int getInt(EntityState state, FieldPath fp) {
        return switch (state) {
            case S1EntityState s1 -> s1.getInt((S1FieldPath) fp);
            case S2EntityState s2 -> s2.getInt((S2FieldPath) fp);
        };
    }

    /** Like {@link #getInt}, for {@code long}-typed fields. */
    static long getLong(EntityState state, FieldPath fp) {
        return switch (state) {
            case S1EntityState s1 -> s1.getLong((S1FieldPath) fp);
            case S2EntityState s2 -> s2.getLong((S2FieldPath) fp);
        };
    }

    /** Like {@link #getInt}, for {@code float}-typed fields. */
    static float getFloat(EntityState state, FieldPath fp) {
        return switch (state) {
            case S1EntityState s1 -> s1.getFloat((S1FieldPath) fp);
            case S2EntityState s2 -> s2.getFloat((S2FieldPath) fp);
        };
    }

    /**
     * Reads a property of any type as an object (strings, vectors, handles,
     * ...). Primitive fields come back boxed; use the primitive getters for
     * those. Returns {@code null} if the field is unset.
     */
    static Object getObject(EntityState state, FieldPath fp) {
        return switch (state) {
            case S1EntityState s1 -> s1.getObject((S1FieldPath) fp);
            case S2EntityState s2 -> s2.getObject((S2FieldPath) fp);
        };
    }

    /**
     * Captures the current values of {@code fps[0..num)} into a sparse
     * {@link StateDelta}, without copying the rest of the state. Typical use
     * is handing the fields changed by an {@code @OnEntityUpdated} event to
     * another thread, which merges them into its own long-lived state with
     * {@link #applyFrom} or {@link #applyAll} instead of taking a full
     * {@link #copy()} per update. The delta is independent of {@code state}:
     * later mutations of {@code state} do not affect it.
     */
    static StateDelta captureChanged(EntityState state, FieldPath[] fps, int num) {
        return switch (state) {
            case S1EntityState s1 -> s1.captureChanged(toS1(fps, num), num);
            case S2EntityState s2 -> s2.captureChanged(toS2(fps, num), num);
        };
    }

    /**
     * Writes the value {@code delta} holds for {@code fp} into {@code state};
     * a field captured as unset is cleared. {@code delta} must come from
     * {@link #captureChanged} on a state of the same entity class.
     */
    static void applyFrom(EntityState state, StateDelta delta, FieldPath fp) {
        switch (state) {
            case S1EntityState s1 -> s1.applyFrom(delta, (S1FieldPath) fp);
            case S2EntityState s2 -> s2.applyFrom(delta, (S2FieldPath) fp);
        }
    }

    /**
     * Applies every field covered by {@code delta} to {@code state}, as by
     * {@link #applyFrom}. Fields not covered by the delta are left untouched.
     */
    static void applyAll(EntityState state, StateDelta delta) {
        switch (state) {
            case S1EntityState s1 -> s1.applyAll(delta);
            case S2EntityState s2 -> s2.applyAll(delta);
        }
    }

    private static S1FieldPath[] toS1(FieldPath[] src, int num) {
        if (src instanceof S1FieldPath[] typed) return typed;
        var out = new S1FieldPath[num];
        for (var i = 0; i < num; i++) out[i] = (S1FieldPath) src[i];
        return out;
    }

    private static S2FieldPath[] toS2(FieldPath[] src, int num) {
        if (src instanceof S2FieldPath[] typed) return typed;
        var out = new S2FieldPath[num];
        for (var i = 0; i < num; i++) out[i] = (S2FieldPath) src[i];
        return out;
    }

}
