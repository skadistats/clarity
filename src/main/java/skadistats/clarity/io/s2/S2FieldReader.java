package skadistats.clarity.io.s2;

import skadistats.clarity.ClarityException;
import skadistats.clarity.io.FieldChanges;
import skadistats.clarity.io.FieldReader;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.DecoderDispatch;
import skadistats.clarity.io.s2.field.PointerField;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.S2ModifiableFieldPath;
import skadistats.clarity.model.state.S2EntityState;
import skadistats.clarity.model.state.S2FlatEntityState;
import skadistats.clarity.model.state.StateMutation;
import skadistats.clarity.util.TextTable;

import java.util.Arrays;

public class S2FieldReader implements FieldReader<S2DTClass, S2FieldPath, S2EntityState> {

    protected final S2FieldPath[] fieldPaths = new S2FieldPath[MAX_PROPERTIES];

    private final Serializer[] pointerOverrides;

    public S2FieldReader(int pointerCount) {
        this.pointerOverrides = new Serializer[pointerCount];
    }

    private final TextTable dataDebugTable = new TextTable.Builder()
        .setFrame(TextTable.FRAME_COMPAT)
        .setPadding(0, 0)
        .addColumn("FP")
        .addColumn("Name")
        .addColumn("L", TextTable.Alignment.RIGHT)
        .addColumn("H", TextTable.Alignment.RIGHT)
        .addColumn("BC", TextTable.Alignment.RIGHT)
        .addColumn("Flags", TextTable.Alignment.RIGHT)
        .addColumn("Decoder")
        .addColumn("Type")
        .addColumn("Value")
        .addColumn("#", TextTable.Alignment.RIGHT)
        .addColumn("read")
        .build();

    private final TextTable opDebugTable = new TextTable.Builder()
        .setFrame(TextTable.FRAME_COMPAT)
        .setTitle("FieldPath Operations")
        .setPadding(0, 0)
        .addColumn("OP")
        .addColumn("FP")
        .addColumn("#", TextTable.Alignment.RIGHT)
        .addColumn("read")
        .build();

    @Override
    public FieldChanges<S2FieldPath> readFields(BitStream bs, S2DTClass dtClass, S2EntityState state, boolean debug, boolean materialize) {
        if (debug) return readFieldsDebug(bs, dtClass, state);
        if (materialize) return readFieldsMaterialized(bs, dtClass, state);
        return readFieldsFast(bs, dtClass, state);
    }

    private Field resolveField(S2EntityState state, S2DTClass dtClass, S2FieldPath fp) {
        Field f = state.getRootField();
        for (int i = 0; i <= fp.last(); i++) {
            f = f.getChild(state, fp.get(i));
            if (f == null) {
                throw new ClarityException("no field for class %s at %s!", dtClass.getDtName(), fp);
            }
        }
        return f;
    }

    private Field resolveFieldDebug(S2EntityState state, S2DTClass dtClass, S2FieldPath fp) {
        Field f = state.getRootField();
        for (int i = 0; i <= fp.last(); i++) {
            if (f instanceof PointerField pf) {
                f = pf.getChild(fp.get(i), state, pointerOverrides[pf.getPointerId()]);
            } else {
                f = f.getChild(state, fp.get(i));
            }
            if (f == null) {
                throw new ClarityException("no field for class %s at %s!", dtClass.getDtName(), fp);
            }
        }
        return f;
    }

    private FieldChanges<S2FieldPath> readFieldsFast(BitStream bs, S2DTClass dtClass, S2EntityState state) {
        var n = 0;
        var mfp = S2ModifiableFieldPath.newInstance();

        while (true) {
            var opId = bs.readFieldOpId();
            if (opId == FieldOp.FIELD_PATH_ENCODE_FINISH) {
                break;
            }
            FieldOp.execute(opId, mfp, bs);
            fieldPaths[n++] = mfp.unmodifiable();
        }

        var isFlat = state instanceof S2FlatEntityState;
        var fes = isFlat ? (S2FlatEntityState) state : null;
        var capacityChanged = false;
        for (var r = 0; r < n; r++) {
            var fp = fieldPaths[r];
            var field = resolveField(state, dtClass, fp);
            if (isFlat && field.isPrimitiveLeaf()) {
                capacityChanged |= fes.decodeInto(fp, field.getDecoder(), bs);
            } else {
                var decoded = DecoderDispatch.decode(bs, field.getDecoder());
                var writeValue = field.prepareForWrite(decoded, fp.last() + 1);
                capacityChanged |= state.write(fp, writeValue);
            }
        }

        return new FieldChanges<>(fieldPaths, n, capacityChanged);
    }

