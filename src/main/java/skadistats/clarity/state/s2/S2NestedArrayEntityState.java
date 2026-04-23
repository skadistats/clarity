package skadistats.clarity.state.s2;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.Field;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.Serializer;
import skadistats.clarity.model.s2.field.ArrayField;
import skadistats.clarity.model.s2.field.FixedPointerField;
import skadistats.clarity.model.s2.field.PolymorphicPointerField;
import skadistats.clarity.model.s2.field.SerializerField;
import skadistats.clarity.model.s2.field.VectorField;
import skadistats.clarity.state.EntityState;
import skadistats.clarity.state.SparseStateDelta;
import skadistats.clarity.state.StateDelta;
import skadistats.clarity.state.StateMutation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

public final class S2NestedArrayEntityState extends S2EntityState {

    private List<Entry> entries;
    private Deque<Integer> freeEntries;
    private boolean capacityChanged;

    public S2NestedArrayEntityState(SerializerField field, int pointerCount) {
        super(field, pointerCount);
        entries = new ArrayList<>(20);
        entries.add(new Entry());
        // freeEntries is lazy-allocated when the first slot is freed.
    }

    private S2NestedArrayEntityState(S2NestedArrayEntityState other) {
        super(other);
        var size = other.entries.size();
        entries = new ArrayList<>(size);
        for (var e : other.entries) {
            entries.add(e == null ? null : new Entry(e.state.length == 0 ? EMPTY_STATE : e.state.clone()));
        }
        freeEntries = other.freeEntries == null || other.freeEntries.isEmpty()
            ? null
            : new ArrayDeque<>(other.freeEntries);
    }

    private Entry rootEntry() {
        return entries.get(0);
    }

