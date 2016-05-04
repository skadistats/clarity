package skadistats.clarity.decoder.s2.field;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldType {

    private static final Pattern FIELD_TYPE_PATTERN = Pattern.compile("(.*?)(< (.*) >)?(\\*)?(\\[(.*?)\\])?");

    private final String baseType;
    private final FieldType genericType;
    private final boolean pointer;
    private final String elementCount;

    public FieldType(String typeString) {
        Matcher m = FIELD_TYPE_PATTERN.matcher(typeString);
        if (!m.matches()) {
            throw new RuntimeException("cannot parse field type");
        }
        baseType = m.group(1);
        genericType = m.group(3) != null ? new FieldType(m.group(3)) : null;
        pointer = m.group(4) != null;
        elementCount = m.group(6);
    }

    public String getBaseType() {
        return baseType;
    }

    public FieldType getGenericType() {
        return genericType;
    }

    public boolean isPointer() {
        return pointer;
    }

    public String getElementCount() {
        return elementCount;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean omitElementCount) {
        final StringBuilder sb = new StringBuilder();
        sb.append(baseType);
        if (genericType != null) {
            sb.append("< ");
            sb.append(genericType.toString());
            sb.append(" >");
        }
        if (pointer) {
            sb.append('*');
        }
        if (!omitElementCount && elementCount != null) {
            sb.append('[');
            sb.append(elementCount.toString());
            sb.append(']');
        }
        return sb.toString();
    }

}
