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

}
