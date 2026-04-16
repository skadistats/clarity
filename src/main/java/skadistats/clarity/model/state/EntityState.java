package skadistats.clarity.model.state;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.util.TextTable;

import java.util.Iterator;
import java.util.function.Function;

public interface EntityState {

    EntityState copy();

    // returns true if capacity has changed
    boolean applyMutation(FieldPath fp, StateMutation mutation);

    // Direct-write entry point used by field readers on the hot path.
    // decoded shape depends on the leaf kind at fp:
    //   Primitive / Ref / InlineString leaf -> the decoded value itself
    //   SubState-Pointer leaf               -> a Serializer (or null)
    //   SubState-Vector leaf                -> an Integer length
    // Returns true if capacity has changed.
    boolean write(FieldPath fp, Object decoded);

    <T> T getValueForFieldPath(FieldPath fp);

    Iterator<FieldPath> fieldPathIterator();

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
            table.setData(i, 2, getValueForFieldPath(fp));
            i++;
        }

        return table.toString();
    }

}
