package skadistats.clarity.decoder.s2;

import skadistats.clarity.model.FieldPath;

public class DumpEntry {
    final String fieldPath;
    final String name;
    final String value;

    public DumpEntry(FieldPath fieldPath, String name, Object value) {
        this.fieldPath = fieldPath.toString();
        this.name = name;
        this.value = value.toString();
    }
}
