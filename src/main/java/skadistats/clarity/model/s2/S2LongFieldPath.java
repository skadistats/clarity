package skadistats.clarity.model.s2;

public class S2LongFieldPath implements S2FieldPath<S2LongFieldPath> {

    private final long id;

    S2LongFieldPath(long id) {
        this.id = id;
    }

    @Override
    public int get(int i) {
        return S2LongFieldPathFormat.get(id, i);
    }

    @Override
    public int last() {
        return S2LongFieldPathFormat.last(id);
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof S2LongFieldPath) {
            return id == ((S2LongFieldPath) o).id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return S2LongFieldPathFormat.hashCode(id);
    }

    @Override
    public int compareTo(S2LongFieldPath o) {
        return S2LongFieldPathFormat.compareTo(id, o.id);
    }

}
