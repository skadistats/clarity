package clarity.model;

public enum PropFlag {

    UNSIGNED(1 << 0),
    COORD(1 << 1),
    NO_SCALE(1 << 2),
    ROUND_DOWN(1 << 3),
    ROUND_UP(1 << 4),
    NORMAL(1 << 5),
    EXCLUDE(1 << 6),
    XYZE(1 << 7),
    INSIDE_ARRAY(1 << 8),
    PROXY_ALWAYS(1 << 9),
    VECTOR_ELEM(1 << 10),
    COLLAPSIBLE(1 << 11),
    COORD_MP(1 << 12),
    COORD_MP_LOW_PRECISION(1 << 13),
    COORD_MP_INTEGRAL(1 << 14),
    CELL_COORD(1 << 15),
    CELL_COORD_LOW_PRECISION(1 << 16),
    CELL_COORD_INTEGRAL(1 << 17),
    CHANGES_OFTEN(1 << 18),
    ENCODED_AGAINST_TICKCOUNT(1 << 19);

    private final int flag;

    private PropFlag(int flag) {
        this.flag = flag;
    }

    public int getFlag() {
        return flag;
    }

}