    @Override
    public Iterator<FieldPath> fieldPathIterator() {
        return new S2NestedArrayEntityStateIterator(rootEntry());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValueForFieldPath(S2FieldPath fp) {
        Field field = rootField;
        Entry node = rootEntry();
        var last = fp.last();

        var i = 0;
        while (true) {
            var idx = fp.get(i);
            if (i == last) {
                // Hidden composite terminals store a sub-entry EntryRef at
                // node.get(idx), not the originally-decoded value. Return
                // what `write` would accept at the same fp, so that
                // write(fp, get(fp)) is a semantic no-op — this is the
                // contract `captureChanged`/`applyFrom` relies on.
                var child = field.getChild(this, idx);
                return switch (child) {
                    case VectorField vf -> (T) (Integer) (node.isSub(idx) ? node.subEntry(idx).length() : 0);
                    case FixedPointerField fpf -> (T) (node.isSub(idx) ? fpf.getSerializer() : null);
                    case PolymorphicPointerField ppf -> (T) (node.isSub(idx) ? pointerSerializers[ppf.getPointerId()] : null);
                    default -> (T) node.get(idx);
                };
            }
            field = field.getChild(this, idx);
            if (!node.isSub(idx)) {
                return null;
            }
            node = node.subEntry(idx);
            i++;
        }
    }

    @Override
    public EntityState copy() {
        return new S2NestedArrayEntityState(this);
    }

    @Override
    public boolean write(S2FieldPath fp, Object decoded) {
        Field field = rootField;
        Entry node = rootEntry();
        var last = fp.last();
        capacityChanged = false;

        var i = 0;
        while (true) {
            var idx = fp.get(i);
            if (node.length() <= idx) {
                ensureNodeCapacity(field, node, idx);
            }
            var child = field.getChild(this, idx);
            if (i == last) {
                return switch (child) {
                    case PolymorphicPointerField ppf -> handlePolymorphicPointerSwitch(node, idx, ppf.getPointerId(), (Serializer) decoded);
                    case FixedPointerField fpf       -> handleFixedPointerSwitch(node, idx, (Serializer) decoded);
                    case VectorField vf              -> handleResizeVector(node, idx, (Integer) decoded);
                    default -> {
                        node.set(idx, decoded);
                        yield capacityChanged;
                    }
                };
            }
            field = child;
            node = subEntryForWrite(node, idx);
            i++;
        }
    }

    @Override
    public boolean decodeInto(S2FieldPath fp, Decoder decoder, BitStream bs) {
        throw new UnsupportedOperationException("decodeInto is implemented only on S2FlatEntityState (S2) and S1FlatEntityState (S1)");
    }

    @Override
    public boolean applyMutation(S2FieldPath fp, StateMutation mutation) {
        Field field = rootField;
        Entry node = rootEntry();
        var last = fp.last();
        capacityChanged = false;

        var i = 0;
        while (true) {
            var idx = fp.get(i);
            if (node.length() <= idx) {
                ensureNodeCapacity(field, node, idx);
            }
            var child = field.getChild(this, idx);
            if (i == last) {
                return switch (mutation) {
                    case StateMutation.WriteValue wv -> {
                        node.set(idx, wv.value());
                        yield capacityChanged;
                    }
                    case StateMutation.ResizeVector rv -> handleResizeVector(node, idx, rv.count());
                    case StateMutation.SwitchPolymorphicPointer sp -> handlePolymorphicPointerSwitch(node, idx, sp.pointerId(), sp.newSerializer());
                    case StateMutation.SwitchFixedPointer sfp -> handleFixedPointerSwitch(node, idx, sfp.serializer());
                };
            }
            field = child;
            node = subEntryForWrite(node, idx);
            i++;
        }
    }

    private Entry subEntryForWrite(Entry parent, int idx) {
        if (!parent.isSub(idx)) {
            var fresh = new Entry();
            var ref = createEntryRef(fresh);
            parent.set(idx, ref);
            return fresh;
        }
        var entryRef = (EntryRef) parent.get(idx);
        return entries.get(entryRef.idx);
    }

    private void ensureNodeCapacity(Field parentField, Entry node, int idx) {
        switch (parentField) {
            case SerializerField sf     -> node.capacity(sf.getSerializer().getFieldCount(), false);
            case ArrayField af          -> node.capacity(af.getLength(), false);
            case FixedPointerField fpf  -> node.capacity(fpf.getSerializer().getFieldCount(), false);
            default                     -> node.capacity(idx + 1, false);
        }
    }

    private boolean handlePolymorphicPointerSwitch(Entry node, int idx, int pointerId, Serializer newSerializer) {
        var currentSerializer = pointerSerializers[pointerId];
        if (currentSerializer == newSerializer) return false;
        var removedOccupied = false;
        if (node.has(idx)) {
            removedOccupied = hasAnyOccupiedPath(subEntryForWrite(node, idx));
            pointerSerializers[pointerId] = null;
            node.clear(idx);
        }
        if (newSerializer != null) {
            pointerSerializers[pointerId] = newSerializer;
            subEntryForWrite(node, idx);
        }
        return removedOccupied;
    }

    private boolean handleFixedPointerSwitch(Entry node, int idx, Serializer newSerializer) {
        if (newSerializer != null) {
            subEntryForWrite(node, idx);
            return false;
        }
        if (!node.has(idx)) return false;
        var removed = hasAnyOccupiedPath(subEntryForWrite(node, idx));
        node.clear(idx);
        return removed;
    }

    private boolean handleResizeVector(Entry node, int idx, int newCount) {
        var oldCount = node.isSub(idx) ? node.subEntry(idx).length() : 0;
        if (oldCount == newCount) return false;
        var droppedOccupied = false;
        if (newCount < oldCount) {
            var sub = node.subEntry(idx);
            for (var i = newCount; i < oldCount && !droppedOccupied; i++) {
                if (hasOccupiedSlot(sub, i)) droppedOccupied = true;
            }
        }
        subEntryForWrite(node, idx).capacity(newCount, true);
        return droppedOccupied;
    }

    private boolean hasAnyOccupiedPath(Entry entry) {
        for (var i = 0; i < entry.length(); i++) {
            if (hasOccupiedSlot(entry, i)) return true;
        }
        return false;
    }

    private boolean hasOccupiedSlot(Entry entry, int i) {
        if (!entry.has(i)) return false;
        var v = entry.get(i);
        if (v instanceof EntryRef ref) {
            var sub = entries.get(ref.idx);
            return sub != null && hasAnyOccupiedPath(sub);
        }
        return true;
    }

    private EntryRef createEntryRef(Entry entry) {
        int i;
        if (freeEntries == null || freeEntries.isEmpty()) {
            i = entries.size();
            entries.add(entry);
        } else {
            i = freeEntries.removeFirst();
            entries.set(i, entry);
        }
        return new EntryRef(i);
    }

    private void clearEntryRef(EntryRef entryRef) {
        entries.set(entryRef.idx, null);
        ensureFreeEntries().add(entryRef.idx);
    }

    private Deque<Integer> ensureFreeEntries() {
        if (freeEntries == null) freeEntries = new ArrayDeque<>();
        return freeEntries;
    }

    private void releaseEntryRef(EntryRef entryRef) {
        var e = entries.get(entryRef.idx);
        if (e != null) {
            for (var slot : e.state) {
                if (slot instanceof EntryRef child) {
                    releaseEntryRef(child);
                }
            }
        }
        clearEntryRef(entryRef);
    }

    public int slabSize() {
        return entries.size();
    }

    public int freeSlotCount() {
        return freeEntries == null ? 0 : freeEntries.size();
    }

    @Override
    public int getInt(S2FieldPath fp) {
        Object v = getValueForFieldPath(fp);
        return v instanceof Integer i ? i : 0;
    }

    @Override
    public long getLong(S2FieldPath fp) {
        Object v = getValueForFieldPath(fp);
        return v instanceof Long l ? l : 0L;
    }

    @Override
    public float getFloat(S2FieldPath fp) {
        Object v = getValueForFieldPath(fp);
        return v instanceof Float f ? f : 0.0f;
    }

    @Override
    public Object getObject(S2FieldPath fp) {
        return getValueForFieldPath(fp);
    }

    @Override
    public StateDelta captureChanged(S2FieldPath[] fps, int num) {
        var delta = new SparseStateDelta(num, true);
        for (var i = 0; i < num; i++) {
            var fp = fps[i];
            Object v = getValueForFieldPath(fp);
            if (v == null) delta.putEmpty(i, fp);
            else delta.putObject(i, fp, v);
        }
        return delta;
    }

    @Override
    public void applyFrom(StateDelta delta, S2FieldPath fp) {
        if (!(delta instanceof SparseStateDelta sd)) {
            throw new IllegalArgumentException("applyFrom requires a SparseStateDelta");
        }
        var i = sd.indexOf(fp);
        if (i < 0) return;
        applySlot(sd, i, fp);
    }

    @Override
    public void applyAll(StateDelta delta) {
        if (!(delta instanceof SparseStateDelta sd)) {
            throw new IllegalArgumentException("applyAll requires a SparseStateDelta");
        }
        var fields = sd.fields();
        for (var i = 0; i < fields.length; i++) {
            var fp = fields[i];
            if (fp == null) continue;
            applySlot(sd, i, (S2FieldPath) fp);
        }
    }

    private void applySlot(SparseStateDelta sd, int i, S2FieldPath fp) {
        // TAG_EMPTY represents "field was unset at capture time" — on the apply
        // target this means clear. write() dispatches by terminal field type:
        // leaf ValueField → node.set(idx, null); FixedPointer/PolymorphicPointer
        // → switch(null); VectorField never receives null here because
        // getValueForFieldPath returns Integer(0) for unset vectors, keeping
        // the captured tag non-empty.
        Object value = sd.tagAt(i) == SparseStateDelta.TAG_EMPTY ? null : deltaSlotAsObject(sd, i);
        write(fp, value);
    }

    private static Object deltaSlotAsObject(SparseStateDelta sd, int i) {
        return switch (sd.tagAt(i)) {
            case SparseStateDelta.TAG_OBJECT -> sd.objAt(i);
            case SparseStateDelta.TAG_INT    -> Integer.valueOf((int) sd.primAt(i));
            case SparseStateDelta.TAG_LONG   -> Long.valueOf(sd.primAt(i));
            case SparseStateDelta.TAG_FLOAT  -> Float.valueOf(Float.intBitsToFloat((int) sd.primAt(i)));
            default -> null;
        };
    }


    private static class EntryRef {

        private final int idx;

        private EntryRef(int idx) {
            this.idx = idx;
        }

        @Override
        public String toString() {
            return "EntryRef[" + idx + "]";
        }
    }

    private static final Object[] EMPTY_STATE = {};

    public class Entry implements S2NestedEntityState {

        private Object[] state;

        private Entry() {
            this(EMPTY_STATE);
        }

        private Entry(Object[] state) {
            this.state = state;
        }

        @Override
        public int length() {
            return state.length;
        }

        @Override
        public boolean has(int idx) {
            return state.length > idx && state[idx] != null;
        }

        @Override
        public Object get(int idx) {
            return state.length > idx ? state[idx] : null;
        }

        @Override
        public void set(int idx, Object value) {
            if (state[idx] instanceof EntryRef ref) {
                releaseEntryRef(ref);
            }
            if ((state[idx] == null) ^ (value == null)) {
                capacityChanged = true;
            }
            state[idx] = value;
        }

        @Override
        public void clear(int idx) {
            set(idx, null);
        }

        @Override
        public boolean isSub(int idx) {
            return has(idx) && get(idx) instanceof EntryRef;
        }

        @Override
        public S2NestedEntityState sub(int idx) {
            return subEntry(idx);
        }

        Entry subEntry(int idx) {
            if (!isSub(idx)) {
                set(idx, createEntryRef(new Entry()));
            }
            var entryRef = (EntryRef) get(idx);
            return entries.get(entryRef.idx);
        }

        @Override
        public S2NestedEntityState capacity(int wantedSize, boolean shrinkIfNeeded) {
            var curSize = state.length;
            if (wantedSize == curSize) {
                return this;
            }

            Object[] newState = null;
            if (wantedSize > curSize) {
                newState = new Object[wantedSize];
            } else if (shrinkIfNeeded) {
                for (var i = wantedSize; i < curSize; i++) {
                    if (state[i] instanceof EntryRef ref) {
                        releaseEntryRef(ref);
                    }
                }
                newState = wantedSize == 0 ? EMPTY_STATE : new Object[wantedSize];
            }

            if (newState != null) {
                System.arraycopy(state, 0, newState, 0, Math.min(curSize, wantedSize));
                state = newState;
                capacityChanged = true;
            }
            return this;
        }

        @Override
        public String toString() {
            return "Entry[size=" + state.length + "]";
        }

    }

}
