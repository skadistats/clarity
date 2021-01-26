package skadistats.clarity.model.state;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.S2ModifiableFieldPath;
import skadistats.clarity.model.state.NestedArrayEntityState.Entry;

import java.util.Iterator;
import java.util.NoSuchElementException;

class NestedArrayEntityStateIterator implements Iterator<FieldPath> {

    private final Entry[] entry = new Entry[6];
    private final S2ModifiableFieldPath fp = S2ModifiableFieldPath.newInstance();
    private S2FieldPath next;

    public NestedArrayEntityStateIterator(Entry rootEntry) {
        entry[0] = rootEntry;
        fp.inc(1);
        next = advance();
    }

    private S2FieldPath advance() {
        while (true) {
            int last = fp.last();
            Entry e = entry[last];
            int idx = fp.get(last);
            if (e.length() <= idx) {
                if (last == 0) {
                    return null;
                }
                fp.up(1);
                fp.inc(1);
                continue;
            }
            if (!e.has(idx)) {
                fp.inc(1);
                continue;
            }
            if (e.isSub(idx)) {
                entry[last + 1] = (Entry) e.sub(idx);
                fp.down();
                continue;
            }
            S2FieldPath result = fp.unmodifiable();
            fp.inc(1);
            return result;
        }
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    @Override
    public S2FieldPath next() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        S2FieldPath result = next;
        next = advance();
        return result;
    }

}
