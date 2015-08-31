package skadistats.clarity.model.s2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldType {

    private static final Pattern FIELD_TYPE_PATTERN = Pattern.compile("(.*?)(< (.*) >)?(\\*)?(\\[(.*?)\\])?");

    private static final Set<String> VARIABLE_ARRAYS = new HashSet<>();
    static {
        VARIABLE_ARRAYS.add("DOTA_PlayerChallengeInfo");
        VARIABLE_ARRAYS.add("m_SpeechBubbles");
        VARIABLE_ARRAYS.add("CUtlVector");
    }

    private static final Map<String, Integer> ITEM_COUNTS = new HashMap<>();
    static {
        ITEM_COUNTS.put("MAX_ITEM_STOCKS", 8);
        ITEM_COUNTS.put("MAX_ABILITY_DRAFT_ABILITIES", 48);
    }

    private final String baseType;
    private final FieldType genericType;
    private final boolean pointer;
    private final boolean variableArray;
    private final boolean fixedArray;
    private Integer elementCount;

    public FieldType(String typeString) {
        Matcher m = FIELD_TYPE_PATTERN.matcher(typeString);
        if (!m.matches()) {
            throw new RuntimeException("cannot parse field type");
        }
        baseType = m.group(1);
        genericType = m.group(3) != null ? new FieldType(m.group(3)) : null;
        pointer = m.group(4) != null;
        variableArray = VARIABLE_ARRAYS.contains(baseType);
        String countStr = m.group(6);
        fixedArray = countStr != null;
        if (fixedArray) {
            elementCount = ITEM_COUNTS.get(countStr);
            if (elementCount == null) {
                elementCount = Integer.valueOf(countStr);
            }
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

    public boolean isVariableArray() {
        return variableArray;
    }

    public boolean isFixedArray() {
        return fixedArray;
    }

    public Integer getElementCount() {
        return elementCount;
    }

    @Override
    public String toString() {
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
        if (elementCount != null) {
            sb.append('[');
            sb.append(elementCount.toString());
            sb.append(']');
        }
        return sb.toString();
    }
}
