package skadistats.clarity.model.state;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.DecoderDispatch;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s1.S1FieldPath;
import skadistats.clarity.util.SimpleIterator;

import java.util.Iterator;

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
        return new SimpleIterator<>() {
            int i = 0;

            @Override
            public FieldPath readNext() {
                return i < state.length ? new S1FieldPath(i++) : null;
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

}
