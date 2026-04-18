package skadistats.clarity.model.s2;

public final class S2LongFieldPath implements S2FieldPath {

    private final long id;

    public S2LongFieldPath(long id) {
        this.id = id;
    }

    public long id() {
        return id;
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
    public S2FieldPath childAt(int index) {
        var extended = S2LongFieldPathFormat.down(id);
        var depth = S2LongFieldPathFormat.last(extended);
        return new S2LongFieldPath(S2LongFieldPathFormat.set(extended, depth, index));
    }

    @Override
    public S2FieldPath upperBoundForSubtreeAt(int depth) {
        var idx = S2LongFieldPathFormat.get(id, depth);
        if (idx >= S2LongFieldPathFormat.maxIndexAtDepth(depth)) {
            return new S2LongFieldPath(Long.MAX_VALUE);
        }
        var last = S2LongFieldPathFormat.last(id);
        var truncated = last > depth ? S2LongFieldPathFormat.up(id, last - depth) : id;
        return new S2LongFieldPath(S2LongFieldPathFormat.set(truncated, depth, idx + 1));
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof S2LongFieldPath other && id == other.id;
    }

    @Override
    public int hashCode() {
        return S2LongFieldPathFormat.hashCode(id);
    }

    @Override
    public int compareTo(S2FieldPath o) {
        return S2LongFieldPathFormat.compareTo(id, ((S2LongFieldPath) o).id);
    }

}
