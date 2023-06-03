package skadistats.clarity.io.s2;

import skadistats.clarity.ClarityException;
import skadistats.clarity.io.FieldChanges;
import skadistats.clarity.io.FieldReader;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.S2ModifiableFieldPath;
import skadistats.clarity.util.TextTable;

public class S2FieldReader extends FieldReader<S2DTClass> {

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
    public FieldChanges readFields(BitStream bs, S2DTClass dtClass, boolean debug) {
        try {
            if (debug) {
                dataDebugTable.setTitle(dtClass.toString());
                dataDebugTable.clear();
                opDebugTable.clear();
            }

            var n = 0;
            var mfp = S2ModifiableFieldPath.newInstance();
            while (true) {
                var offsBefore = bs.pos();
                var op = bs.readFieldOp();
                op.execute(mfp, bs);
                if (debug) {
                    opDebugTable.setData(n, 0, op);
                    opDebugTable.setData(n, 1, mfp.unmodifiable());
                    opDebugTable.setData(n, 2, bs.pos() - offsBefore);
                    opDebugTable.setData(n, 3, bs.toString(offsBefore, bs.pos()));
                }
                if (op == FieldOpType.FieldPathEncodeFinish) {
                    break;
                }
                fieldPaths[n++] = mfp.unmodifiable();
            }

            var result = new FieldChanges(fieldPaths, n);

            for (var r = 0; r < n; r++) {
                var fp = fieldPaths[r].s2();
                var decoder = dtClass.getDecoderForFieldPath(fp);
                if (decoder == null) {
                    throw new ClarityException("no decoder for class %s at %s!", dtClass.getDtName(), fp);
                }
                var offsBefore = bs.pos();
                result.setValue(r, decoder.decode(bs));

                if (debug) {
                    var props = dtClass.getFieldForFieldPath(fp).getDecoderProperties();
                    var type = dtClass.getTypeForFieldPath(fp);
                    dataDebugTable.setData(r, 0, fp);
                    dataDebugTable.setData(r, 1, dtClass.getNameForFieldPath(fp));
                    dataDebugTable.setData(r, 2, props.getLowValue());
                    dataDebugTable.setData(r, 3, props.getHighValue());
                    dataDebugTable.setData(r, 4, props.getBitCount());
                    dataDebugTable.setData(r, 5, props.getEncodeFlags() != null ? Integer.toHexString(props.getEncodeFlags()) : "-");
                    dataDebugTable.setData(r, 6, decoder.getClass().getSimpleName());
                    dataDebugTable.setData(r, 7, String.format("%s%s", type, props.getEncoderType() != null ? String.format(" {%s}", props.getEncoderType()) : ""));
                    dataDebugTable.setData(r, 8, result.getValue(r));
                    dataDebugTable.setData(r, 9, bs.pos() - offsBefore);
                    dataDebugTable.setData(r, 10, bs.toString(offsBefore, bs.pos()));
                }
            }
            return result;
        } finally {
            if (debug) {
                dataDebugTable.print(DEBUG_STREAM);
                opDebugTable.print(DEBUG_STREAM);
            }
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
