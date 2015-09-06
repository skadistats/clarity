package skadistats.clarity.model;

public class BuildNumberRange {
    private final Integer start;
    private final Integer end;

    public BuildNumberRange(Integer start, Integer end) {
        this.start = start;
        this.end = end;
    }

    public boolean appliesTo(int buildNumber) {
        return (start == null || start <= buildNumber) && (end == null || end >= buildNumber);
    }
}
