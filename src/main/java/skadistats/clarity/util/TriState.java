package skadistats.clarity.util;

public enum TriState {

    UNSET, YES, NO;

    public static TriState fromBoolean(Boolean b) {
        return b == null ? UNSET : (b ? YES : NO);
    }

}
