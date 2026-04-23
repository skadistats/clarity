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
import skadistats.clarity.state.PrimitiveType;
import skadistats.clarity.state.SparseStateDelta;
import skadistats.clarity.state.StateDelta;
import skadistats.clarity.state.StateMutation;
import skadistats.clarity.util.SimpleIterator;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;

import static skadistats.clarity.state.PrimitiveType.FLOAT_VH;
import static skadistats.clarity.state.PrimitiveType.INT_VH;
import static skadistats.clarity.state.PrimitiveType.LONG_VH;

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

    @Override
    public int getInt(S1FieldPath fp) {
        var leaf = layout.leaves()[fp.idx()];
        if (leaf instanceof FieldLayout.Primitive p && p.type() == PrimitiveType.Scalar.INT) {
            var offset = p.offset();
            if (data[offset] == 0) return 0;
            return (int) INT_VH.get(data, offset + 1);
        }
        return 0;
    }

    @Override
    public long getLong(S1FieldPath fp) {
        var leaf = layout.leaves()[fp.idx()];
        if (leaf instanceof FieldLayout.Primitive p && p.type() == PrimitiveType.Scalar.LONG) {
            var offset = p.offset();
            if (data[offset] == 0) return 0L;
            return (long) LONG_VH.get(data, offset + 1);
        }
        return 0L;
    }

    @Override
    public float getFloat(S1FieldPath fp) {
        var leaf = layout.leaves()[fp.idx()];
        if (leaf instanceof FieldLayout.Primitive p && p.type() == PrimitiveType.Scalar.FLOAT) {
            var offset = p.offset();
            if (data[offset] == 0) return 0.0f;
            return (float) FLOAT_VH.get(data, offset + 1);
        }
        return 0.0f;
    }

    @Override
    public Object getObject(S1FieldPath fp) {
        return getValueForFieldPath(fp);
    }

    @Override
    public StateDelta captureChanged(S1FieldPath[] fps, int num) {
        var delta = new SparseStateDelta(num, true);
        for (var i = 0; i < num; i++) {
            captureOne(delta, i, fps[i]);
        }
        return delta;
    }

    private void captureOne(SparseStateDelta delta, int i, S1FieldPath fp) {
        var leaf = layout.leaves()[fp.idx()];
        switch (leaf) {
            case FieldLayout.Primitive p -> {
                if (data[p.offset()] == 0) {
                    delta.putEmpty(i, fp);
                    return;
                }
                var dataOff = p.offset() + 1;
                if (p.type() == PrimitiveType.Scalar.INT) {
                    delta.putInt(i, fp, (int) INT_VH.get(data, dataOff));
                } else if (p.type() == PrimitiveType.Scalar.LONG) {
                    delta.putLong(i, fp, (long) LONG_VH.get(data, dataOff));
                } else if (p.type() == PrimitiveType.Scalar.FLOAT) {
                    delta.putFloat(i, fp, (float) FLOAT_VH.get(data, dataOff));
                } else {
                    delta.putObject(i, fp, p.type().read(data, dataOff));
                }
            }
            case FieldLayout.InlineString is -> {
                if (data[is.offset()] == 0) {
                    delta.putEmpty(i, fp);
                    return;
                }
                var offset = is.offset();
                var len = (data[offset + 1] & 0xFF) | ((data[offset + 2] & 0xFF) << 8);
                delta.putObject(i, fp, new String(data, offset + 3, len, StandardCharsets.UTF_8));
            }
            case FieldLayout.Ref r -> {
                if (data[r.offset()] == 0) {
                    delta.putEmpty(i, fp);
                    return;
                }
                delta.putObject(i, fp, refs[(int) INT_VH.get(data, r.offset() + 1)]);
            }
            default -> delta.putEmpty(i, fp);
        }
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
        var tag = sd.tagAt(i);
        if (tag == SparseStateDelta.TAG_EMPTY) return;
        var leaf = layout.leaves()[fp.idx()];
        switch (leaf) {
            case FieldLayout.Primitive p -> {
                var dataOff = p.offset() + 1;
                switch (tag) {
                    case SparseStateDelta.TAG_INT -> {
                        if (p.type() == PrimitiveType.Scalar.INT) {
                            INT_VH.set(data, dataOff, (int) sd.primAt(i));
                            data[p.offset()] = 1;
                        }
                    }
                    case SparseStateDelta.TAG_LONG -> {
                        if (p.type() == PrimitiveType.Scalar.LONG) {
                            LONG_VH.set(data, dataOff, sd.primAt(i));
                            data[p.offset()] = 1;
                        }
                    }
                    case SparseStateDelta.TAG_FLOAT -> {
                        if (p.type() == PrimitiveType.Scalar.FLOAT) {
                            FLOAT_VH.set(data, dataOff, Float.intBitsToFloat((int) sd.primAt(i)));
                            data[p.offset()] = 1;
                        }
                    }
                    case SparseStateDelta.TAG_OBJECT -> {
                        var value = sd.objAt(i);
                        if (value != null) {
                            p.type().write(data, dataOff, value);
                            data[p.offset()] = 1;
                        }
                    }
                    default -> { /* unreachable */ }
                }
            }
            case FieldLayout.InlineString is -> {
                if (tag != SparseStateDelta.TAG_OBJECT) return;
                var value = sd.objAt(i);
                if (value != null) {
                    data[is.offset()] = 1;
                    writeInlineString(is.offset() + 1, (String) value, is.maxLength());
                }
            }
            case FieldLayout.Ref r -> {
                if (tag != SparseStateDelta.TAG_OBJECT) return;
                var value = sd.objAt(i);
                if (value != null) {
                    int slot;
                    if (data[r.offset()] == 0) {
                        slot = allocateRefSlot();
                        INT_VH.set(data, r.offset() + 1, slot);
                        data[r.offset()] = 1;
                    } else {
                        slot = (int) INT_VH.get(data, r.offset() + 1);
                    }
                    refs[slot] = value;
                }
            }
            default -> { /* non-leaf — skip */ }
        }
    }
}
