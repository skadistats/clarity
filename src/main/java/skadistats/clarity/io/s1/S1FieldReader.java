package skadistats.clarity.io.s1;

import skadistats.clarity.io.FieldChanges;
import skadistats.clarity.io.FieldReader;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.DecoderDispatch;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.S1DTClass;
import skadistats.clarity.model.s1.S1FieldPath;
import skadistats.clarity.state.FieldLayout;
import skadistats.clarity.state.StateMutation;
import skadistats.clarity.state.s1.S1EntityState;
import skadistats.clarity.util.TextTable;

import java.util.Arrays;

public abstract class S1FieldReader implements FieldReader<S1DTClass, S1FieldPath, S1EntityState> {

    protected final S1FieldPath[] fieldPaths = new S1FieldPath[MAX_PROPERTIES];

    private final TextTable debugTable = new TextTable.Builder()
        .setFrame(TextTable.FRAME_COMPAT)
        .setPadding(0, 0)
        .addColumn("Idx")
        .addColumn("Name")
        .addColumn("L", TextTable.Alignment.RIGHT)
        .addColumn("H", TextTable.Alignment.RIGHT)
        .addColumn("BC", TextTable.Alignment.RIGHT)
        .addColumn("Flags", TextTable.Alignment.RIGHT)
        .addColumn("Decoder")
        .addColumn("Value")
        .addColumn("#", TextTable.Alignment.RIGHT)
        .addColumn("read")
        .build();

    protected abstract int readIndices(BitStream bs, S1DTClass dtClass);

    @Override
    public FieldChanges<S1FieldPath> readFields(BitStream bs, S1DTClass dtClass, S1EntityState state, boolean debug, boolean materialize) {
        if (debug) return readFieldsDebug(bs, dtClass);
        if (materialize) return readFieldsMaterialize(bs, dtClass);

        var layout = dtClass.getFlatLayout();
        var receiveProps = dtClass.getReceiveProps();
        var n = readIndices(bs, dtClass);
        var leaves = layout.leaves();

        for (var ci = 0; ci < n; ci++) {
            var fp = fieldPaths[ci];
            var o = fp.idx();
            var decoder = receiveProps[o].getSendProp().getDecoder();
            if (leaves[o] instanceof FieldLayout.Ref) {
                state.write(fp, DecoderDispatch.decode(bs, decoder));
            } else {
                state.decodeInto(fp, decoder, bs);
            }
        }
        return new FieldChanges<>(fieldPaths, n, false);
    }

    private FieldChanges<S1FieldPath> readFieldsMaterialize(BitStream bs, S1DTClass dtClass) {
        var receiveProps = dtClass.getReceiveProps();
        var n = readIndices(bs, dtClass);
        var result = new FieldChanges<>(fieldPaths, n);
        for (var ci = 0; ci < n; ci++) {
            var o = fieldPaths[ci].idx();
            var decoded = DecoderDispatch.decode(bs, receiveProps[o].getSendProp().getDecoder());
            result.setMutation(ci, new StateMutation.WriteValue(decoded));
        }
        return result;
    }

    private FieldChanges<S1FieldPath> readFieldsDebug(BitStream bs, S1DTClass dtClass) {
        try {
            debugTable.setTitle(dtClass.getDtName());
            debugTable.clear();

            var n = readIndices(bs, dtClass);
            var result = new FieldChanges<>(fieldPaths, n);
            var receiveProps = dtClass.getReceiveProps();
            for (var ci = 0; ci < n; ci++) {
                var offsBefore = bs.pos();
                var o = fieldPaths[ci].idx();
                var sp = receiveProps[o].getSendProp();
                var decoded = DecoderDispatch.decode(bs, sp.getDecoder());
                result.setMutation(ci, new StateMutation.WriteValue(decoded));

                debugTable.setData(ci, 0, o);
                debugTable.setData(ci, 1, receiveProps[o].getVarName());
                debugTable.setData(ci, 2, sp.getLowValue());
                debugTable.setData(ci, 3, sp.getHighValue());
                debugTable.setData(ci, 4, sp.getNumBits());
                debugTable.setData(ci, 5, PropFlag.descriptionForFlags(sp.getFlags()));
                debugTable.setData(ci, 6, sp.getDecoder().getClass().getSimpleName());
                debugTable.setData(ci, 7, decoded.getClass().isArray() ? Arrays.toString((Object[]) decoded) : decoded);
                debugTable.setData(ci, 8, bs.pos() - offsBefore);
                debugTable.setData(ci, 9, bs.toString(offsBefore, bs.pos()));
            }
            return result;
        } finally {
            debugTable.print(Debug.STREAM);
        }
    }

    @Override
    public int readDeletions(BitStream bs, int indexBits, int[] deletions) {
        var n = 0;
        while (bs.readBitFlag()) {
            deletions[n++]= bs.readUBitInt(indexBits);
        }
        return n;
    }

}
