package skadistats.clarity.model.s2;

public final class S2LongFieldPathBuilder implements S2FieldPathBuilder {

    private long id;

    @Override
    public void set(int i, int v) {
        id = S2LongFieldPathFormat.set(id, i, v);
    }

    @Override
    public int get(int i) {
        return S2LongFieldPathFormat.get(id, i);
    }

    @Override
    public void down() {
        id = S2LongFieldPathFormat.down(id);
    }

    @Override
    public void up(int n) {
        id = S2LongFieldPathFormat.up(id, n);
    }

    @Override
    public int last() {
        return S2LongFieldPathFormat.last(id);
    }

    @Override
    public void inc(int i, int n) {
        id = S2LongFieldPathFormat.set(id, i, S2LongFieldPathFormat.get(id, i) + n);
    }

    @Override
    public void inc(int n) {
        var i = S2LongFieldPathFormat.last(id);
        id = S2LongFieldPathFormat.set(id, i, S2LongFieldPathFormat.get(id, i) + n);
    }

    @Override
    public void cur(int v) {
        id = S2LongFieldPathFormat.set(id, S2LongFieldPathFormat.last(id), v);
    }

    @Override
    public int cur() {
        return S2LongFieldPathFormat.get(id, S2LongFieldPathFormat.last(id));
    }

    @Override
    public S2FieldPath snapshot() {
        return new S2LongFieldPath(id);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        for (var i = 0; i <= last(); i++) {
            if (i != 0) sb.append('/');
            sb.append(get(i));
        }
        return sb.toString();
    }

}
