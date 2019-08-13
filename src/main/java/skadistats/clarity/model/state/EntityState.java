package skadistats.clarity.model.state;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.util.TextTable;

import java.util.Collection;
import java.util.function.Function;

public interface EntityState<F extends FieldPath> {

    EntityState clone();

    void setValueForFieldPath(F fp, Object value);
    <T> T getValueForFieldPath(F fp);

    Collection<F> collectFieldPaths();

    default String dump(String title, Function<F, String> nameResolver) {
        final TextTable table = new TextTable.Builder()
                .setFrame(TextTable.FRAME_COMPAT)
                .addColumn("FP")
                .addColumn("Property")
                .addColumn("Value")
                .setTitle(title)
                .build();

        int i = 0;
        for (F fp : collectFieldPaths()) {
            table.setData(i, 0, fp);
            table.setData(i, 1, nameResolver.apply(fp));
            table.setData(i, 2, getValueForFieldPath(fp));
            i++;
        }

        return table.toString();
    }

}
