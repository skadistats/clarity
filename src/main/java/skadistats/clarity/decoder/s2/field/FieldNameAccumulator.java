package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.decoder.s2.Field;

public class FieldNameAccumulator implements AccessorFunction<String> {

    private final StringBuilder sb = new StringBuilder();
    private AccessorFunction<Field> fieldAccessor;

    public FieldNameAccumulator(AccessorFunction<Field> fieldAccessor) {
        this.fieldAccessor = fieldAccessor;
    }

    @Override
    public AccessorFunction<String> down(int i) {
        fieldAccessor = fieldAccessor.down(i);
        if (sb.length() != 0) {
            sb.append('.');
        }
        sb.append(fieldAccessor.get().getFieldProperties().getNameForIndex(i));
        return this;
    }

    @Override
    public String get() {
        return sb.toString();
    }

}
