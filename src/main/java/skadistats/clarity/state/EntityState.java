package skadistats.clarity.state;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s1.S1FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.state.s1.S1EntityState;
import skadistats.clarity.state.s2.S2EntityState;
import skadistats.clarity.util.TextTable;

import java.util.Iterator;
import java.util.function.Function;

public sealed interface EntityState permits S1EntityState, S2EntityState {

    Iterator<FieldPath> fieldPathIterator();

    @SuppressWarnings("unchecked")
    static <T> T getValueForFieldPath(EntityState state, FieldPath fp) {
        return (T) switch (state) {
            case S1EntityState s1 -> s1.getValueForFieldPath((S1FieldPath) fp);
            case S2EntityState s2 -> s2.getValueForFieldPath((S2FieldPath) fp);
        };
    }

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

    EntityState copy();

    static boolean applyMutation(EntityState state, FieldPath fp, StateMutation mutation) {
        return switch (state) {
            case S1EntityState s1 -> s1.applyMutation((S1FieldPath) fp, mutation);
            case S2EntityState s2 -> s2.applyMutation((S2FieldPath) fp, mutation);
        };
    }

    static int getInt(EntityState state, FieldPath fp) {
        return switch (state) {
            case S1EntityState s1 -> s1.getInt((S1FieldPath) fp);
            case S2EntityState s2 -> s2.getInt((S2FieldPath) fp);
        };
    }

    static long getLong(EntityState state, FieldPath fp) {
        return switch (state) {
            case S1EntityState s1 -> s1.getLong((S1FieldPath) fp);
            case S2EntityState s2 -> s2.getLong((S2FieldPath) fp);
        };
    }

    static float getFloat(EntityState state, FieldPath fp) {
        return switch (state) {
            case S1EntityState s1 -> s1.getFloat((S1FieldPath) fp);
            case S2EntityState s2 -> s2.getFloat((S2FieldPath) fp);
        };
    }

    static Object getObject(EntityState state, FieldPath fp) {
        return switch (state) {
            case S1EntityState s1 -> s1.getObject((S1FieldPath) fp);
            case S2EntityState s2 -> s2.getObject((S2FieldPath) fp);
        };
    }

    static StateDelta captureChanged(EntityState state, FieldPath[] fps, int num) {
        return switch (state) {
            case S1EntityState s1 -> s1.captureChanged(toS1(fps, num), num);
            case S2EntityState s2 -> s2.captureChanged(toS2(fps, num), num);
        };
    }

    static void applyFrom(EntityState state, StateDelta delta, FieldPath fp) {
        switch (state) {
            case S1EntityState s1 -> s1.applyFrom(delta, (S1FieldPath) fp);
            case S2EntityState s2 -> s2.applyFrom(delta, (S2FieldPath) fp);
        }
    }

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
