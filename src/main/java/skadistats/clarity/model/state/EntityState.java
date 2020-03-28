package skadistats.clarity.model.state;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.util.TextTable;

import java.util.Iterator;
import java.util.function.Function;

public interface EntityState {

    EntityState copy();

    void setValueForFieldPath(FieldPath fp, Object value);
    <T> T getValueForFieldPath(FieldPath fp);

    Iterator<FieldPath> fieldPathIterator();

    default String dump(String title, Function<FieldPath, String> nameResolver) {
        final TextTable table = new TextTable.Builder()
                .setFrame(TextTable.FRAME_COMPAT)
                .addColumn("FP")
                .addColumn("Property")
                .addColumn("Value")
                .setTitle(title)
                .build();

        int i = 0;
        final Iterator<FieldPath> iter = fieldPathIterator();
        while (iter.hasNext()) {
            FieldPath fp = iter.next();
            table.setData(i, 0, fp);
            table.setData(i, 1, nameResolver.apply(fp));
            table.setData(i, 2, getValueForFieldPath(fp));
            i++;
        }

        return table.toString();
    }

}
