package skadistats.clarity.io.s1;

import skadistats.clarity.io.FieldChanges;
import skadistats.clarity.io.FieldReader;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.DecoderDispatch;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.S1FlatLayout;
import skadistats.clarity.model.state.StateMutation;
import skadistats.clarity.util.TextTable;

import java.util.Arrays;

public abstract class S1FieldReader extends FieldReader {

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
    public FieldChanges readFields(BitStream bs, DTClass dtClassGeneric, EntityState state, boolean debug, boolean materialize) {
        var dtClass = dtClassGeneric.s1();
        if (debug) return readFieldsDebug(bs, dtClass);
        if (materialize) return readFieldsMaterialize(bs, dtClass);

        var layout = dtClass.getFlatLayout();
        var receiveProps = dtClass.getReceiveProps();
        var n = readIndices(bs, dtClass);
        var kinds = layout.kinds();

        for (var ci = 0; ci < n; ci++) {
            var fp = fieldPaths[ci];
            var o = fp.s1().idx();
            var decoder = receiveProps[o].getSendProp().getDecoder();
            if (kinds[o] == S1FlatLayout.LeafKind.REF) {
                state.write(fp, DecoderDispatch.decode(bs, decoder));
            } else {
                state.decodeInto(fp, decoder, bs);
            }
        }
        return new FieldChanges(fieldPaths, n, false);
    }

    private FieldChanges readFieldsMaterialize(BitStream bs, S1DTClass dtClass) {
        var receiveProps = dtClass.getReceiveProps();
        var n = readIndices(bs, dtClass);
        var result = new FieldChanges(fieldPaths, n);
        for (var ci = 0; ci < n; ci++) {
            var o = fieldPaths[ci].s1().idx();
            var decoded = DecoderDispatch.decode(bs, receiveProps[o].getSendProp().getDecoder());
            result.setMutation(ci, new StateMutation.WriteValue(decoded));
        }
        return result;
    }

    private FieldChanges readFieldsDebug(BitStream bs, S1DTClass dtClass) {
        try {
            debugTable.setTitle(dtClass.getDtName());
            debugTable.clear();

            var n = readIndices(bs, dtClass);
            var result = new FieldChanges(fieldPaths, n);
            var receiveProps = dtClass.getReceiveProps();
            for (var ci = 0; ci < n; ci++) {
                var offsBefore = bs.pos();
                var o = fieldPaths[ci].s1().idx();
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
            debugTable.print(DEBUG_STREAM);
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
