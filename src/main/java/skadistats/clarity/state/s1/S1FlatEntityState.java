package skadistats.clarity.state.s1;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.DecoderDispatch;
import skadistats.clarity.io.decoder.StringLenDecoder;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s1.S1DTClass;
import skadistats.clarity.model.s1.S1FieldPath;
import skadistats.clarity.state.EntityState;
import skadistats.clarity.state.FieldLayout;
import skadistats.clarity.state.StateMutation;
import skadistats.clarity.util.SimpleIterator;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;

import static skadistats.clarity.state.PrimitiveType.INT_VH;

public final class S1FlatEntityState implements S1EntityState {

    private static final Object[] EMPTY_REFS = {};
    private static final int[] EMPTY_FREE_SLOTS = {};

    private final S1FlatLayout layout;
    private byte[] data;
    private Object[] refs;
    private int refsSize;
    private int[] freeSlots;
    private int freeSlotsTop;

    public S1FlatEntityState(S1DTClass dtClass) {
        this.layout = dtClass.getFlatLayout();
        this.data = new byte[layout.dataBytes()];
        if (layout.refSlots() > 0) {
            this.refs = new Object[Math.max(4, layout.refSlots())];
            this.freeSlots = new int[refs.length];
        } else {
            this.refs = EMPTY_REFS;
            this.freeSlots = EMPTY_FREE_SLOTS;
        }
    }

    private S1FlatEntityState(S1FlatEntityState other) {
        this.layout = other.layout;
        this.data = Arrays.copyOf(other.data, other.data.length);
        this.refs = other.refs.length == 0 ? EMPTY_REFS : Arrays.copyOf(other.refs, other.refs.length);
        this.refsSize = other.refsSize;
        this.freeSlots = other.freeSlots.length == 0 ? EMPTY_FREE_SLOTS : Arrays.copyOf(other.freeSlots, other.freeSlots.length);
        this.freeSlotsTop = other.freeSlotsTop;
    }

    @Override
    public Iterator<FieldPath> fieldPathIterator() {
        var n = layout.leaves().length;
        return new SimpleIterator<>() {
            int i = 0;

            @Override
            public FieldPath readNext() {
                return i < n ? new S1FieldPath(i++) : null;
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValueForFieldPath(S1FieldPath fpX) {
        var leaf = layout.leaves()[fpX.idx()];
        return (T) switch (leaf) {
            case FieldLayout.Primitive p -> {
                var offset = p.offset();
                if (data[offset] == 0) yield null;
                yield p.type().read(data, offset + 1);
            }
            case FieldLayout.InlineString is -> {
                var offset = is.offset();
                if (data[offset] == 0) yield null;
                var len = (data[offset + 1] & 0xFF) | ((data[offset + 2] & 0xFF) << 8);
                yield new String(data, offset + 3, len, StandardCharsets.UTF_8);
            }
            case FieldLayout.Ref r -> {
                var offset = r.offset();
                if (data[offset] == 0) yield null;
                yield refs[(int) INT_VH.get(data, offset + 1)];
            }
            default -> throw new IllegalStateException("S1 layout produced non-leaf: " + leaf);
        };
    }

    @Override
    public EntityState copy() {
        return new S1FlatEntityState(this);
    }

    @Override
    public boolean write(S1FieldPath fpX, Object decoded) {
        var leaf = layout.leaves()[fpX.idx()];
        switch (leaf) {
            case FieldLayout.Primitive p -> {
                var offset = p.offset();
                data[offset] = 1;
                p.type().write(data, offset + 1, decoded);
            }
            case FieldLayout.InlineString is -> {
                var offset = is.offset();
                data[offset] = 1;
                writeInlineString(offset + 1, (String) decoded, is.maxLength());
            }
            case FieldLayout.Ref r -> {
                var offset = r.offset();
                int slot;
                if (data[offset] == 0) {
                    slot = allocateRefSlot();
                    INT_VH.set(data, offset + 1, slot);
                    data[offset] = 1;
                } else {
                    slot = (int) INT_VH.get(data, offset + 1);
                }
                refs[slot] = decoded;
            }
            default -> throw new IllegalStateException("S1 layout produced non-leaf: " + leaf);
        }
        return false;
    }

    @Override
    public boolean decodeInto(S1FieldPath fpX, Decoder decoder, BitStream bs) {
        var leaf = layout.leaves()[fpX.idx()];
        switch (leaf) {
            case FieldLayout.Primitive p -> {
                var offset = p.offset();
                data[offset] = 1;
                DecoderDispatch.decodeInto(bs, decoder, data, offset + 1);
            }
            case FieldLayout.InlineString is -> {
                var offset = is.offset();
                data[offset] = 1;
                StringLenDecoder.decodeIntoInline(bs, data, offset + 1, is.maxLength());
            }
            case FieldLayout.Ref r -> throw new IllegalStateException("decodeInto called on REF leaf, idx=" + fpX.idx());
            default -> throw new IllegalStateException("S1 layout produced non-leaf: " + leaf);
        }
        return false;
    }

    @Override
    public boolean applyMutation(S1FieldPath fp, StateMutation mutation) {
        var wv = (StateMutation.WriteValue) mutation;
        return write(fp, wv.value());
    }

    private void writeInlineString(int offset, String value, int maxLength) {
        var bytes = value.getBytes(StandardCharsets.UTF_8);
        var len = Math.min(bytes.length, maxLength);
        data[offset] = (byte) (len & 0xFF);
        data[offset + 1] = (byte) ((len >>> 8) & 0xFF);
        System.arraycopy(bytes, 0, data, offset + 2, len);
    }

    private int allocateRefSlot() {
        if (freeSlotsTop > 0) {
            return freeSlots[--freeSlotsTop];
        }
        if (refsSize == refs.length) {
            var newCap = refs.length == 0 ? 4 : refs.length * 2;
            refs = Arrays.copyOf(refs, newCap);
            freeSlots = Arrays.copyOf(freeSlots, newCap);
        }
        return refsSize++;
    }
}
