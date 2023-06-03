package skadistats.clarity.io.s2;

import skadistats.clarity.ClarityException;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldType {

    private static final Pattern FIELD_TYPE_PATTERN = Pattern.compile("(.*?)(< (.*) >)?(\\*)?(\\[(.*?)\\])?");

    private final String baseType;
    private final FieldType genericType;
    private final boolean pointer;
    private final String elementCount;
    private final FieldType elementType;

    public FieldType(String typeString) {
        var m = FIELD_TYPE_PATTERN.matcher(typeString);
        if (!m.matches()) {
            throw new ClarityException("cannot parse field type");
        }
        baseType = m.group(1);
        genericType = m.group(3) != null ? forString(m.group(3)) : null;
        pointer = m.group(4) != null;
        elementCount = m.group(6);
        if (elementCount == null) {
            elementType = null;
        } else {
            elementType = forString(toString(true));
        }
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

    public FieldType getElementType() {
        return elementType;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    private String toString(boolean omitElementCount) {
        final var sb = new StringBuilder();
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
            sb.append(elementCount);
            sb.append(']');
        }
        return sb.toString();
    }

    private static final Map<String, FieldType> FIELD_TYPE_MAP = new HashMap<>();

    public static FieldType forString(String fieldTypeString) {
        var result = FIELD_TYPE_MAP.get(fieldTypeString);
        if (result == null) {
            synchronized (FIELD_TYPE_MAP) {
                result = FIELD_TYPE_MAP.get(fieldTypeString);
                if (result == null) {
                    result = new FieldType(fieldTypeString);
                    FIELD_TYPE_MAP.put(fieldTypeString, result);
                }
            }
        }
        return result;
    }

}
