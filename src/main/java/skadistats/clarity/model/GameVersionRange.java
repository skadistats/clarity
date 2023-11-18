package skadistats.clarity.model;

public class GameVersionRange {
    private final Integer start;
    private final Integer end;

    public GameVersionRange(Integer start, Integer end) {
        this.start = start;
        this.end = end;
    }

    public boolean appliesTo(int gameVersion) {
        if (start == null && end == null) return true;
        return gameVersion != -1 && (start == null || start <= gameVersion) && (end == null || end >= gameVersion);
    }
}
