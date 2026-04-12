package skadistats.clarity.model.state;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import skadistats.clarity.io.s2.field.SerializerField;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2LongFieldPath;
import skadistats.clarity.model.s2.S2LongFieldPathFormat;

import java.util.Iterator;

public class TreeMapEntityState extends S2EntityState {

    private final Object2ObjectAVLTreeMap<S2LongFieldPath, Object> state;

    public TreeMapEntityState(SerializerField field) {
        super(field);
        state = new Object2ObjectAVLTreeMap<>();
    }

    private TreeMapEntityState(TreeMapEntityState other) {
        super(other.rootField);
        state = other.state.clone();
    }

    @Override
    public EntityState copy() {
        return new TreeMapEntityState(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<FieldPath> fieldPathIterator() {
        return (Iterator<FieldPath>) (Iterator<?>) state.keySet().iterator();
    }

    // --- NestedEntityState (root delegates to root View) ---

    private View rootView() {
        return new View(0L, 0, Long.MAX_VALUE);
    }

    @Override
    public int length() {
        return rootView().length();
    }

    @Override
    public boolean has(int idx) {
        return rootView().has(idx);
    }

    @Override
    public Object get(int idx) {
        return rootView().get(idx);
    }

    @Override
    public void set(int idx, Object value) {
        rootView().set(idx, value);
    }

    @Override
    public void clear(int idx) {
        rootView().clear(idx);
    }

    @Override
    public boolean isSub(int idx) {
        return rootView().isSub(idx);
    }

    @Override
    public NestedEntityState sub(int idx) {
        return rootView().sub(idx);
    }

    @Override
    public NestedEntityState capacity(int wantedSize, boolean shrinkIfNeeded) {
        return rootView().capacity(wantedSize, shrinkIfNeeded);
    }

    // --- Inner View class ---

    private class View implements NestedEntityState {

        private final long base;
        private final int depth;
        private final long upperBound;

        private View(long base, int depth, long upperBound) {
            this.base = base;
            this.depth = depth;
            this.upperBound = upperBound;
        }

        private S2LongFieldPath childKey(int idx) {
            return new S2LongFieldPath(S2LongFieldPathFormat.set(base, depth, idx));
        }

        private S2LongFieldPath lowerBound() {
            return childKey(0);
        }

        private long nextBound(int idx) {
            if (idx < S2LongFieldPathFormat.maxIndexAtDepth(depth)) {
                return S2LongFieldPathFormat.set(base, depth, idx + 1);
            }
            return upperBound;
        }

        @Override
        public int length() {
            var from = lowerBound();
            var to = new S2LongFieldPath(upperBound);
            var sub = state.subMap(from, to);
            if (sub.isEmpty()) {
                return 0;
            }
            return S2LongFieldPathFormat.get(sub.lastKey().id(), depth) + 1;
        }

        @Override
        public boolean has(int idx) {
            var from = childKey(idx);
            var to = new S2LongFieldPath(nextBound(idx));
            return !state.subMap(from, to).isEmpty();
        }

        @Override
        public Object get(int idx) {
            return state.get(childKey(idx));
        }

        @Override
        public void set(int idx, Object value) {
            var key = childKey(idx);
            if (value != null) {
                if (state.put(key, value) == null) {
                    capacityChanged = true;
                }
            } else {
                if (state.remove(key) != null) {
                    capacityChanged = true;
                }
            }
        }

        @Override
        public void clear(int idx) {
            var from = childKey(idx);
            var to = new S2LongFieldPath(nextBound(idx));
            var sub = state.subMap(from, to);
            if (!sub.isEmpty()) {
                capacityChanged = true;
                sub.clear();
            }
        }

        @Override
        public boolean isSub(int idx) {
            if (!has(idx)) {
                return false;
            }
            // has(idx) is true — check if there are keys deeper than the exact child key
            var exactKey = childKey(idx);
            var to = new S2LongFieldPath(nextBound(idx));
            var sub = state.subMap(exactKey, to);
            // if size > 1, there are deeper keys; if size == 1, it might be just the leaf
            // or if the single key IS deeper than exactKey
            if (sub.size() > 1) {
                return true;
            }
            if (sub.size() == 1) {
                var onlyKey = sub.firstKey();
                return !onlyKey.equals(exactKey);
            }
            return false;
        }

        @Override
        public NestedEntityState sub(int idx) {
            var childLong = S2LongFieldPathFormat.set(base, depth, idx);
            var newBase = S2LongFieldPathFormat.down(childLong);
            return new View(newBase, depth + 1, nextBound(idx));
        }

        @Override
        public NestedEntityState capacity(int wantedSize, boolean shrinkIfNeeded) {
            if (!shrinkIfNeeded) {
                return this;
            }
            // remove all entries with child index >= wantedSize
            if (wantedSize == 0) {
                var from = lowerBound();
                var to = new S2LongFieldPath(upperBound);
                var sub = state.subMap(from, to);
                if (!sub.isEmpty()) {
                    capacityChanged = true;
                    sub.clear();
                }
            } else {
                var from = childKey(wantedSize);
                var to = new S2LongFieldPath(upperBound);
                var sub = state.subMap(from, to);
                if (!sub.isEmpty()) {
                    capacityChanged = true;
                    sub.clear();
                }
            }
            return this;
        }

    }

}
