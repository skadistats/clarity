package skadistats.clarity.model;

/**
 * An inclusive range of game versions with optionally open ends. A
 * {@code null} bound is unbounded on that side.
 */
public class GameVersionRange {
    private final Integer start;
    private final Integer end;

    /** Creates a range; either bound may be {@code null}. */
    public GameVersionRange(Integer start, Integer end) {
        this.start = start;
        this.end = end;
    }

    /**
     * @return true if both bounds are {@code null}; otherwise true if
     * {@code gameVersion} is not {@code -1} (unknown) and lies within the
     * bounds, inclusive
     */
    public boolean appliesTo(int gameVersion) {
        if (start == null && end == null) return true;
        return gameVersion != -1 && (start == null || start <= gameVersion) && (end == null || end >= gameVersion);
    }
}
