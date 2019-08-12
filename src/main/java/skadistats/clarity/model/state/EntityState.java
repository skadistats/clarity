package skadistats.clarity.model.state;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.util.TextTable;

import java.util.Collection;

public interface EntityState {

    EntityState clone();

    void setValueForFieldPath(FieldPath fp, Object value);
    <T> T getValueForFieldPath(FieldPath fp);
    String getNameForFieldPath(FieldPath fp);

    Collection<FieldPath> collectFieldPaths();

    default String dump(String title) {
        final TextTable table = new TextTable.Builder()
                .setFrame(TextTable.FRAME_COMPAT)
                .addColumn("FP")
                .addColumn("Property")
                .addColumn("Value")
                .setTitle(title)
                .build();

        int i = 0;
        for (FieldPath fp : collectFieldPaths()) {
            table.setData(i, 0, fp);
            table.setData(i, 1, getNameForFieldPath(fp));
            table.setData(i, 2, getValueForFieldPath(fp));
            i++;
        }

        return table.toString();
    }

}
