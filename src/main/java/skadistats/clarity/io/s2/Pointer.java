package skadistats.clarity.io.s2;

public class Pointer {

    private final Integer typeIndex;
    private final String description;

    public Pointer(Integer typeIndex, String description) {
        this.typeIndex = typeIndex;
        this.description = description;
    }

    public Integer getTypeIndex() {
        return typeIndex;
    }

    @Override
    public String toString() {
        return description;
    }
}
