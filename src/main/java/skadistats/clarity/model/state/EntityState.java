package skadistats.clarity.model.state;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.util.TextTable;

import java.util.List;

public interface EntityState {

    EntityState clone();

    void setValueForFieldPath(FieldPath fp, Object value);
    <T> T getValueForFieldPath(FieldPath fp);
    String getNameForFieldPath(FieldPath fp);

    List<FieldPath> collectFieldPaths();

    default String dump(String title) {
        final TextTable table = new TextTable.Builder()
                .setFrame(TextTable.FRAME_COMPAT)
                .addColumn("FP")
                .addColumn("Property")
                .addColumn("Value")
                .setTitle(title)
                .build();

        final List<FieldPath> fieldPaths = collectFieldPaths();
        for (int i = 0; i < fieldPaths.size(); i++) {
            final FieldPath fp = fieldPaths.get(i);
            table.setData(i, 0, fp);
            table.setData(i, 1, getNameForFieldPath(fp));
            table.setData(i, 2, getValueForFieldPath(fp));
        }
        return table.toString();
    }

}
