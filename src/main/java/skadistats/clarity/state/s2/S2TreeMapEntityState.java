package skadistats.clarity.state.s2;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.Serializer;
import skadistats.clarity.model.s2.field.PointerField;
import skadistats.clarity.model.s2.field.SerializerField;
import skadistats.clarity.model.s2.field.VectorField;
import skadistats.clarity.state.EntityState;
import skadistats.clarity.state.StateMutation;

import java.util.Iterator;

public final class S2TreeMapEntityState extends S2EntityState {

    private final Object2ObjectAVLTreeMap<S2FieldPath, Object> state;

    public S2TreeMapEntityState(SerializerField field, int pointerCount) {
        super(field, pointerCount);
        state = new Object2ObjectAVLTreeMap<>();
    }

    private S2TreeMapEntityState(S2TreeMapEntityState other) {
        super(other);
        state = other.state.clone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<FieldPath> fieldPathIterator() {
        return (Iterator<FieldPath>) (Iterator<?>) state.keySet().iterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValueForFieldPath(S2FieldPath fp) {
        return (T) state.get(fp);
    }

    @Override
    public EntityState copy() {
        return new S2TreeMapEntityState(this);
    }

    @Override
    public boolean write(S2FieldPath fp, Object decoded) {
        return switch (getFieldForFieldPath(fp)) {
            case PointerField pf -> switchPointer(fp, pf, (Serializer) decoded);
            case VectorField vf  -> trimEntries(fp, (Integer) decoded);
            default              -> writeValue(fp, decoded);
        };
    }

    @Override
    public boolean decodeInto(S2FieldPath fp, Decoder decoder, BitStream bs) {
        throw new UnsupportedOperationException("decodeInto is implemented only on S2FlatEntityState (S2) and S1FlatEntityState (S1)");
    }

    @Override
    public boolean applyMutation(S2FieldPath fp, StateMutation mutation) {
        return switch (mutation) {
            case StateMutation.WriteValue wv    -> writeValue(fp, wv.value());
            case StateMutation.ResizeVector rv  -> trimEntries(fp, rv.count());
            case StateMutation.SwitchPointer sp -> getFieldForFieldPath(fp) instanceof PointerField pf
                    && switchPointer(fp, pf, sp.newSerializer());
        };
    }

    private boolean writeValue(S2FieldPath fp, Object value) {
        if (value != null) {
            return state.put(fp, value) == null;
        }
        return state.remove(fp) != null;
    }

    private boolean switchPointer(S2FieldPath fp, PointerField pf, Serializer newSerializer) {
        var currentSerializer = pointerSerializers[pf.getPointerId()];
        if (currentSerializer == newSerializer) return false;
        var cleared = clearSubEntries(fp);
        pointerSerializers[pf.getPointerId()] = newSerializer;
        return cleared;
    }

    private boolean trimEntries(S2FieldPath fp, int count) {
        var from = fp.childAt(count);
        var to = fp.upperBoundForSubtreeAt(fp.last());
        var sub = state.subMap(from, to);
        if (!sub.isEmpty()) {
            sub.clear();
            return true;
        }
        return false;
    }

    private boolean clearSubEntries(S2FieldPath fp) {
        var to = fp.upperBoundForSubtreeAt(fp.last());
        var sub = state.subMap(fp, to);
        if (!sub.isEmpty()) {
            sub.clear();
            return true;
        }
        return false;
    }

}
