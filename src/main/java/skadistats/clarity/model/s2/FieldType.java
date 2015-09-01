package skadistats.clarity.model.s2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldType {

    private static final Pattern FIELD_TYPE_PATTERN = Pattern.compile("(.*?)(< (.*) >)?(\\*)?(\\[(.*?)\\])?");

    private static final Set<String> FIXED_ARRAYS = new HashSet<>();
    static {
        FIXED_ARRAYS.add("DOTA_PlayerChallengeInfo");
        FIXED_ARRAYS.add("m_SpeechBubbles");
    }

    private static final Set<String> GENERIC_ARRAYS = new HashSet<>();
    static {
        GENERIC_ARRAYS.add("CUtlVector");
    }

    private static final Map<String, Integer> ITEM_COUNTS = new HashMap<>();
    static {
        ITEM_COUNTS.put("MAX_ITEM_STOCKS", 8);
        ITEM_COUNTS.put("MAX_ABILITY_DRAFT_ABILITIES", 48);
    }

    private final String baseType;
    private final FieldType genericType;
    private final boolean pointer;
    private final boolean genericArray;
    private final boolean fixedArray;
    private Integer fixedElementCount;

    public FieldType(String typeString) {
        Matcher m = FIELD_TYPE_PATTERN.matcher(typeString);
        if (!m.matches()) {
            throw new RuntimeException("cannot parse field type");
        }
        baseType = m.group(1);
        genericType = m.group(3) != null ? new FieldType(m.group(3)) : null;
        pointer = m.group(4) != null;
        genericArray = GENERIC_ARRAYS.contains(baseType);
        String countStr = m.group(6);
        fixedArray = countStr != null || FIXED_ARRAYS.contains(baseType);
        if (fixedArray) {
            fixedElementCount = ITEM_COUNTS.get(countStr);
            if (fixedElementCount == null && countStr != null) {
                fixedElementCount = Integer.valueOf(countStr);
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

    public boolean isGenericArray() {
        return genericArray;
    }

    public boolean isFixedArray() {
        return fixedArray;
    }

    public Integer getFixedElementCount() {
        return fixedElementCount;
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
        if (!omitElementCount && fixedElementCount != null) {
            sb.append('[');
            sb.append(fixedElementCount.toString());
            sb.append(']');
        }
        return sb.toString();
    }

}
