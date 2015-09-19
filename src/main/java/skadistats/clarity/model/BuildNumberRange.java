package skadistats.clarity.model;

public class BuildNumberRange {
    private final Integer start;
    private final Integer end;

    public BuildNumberRange(Integer start, Integer end) {
        this.start = start;
        this.end = end;
    }

    public boolean appliesTo(int buildNumber) {
        if (start == null && end == null) return true;
        return buildNumber != -1 && (start == null || start <= buildNumber) && (end == null || end >= buildNumber);
    }
}
