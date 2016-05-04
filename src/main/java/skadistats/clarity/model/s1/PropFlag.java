package skadistats.clarity.model.s1;

public class PropFlag {

    public static final int UNSIGNED = 1 << 0;
    public static final int COORD = 1 << 1;
    public static final int NO_SCALE = 1 << 2;
    public static final int ROUND_DOWN = 1 << 3;
    public static final int ROUND_UP = 1 << 4;
    public static final int NORMAL = 1 << 5;
    public static final int EXCLUDE = 1 << 6;
    public static final int XYZE = 1 << 7;
    public static final int INSIDE_ARRAY = 1 << 8;
    public static final int PROXY_ALWAYS = 1 << 9;
    public static final int VECTOR_ELEM = 1 << 10;
    public static final int COLLAPSIBLE = 1 << 11;
    public static final int COORD_MP = 1 << 12;
    public static final int COORD_MP_LOW_PRECISION = 1 << 13;
    public static final int COORD_MP_INTEGRAL = 1 << 14;
    public static final int CELL_COORD = 1 << 15;
    public static final int CELL_COORD_LOW_PRECISION = 1 << 16;
    public static final int CELL_COORD_INTEGRAL = 1 << 17;
    public static final int CHANGES_OFTEN = 1 << 18;
    public static final int ENCODED_AS_VARINT = 1 << 19;

    private static final String[] NAMES = {
        "UNSIGNED",
        "COORD",
        "NO_SCALE",
        "ROUND_DOWN",
        "ROUND_UP",
        "NORMAL",
        "EXCLUDE",
        "XYZE",
        "INSIDE_ARRAY",
        "PROXY_ALWAYS",
        "VECTOR_ELEM",
        "COLLAPSIBLE",
        "COORD_MP",
        "COORD_MP_LOW_PRECISION",
        "COORD_MP_INTEGRAL",
        "CELL_COORD",
        "CELL_COORD_LOW_PRECISION",
        "CELL_COORD_INTEGRAL",
        "CHANGES_OFTEN",
        "ENCODED_AS_VARINT"
    };

    public static String descriptionForFlags(int flags) {
        StringBuilder buf = new StringBuilder();
        for (String name : NAMES) {
            if ((flags & 1) != 0) {
                if (buf.length() > 0) {
                    buf.append(", ");
                }
                buf.append(name);
            }
            flags = flags >> 1;
        }
        return buf.toString();
    }

}