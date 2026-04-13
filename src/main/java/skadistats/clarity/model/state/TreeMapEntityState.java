package skadistats.clarity.model.state;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import skadistats.clarity.io.s2.field.PointerField;
import skadistats.clarity.io.s2.field.SerializerField;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2LongFieldPath;
import skadistats.clarity.model.s2.S2LongFieldPathFormat;

import java.util.Iterator;

public class TreeMapEntityState extends AbstractS2EntityState {

    private final Object2ObjectAVLTreeMap<S2LongFieldPath, Object> state;

    public TreeMapEntityState(SerializerField field, int pointerCount) {
        super(field, pointerCount);
        state = new Object2ObjectAVLTreeMap<>();
    }

    private TreeMapEntityState(TreeMapEntityState other) {
        super(other);
        state = other.state.clone();
    }

    @Override
    public EntityState copy() {
        return new TreeMapEntityState(this);
    }

    @Override
    public boolean applyMutation(FieldPath fpX, StateMutation mutation) {
        var fp = (S2LongFieldPath) fpX;
        if (mutation instanceof StateMutation.WriteValue wv) {
            var value = wv.value();
            if (value != null) {
                return state.put(fp, value) == null;
            } else {
                return state.remove(fp) != null;
            }
        } else if (mutation instanceof StateMutation.ResizeVector rv) {
            return trimEntries(fp, rv.count());
        } else if (mutation instanceof StateMutation.SwitchPointer sp) {
            var cleared = clearSubEntries(fp);
            var field = getFieldForFieldPath(fp);
            if (field instanceof PointerField pf) {
                pointerSerializers[pf.getPointerId()] = sp.newSerializer();
            }
            return cleared;
        }
        throw new IllegalStateException();
    }

    private boolean trimEntries(S2LongFieldPath fp, int count) {
        var id = fp.id();
        var depth = S2LongFieldPathFormat.last(id) + 1;
        var base = S2LongFieldPathFormat.down(id);

        var from = new S2LongFieldPath(S2LongFieldPathFormat.set(base, depth, count));
        var upperBound = nextBound(base, depth - 1, S2LongFieldPathFormat.get(id, depth - 1));
        var to = new S2LongFieldPath(upperBound);
        var sub = state.subMap(from, to);
        if (!sub.isEmpty()) {
            sub.clear();
            return true;
        }
        return false;
    }

    private boolean clearSubEntries(S2LongFieldPath fp) {
        var id = fp.id();
        var depth = S2LongFieldPathFormat.last(id);
        var idx = S2LongFieldPathFormat.get(id, depth);

        long parentBase;
        if (depth == 0) {
            parentBase = 0L;
        } else {
            parentBase = id;
            for (var d = depth; d <= S2LongFieldPathFormat.last(id); d++) {
                parentBase = S2LongFieldPathFormat.set(parentBase, d, 0);
            }
        }

        var upperBound = nextBound(parentBase, depth, idx);
        var to = new S2LongFieldPath(upperBound);
        var sub = state.subMap(fp, to);
        if (!sub.isEmpty()) {
            sub.clear();
            return true;
        }
        return false;
    }

    private long nextBound(long base, int depth, int idx) {
        if (idx < S2LongFieldPathFormat.maxIndexAtDepth(depth)) {
            return S2LongFieldPathFormat.set(base, depth, idx + 1);
        }
        return Long.MAX_VALUE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValueForFieldPath(FieldPath fp) {
        return (T) state.get((S2LongFieldPath) fp);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<FieldPath> fieldPathIterator() {
        return (Iterator<FieldPath>) (Iterator<?>) state.keySet().iterator();
    }

}
