package skadistats.clarity.state.s1;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.DecoderDispatch;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s1.S1FieldPath;
import skadistats.clarity.state.EntityState;
import skadistats.clarity.state.SparseStateDelta;
import skadistats.clarity.state.StateDelta;
import skadistats.clarity.state.StateMutation;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class S1ObjectArrayEntityState implements S1EntityState {

    private final Object[] state;

    public S1ObjectArrayEntityState(int length) {
        state = new Object[length];
    }

    private S1ObjectArrayEntityState(S1ObjectArrayEntityState other) {
        state = new Object[other.state.length];
        System.arraycopy(other.state, 0, state, 0, state.length);
    }

    @Override
    public Iterator<FieldPath> fieldPathIterator() {
        return new Iterator<>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < state.length;
            }

            @Override
            public FieldPath next() {
                if (i >= state.length) throw new NoSuchElementException();
                return new S1FieldPath(i++);
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValueForFieldPath(S1FieldPath fp) {
        return (T) state[fp.idx()];
    }

    @Override
    public EntityState copy() {
        return new S1ObjectArrayEntityState(this);
    }

    @Override
    public boolean write(S1FieldPath fp, Object decoded) {
        state[fp.idx()] = decoded;
        return false;
    }

    @Override
    public boolean decodeInto(S1FieldPath fp, Decoder decoder, BitStream bs) {
        state[fp.idx()] = DecoderDispatch.decode(bs, decoder);
        return false;
    }

    @Override
    public boolean applyMutation(S1FieldPath fp, StateMutation mutation) {
        var wv = (StateMutation.WriteValue) mutation;
        state[fp.idx()] = wv.value();
        return false;
    }

    @Override
    public int getInt(S1FieldPath fp) {
        Object v = state[fp.idx()];
        return v instanceof Integer i ? i : 0;
    }

    @Override
    public long getLong(S1FieldPath fp) {
        Object v = state[fp.idx()];
        return v instanceof Long l ? l : 0L;
    }

    @Override
    public float getFloat(S1FieldPath fp) {
        Object v = state[fp.idx()];
        return v instanceof Float f ? f : 0.0f;
    }

    @Override
    public Object getObject(S1FieldPath fp) {
        return state[fp.idx()];
    }

    @Override
    public StateDelta captureChanged(S1FieldPath[] fps, int num) {
        var delta = new SparseStateDelta(num, true);
        for (var i = 0; i < num; i++) {
            var fp = fps[i];
            Object v = state[fp.idx()];
            if (v == null) delta.putEmpty(i, fp);
            else delta.putObject(i, fp, v);
        }
        return delta;
    }

    @Override
    public void applyFrom(StateDelta delta, S1FieldPath fp) {
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
            applySlot(sd, i, (S1FieldPath) fp);
        }
    }

    private void applySlot(SparseStateDelta sd, int i, S1FieldPath fp) {
        Object value = switch (sd.tagAt(i)) {
            case SparseStateDelta.TAG_OBJECT -> sd.objAt(i);
            case SparseStateDelta.TAG_INT    -> Integer.valueOf((int) sd.primAt(i));
            case SparseStateDelta.TAG_LONG   -> Long.valueOf(sd.primAt(i));
            case SparseStateDelta.TAG_FLOAT  -> Float.valueOf(Float.intBitsToFloat((int) sd.primAt(i)));
            default -> null;
        };
        if (value == null) return;
        state[fp.idx()] = value;
    }

}