    private FieldChanges<S2FieldPath> readFieldsMaterialized(BitStream bs, S2DTClass dtClass, S2EntityState state) {
        var n = 0;
        var mfp = S2ModifiableFieldPath.newInstance();
        while (true) {
            var opId = bs.readFieldOpId();
            if (opId == FieldOp.FIELD_PATH_ENCODE_FINISH) {
                break;
            }
            FieldOp.execute(opId, mfp, bs);
            fieldPaths[n++] = mfp.unmodifiable();
        }

        var result = new FieldChanges<>(fieldPaths, n);
        Arrays.fill(pointerOverrides, null);
        for (var r = 0; r < n; r++) {
            var fp = fieldPaths[r];
            var field = resolveFieldDebug(state, dtClass, fp);
            var decoded = DecoderDispatch.decode(bs, field.getDecoder());
            var mutation = field.createMutation(decoded, fp.last() + 1);
            result.setMutation(r, mutation);
            if (mutation instanceof StateMutation.SwitchPointer sp) {
                pointerOverrides[((PointerField) field).getPointerId()] = sp.newSerializer();
            }
        }
        return result;
    }

    private FieldChanges<S2FieldPath> readFieldsDebug(BitStream bs, S2DTClass dtClass, S2EntityState state) {

        try {
            dataDebugTable.setTitle(dtClass.toString());
            dataDebugTable.clear();
            opDebugTable.clear();

            var n = 0;
            var mfp = S2ModifiableFieldPath.newInstance();
            while (true) {
                var offsBefore = bs.pos();
                var opId = bs.readFieldOpId();
                FieldOp.execute(opId, mfp, bs);
                opDebugTable.setData(n, 0, FieldOp.OPS[opId].name());
                opDebugTable.setData(n, 1, mfp.unmodifiable());
                opDebugTable.setData(n, 2, bs.pos() - offsBefore);
                opDebugTable.setData(n, 3, bs.toString(offsBefore, bs.pos()));
                if (opId == FieldOp.FIELD_PATH_ENCODE_FINISH) {
                    break;
                }
                fieldPaths[n++] = mfp.unmodifiable();
            }

            var result = new FieldChanges<>(fieldPaths, n);
            Arrays.fill(pointerOverrides, null);
            for (var r = 0; r < n; r++) {
                var fp = fieldPaths[r];
                var field = resolveFieldDebug(state, dtClass, fp);
                var decoder = field.getDecoder();
                var offsBefore = bs.pos();
                var decoded = DecoderDispatch.decode(bs, decoder);
                var mutation = field.createMutation(decoded, fp.last() + 1);
                result.setMutation(r, mutation);
                if (mutation instanceof StateMutation.SwitchPointer sp) {
                    pointerOverrides[((PointerField) field).getPointerId()] = sp.newSerializer();
                }

                var props = field.getSerializerProperties();
                var type = state.getTypeForFieldPath(fp);
                dataDebugTable.setData(r, 0, fp);
                dataDebugTable.setData(r, 1, state.getNameForFieldPath(fp));
                dataDebugTable.setData(r, 2, props.getLowValue());
                dataDebugTable.setData(r, 3, props.getHighValue());
                dataDebugTable.setData(r, 4, props.getBitCount());
                dataDebugTable.setData(r, 5, props.getEncodeFlags() != null ? Integer.toHexString(props.getEncodeFlags()) : "-");
                dataDebugTable.setData(r, 6, decoder.getClass().getSimpleName());
                dataDebugTable.setData(r, 7, String.format("%s%s", type, props.getEncoderType() != null ? String.format(" {%s}", props.getEncoderType()) : ""));
                dataDebugTable.setData(r, 8, decoded);
                dataDebugTable.setData(r, 9, bs.pos() - offsBefore);
                dataDebugTable.setData(r, 10, bs.toString(offsBefore, bs.pos()));
            }
            return result;
        } finally {
            dataDebugTable.print(Debug.STREAM);
            opDebugTable.print(Debug.STREAM);
        }
    }

    @Override
    public int readDeletions(BitStream bs, int indexBits, int[] deletions) {
        var n = bs.readUBitVar();
        var c = 0;
        var idx = -1;
        while (c < n) {
            idx += bs.readUBitVar();
            deletions[c++] = idx;
        }
        return n;
    }

}
