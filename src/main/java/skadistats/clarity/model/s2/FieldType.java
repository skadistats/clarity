package skadistats.clarity.model.s2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldType {

    private static final Pattern FIELD_TYPE_PATTERN = Pattern.compile("(.*?)(< (.*) >)?(\\*)?(\\[(.*?)\\])?");

    private static final Set<String> ARRAY_OVERRIDE = new HashSet<>(Arrays.asList("DOTA_PlayerChallengeInfo", "m_SpeechBubbles", "CUtlVector"));

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
        // TODO: dem hacks :(
        elementCount = ARRAY_OVERRIDE.contains(baseType) ? "XXX" : m.group(6);
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
